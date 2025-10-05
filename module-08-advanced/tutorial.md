# 高级并发工具教程

## 一、ForkJoin框架

### 1.1 什么是ForkJoin？

**ForkJoin框架**是Java 7引入的并行计算框架，专门用于**分治算法（Divide and Conquer）**的并行化。

**核心思想**：
1. **Fork（分叉）**：将大任务拆分成多个小任务
2. **Compute（计算）**：并行计算小任务
3. **Join（合并）**：合并小任务的结果

### 1.2 工作窃取算法（Work-Stealing）

**传统线程池问题**：
- 所有线程从同一个全局队列获取任务
- 高并发时队列成为瓶颈

**ForkJoinPool解决方案**：
- 每个线程有自己的**双端队列（Deque）**
- 线程从队列头部获取自己的任务（LIFO）
- 空闲线程从其他队列尾部**窃取**任务（FIFO）

```
线程1队列: [Task1, Task2, Task3] <- 线程1从头部取
                            ^
                            |
                         窃取（从尾部）
                            |
线程2队列: [Task4, Task5] <- 线程2空闲时去窃取
```

**优势**：
- 减少竞争（大部分时间访问自己的队列）
- 负载均衡（空闲线程窃取忙碌线程的任务）
- 提高CPU利用率

### 1.3 RecursiveTask vs RecursiveAction

**RecursiveTask<V>**：有返回值的任务
```java
class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 1000;
    private final int[] array;
    private final int start, end;

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // 任务足够小，直接计算
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // 任务太大，拆分
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);

            left.fork();  // 异步执行左任务
            long rightResult = right.compute(); // 当前线程执行右任务
            long leftResult = left.join(); // 等待左任务完成

            return leftResult + rightResult;
        }
    }
}
```

**RecursiveAction**：无返回值的任务
```java
class SortAction extends RecursiveAction {
    private final int[] array;
    private final int start, end;

    @Override
    protected void compute() {
        if (end - start <= THRESHOLD) {
            Arrays.sort(array, start, end); // 直接排序
        } else {
            int mid = (start + end) / 2;
            invokeAll(
                new SortAction(array, start, mid),
                new SortAction(array, mid, end)
            );
        }
    }
}
```

### 1.4 ForkJoinPool使用

```java
// 创建ForkJoinPool
ForkJoinPool pool = new ForkJoinPool();

// 方式1: submit + get
ForkJoinTask<Long> task = new SumTask(array, 0, array.length);
Long result = pool.submit(task).get();

// 方式2: invoke（推荐）
Long result = pool.invoke(task);

// 使用通用池（推荐）
ForkJoinPool commonPool = ForkJoinPool.commonPool();
Long result = commonPool.invoke(task);
```

### 1.5 最佳实践

**1. 合理设置阈值**
```java
// ❌ 阈值太小 - 过度拆分，调度开销大
private static final int THRESHOLD = 1;

// ✅ 合理阈值 - 平衡并行度和开销
private static final int THRESHOLD = 1000;
```

**2. 避免阻塞操作**
```java
// ❌ 在compute中阻塞
protected Long compute() {
    Thread.sleep(1000); // 阻塞工作线程，影响工作窃取
    return result;
}

// ✅ 只做计算，不阻塞
protected Long compute() {
    return heavyComputation(); // 纯计算任务
}
```

**3. 正确的fork/join模式**
```java
// ✅ 推荐：一个fork，一个compute
left.fork();
long rightResult = right.compute();
long leftResult = left.join();

// ✅ 也可以：都fork再join
left.fork();
right.fork();
return left.join() + right.join();

// ❌ 避免：都compute（串行执行）
long leftResult = left.compute();
long rightResult = right.compute();
```

---

## 二、StampedLock

### 2.1 为什么需要StampedLock？

**ReentrantReadWriteLock的问题**：
- 读锁也需要CAS操作，有一定开销
- 写线程可能饥饿（大量读时写线程难以获取锁）

**StampedLock的改进**（Java 8引入）：
- 提供**乐观读（Optimistic Read）**机制
- 乐观读不加锁，只是获取一个版本号（stamp）
- 读完后验证版本号是否变化
- 如果未变化，读操作有效；否则升级为悲观读

### 2.2 三种锁模式

**1. 写锁（Write Lock）**：排他锁
```java
long stamp = lock.writeLock();
try {
    // 修改数据
    x = newX;
    y = newY;
} finally {
    lock.unlockWrite(stamp);
}
```

**2. 悲观读锁（Pessimistic Read Lock）**：共享锁
```java
long stamp = lock.readLock();
try {
    // 读取数据
    double result = Math.sqrt(x * x + y * y);
} finally {
    lock.unlockRead(stamp);
}
```

**3. 乐观读（Optimistic Read）**：无锁读（性能最高）
```java
long stamp = lock.tryOptimisticRead(); // 获取乐观读stamp
double currentX = x; // 读取数据
double currentY = y;

if (!lock.validate(stamp)) { // 验证期间是否有写入
    // 验证失败，升级为悲观读
    stamp = lock.readLock();
    try {
        currentX = x;
        currentY = y;
    } finally {
        lock.unlockRead(stamp);
    }
}
// 使用读取的数据
double result = Math.sqrt(currentX * currentX + currentY * currentY);
```

### 2.3 完整示例

```java
public class Point {
    private final StampedLock lock = new StampedLock();
    private double x, y;

    // 写操作
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // 乐观读
    public double distanceFromOrigin() {
        long stamp = lock.tryOptimisticRead();
        double currentX = x;
        double currentY = y;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // 锁升级示例
    public void moveIfAtOrigin(double newX, double newY) {
        long stamp = lock.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // 尝试升级为写锁
                long ws = lock.tryConvertToWriteLock(stamp);
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    // 升级失败，释放读锁，获取写锁
                    lock.unlockRead(stamp);
                    stamp = lock.writeLock();
                }
            }
        } finally {
            lock.unlock(stamp);
        }
    }
}
```

### 2.4 StampedLock注意事项

**1. 不可重入**
```java
// ❌ 死锁！StampedLock不支持重入
long stamp = lock.writeLock();
methodThatAlsoNeedsLock(); // 再次获取锁 -> 死锁
lock.unlockWrite(stamp);
```

**2. 不支持Condition**
```java
// ❌ StampedLock没有Condition
// 如需条件等待，使用ReentrantLock
```

**3. 乐观读的线程安全**
```java
// ❌ 错误：直接使用可能不一致的数据
long stamp = lock.tryOptimisticRead();
double result = x / y; // x和y可能不一致！

// ✅ 正确：先读取，再验证
long stamp = lock.tryOptimisticRead();
double localX = x;
double localY = y;
if (!lock.validate(stamp)) {
    // 重新读取...
}
double result = localX / localY; // 使用一致的本地副本
```

---

## 三、Phaser

### 3.1 Phaser vs CyclicBarrier

**CyclicBarrier的局限**：
- 参与线程数固定
- 只能循环使用一个屏障点

**Phaser的优势**（Java 7引入）：
- ✅ 支持**动态增加/减少**参与者
- ✅ 支持**多阶段**同步
- ✅ 更灵活的控制

### 3.2 核心概念

**Phase（阶段）**：
- 每个同步点是一个阶段
- 阶段编号从0开始，自动递增
- 可通过`getPhase()`获取当前阶段

**Parties（参与者）**：
- 注册的线程数
- 可动态调整（register/arriveAndDeregister）

**Termination（终止）**：
- 所有参与者注销后，Phaser终止
- 或者`onAdvance()`返回true

### 3.3 基本用法

```java
// 创建Phaser，初始参与者数量
Phaser phaser = new Phaser(3); // 3个参与者

// 线程执行任务
class Worker implements Runnable {
    @Override
    public void run() {
        // 阶段1：准备
        System.out.println("阶段1：准备数据");
        phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段1

        // 阶段2：处理
        System.out.println("阶段2：处理数据");
        phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段2

        // 阶段3：输出
        System.out.println("阶段3：输出结果");
        phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段3

        phaser.arriveAndDeregister(); // 完成后注销
    }
}
```

### 3.4 核心API

**注册/注销**：
```java
phaser.register();              // 增加一个参与者
phaser.bulkRegister(n);         // 增加n个参与者
phaser.arriveAndDeregister();   // 到达并注销
```

**到达/等待**：
```java
phaser.arrive();                    // 到达但不等待
phaser.arriveAndAwaitAdvance();     // 到达并等待其他线程
int phase = phaser.awaitAdvance(n); // 等待指定阶段完成
```

**查询状态**：
```java
int phase = phaser.getPhase();              // 当前阶段
int parties = phaser.getRegisteredParties(); // 参与者数量
boolean terminated = phaser.isTerminated();  // 是否终止
```

### 3.5 高级用法：自定义阶段完成行为

```java
Phaser phaser = new Phaser(3) {
    @Override
    protected boolean onAdvance(int phase, int registeredParties) {
        System.out.println("阶段 " + phase + " 完成！参与者: " + registeredParties);

        // 返回true表示终止Phaser
        return phase >= 2 || registeredParties == 0;
    }
};
```

### 3.6 实际示例：多阶段并行处理

```java
public class MultiPhaseDataProcessor {
    private final Phaser phaser;
    private final int numWorkers = 4;

    public void process() {
        phaser = new Phaser(numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            new Thread(new DataWorker(i)).start();
        }
    }

    class DataWorker implements Runnable {
        private final int id;

        DataWorker(int id) { this.id = id; }

        @Override
        public void run() {
            // 阶段0：加载数据
            System.out.println("Worker " + id + ": 加载数据");
            phaser.arriveAndAwaitAdvance();

            // 阶段1：数据清洗
            System.out.println("Worker " + id + ": 清洗数据");
            phaser.arriveAndAwaitAdvance();

            // 阶段2：数据分析
            System.out.println("Worker " + id + ": 分析数据");
            phaser.arriveAndAwaitAdvance();

            // 阶段3：生成报告
            System.out.println("Worker " + id + ": 生成报告");
            phaser.arriveAndAwaitAdvance();

            System.out.println("Worker " + id + ": 完成所有阶段");
        }
    }
}
```

### 3.7 动态参与者示例

```java
Phaser phaser = new Phaser(1); // 主线程

// 主线程控制
new Thread(() -> {
    for (int i = 0; i < 3; i++) {
        System.out.println("启动阶段 " + i);

        // 动态添加工作线程
        int workers = (i + 1) * 2; // 阶段0:2个, 阶段1:4个, 阶段2:6个
        for (int j = 0; j < workers; j++) {
            phaser.register();
            new Thread(() -> {
                System.out.println("Worker执行阶段 " + phaser.getPhase());
                phaser.arriveAndDeregister();
            }).start();
        }

        phaser.arriveAndAwaitAdvance(); // 等待当前阶段完成
    }
    phaser.arriveAndDeregister(); // 主线程退出
}).start();
```

---

## 四、Exchanger

### 4.1 什么是Exchanger？

**Exchanger<V>**：用于两个线程之间交换数据的同步点。

**特点**：
- 只支持**两个线程**之间交换
- 双方都到达交换点时，才进行交换
- 可用于对称的生产者-消费者场景

### 4.2 基本用法

```java
Exchanger<String> exchanger = new Exchanger<>();

// 线程1
new Thread(() -> {
    try {
        String data = "来自线程1的数据";
        System.out.println("线程1准备交换: " + data);

        String received = exchanger.exchange(data); // 阻塞等待交换

        System.out.println("线程1收到: " + received);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}).start();

// 线程2
new Thread(() -> {
    try {
        String data = "来自线程2的数据";
        System.out.println("线程2准备交换: " + data);

        String received = exchanger.exchange(data); // 阻塞等待交换

        System.out.println("线程2收到: " + received);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}).start();
```

**输出**：
```
线程1准备交换: 来自线程1的数据
线程2准备交换: 来自线程2的数据
线程1收到: 来自线程2的数据
线程2收到: 来自线程1的数据
```

### 4.3 实际应用：生产者-消费者缓冲区交换

```java
public class ExchangerBufferExample {
    private static final Exchanger<List<String>> exchanger = new Exchanger<>();

    // 生产者
    static class Producer implements Runnable {
        private List<String> buffer = new ArrayList<>();

        @Override
        public void run() {
            for (int i = 1; i <= 10; i++) {
                buffer.add("Item-" + i);

                if (buffer.size() == 3) { // 缓冲区满
                    try {
                        System.out.println("生产者交换缓冲区: " + buffer);
                        buffer = exchanger.exchange(buffer); // 交换空缓冲区
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // 消费者
    static class Consumer implements Runnable {
        private List<String> buffer = new ArrayList<>();

        @Override
        public void run() {
            for (int i = 0; i < 4; i++) { // 交换4次
                try {
                    buffer = exchanger.exchange(buffer); // 交换满缓冲区
                    System.out.println("消费者收到: " + buffer);
                    buffer.clear(); // 清空供下次交换
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### 4.4 超时交换

```java
try {
    String received = exchanger.exchange("数据", 1, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("交换超时，对方未到达");
}
```

---

## 五、工具选择指南

### 5.1 并行计算

| 场景 | 推荐工具 | 原因 |
|------|---------|------|
| 递归分治（如归并排序、快排） | **ForkJoinPool** | 工作窃取，高效负载均衡 |
| 独立任务（如批量HTTP请求） | **ThreadPoolExecutor** | 简单，适合无依赖任务 |
| 流式计算（如数组操作） | **Stream.parallel()** | 内部使用ForkJoinPool，简洁 |

### 5.2 读写场景

| 读写比例 | 推荐工具 | 原因 |
|---------|---------|------|
| 读多写少（读>>写） | **StampedLock乐观读** | 读操作无锁，性能最高 |
| 读多写少（读>写） | **ReentrantReadWriteLock** | 稳定，支持Condition |
| 读写均衡 | **ReentrantLock / synchronized** | 简单可靠 |

### 5.3 同步协调

| 场景 | 推荐工具 | 原因 |
|------|---------|------|
| 固定阶段，固定线程数 | **CyclicBarrier** | 简单够用 |
| 多阶段，动态线程数 | **Phaser** | 灵活，支持动态调整 |
| 一次性屏障 | **CountDownLatch** | 轻量级 |
| 两线程数据交换 | **Exchanger** | 专门设计 |

---

## 六、性能对比实验

### 6.1 ForkJoinPool vs ThreadPoolExecutor

**测试场景**：计算10000000个数的和

```java
// ForkJoinPool
ForkJoinPool pool = new ForkJoinPool();
long result = pool.invoke(new SumTask(array, 0, array.length));
// 耗时：约100ms

// ThreadPoolExecutor（手动分片）
ExecutorService pool = Executors.newFixedThreadPool(8);
List<Future<Long>> futures = new ArrayList<>();
int chunkSize = array.length / 8;
// ... 提交任务
// 耗时：约150ms（手动分片，不够灵活）
```

**结论**：递归分治任务，ForkJoinPool性能更优。

### 6.2 StampedLock vs ReentrantReadWriteLock

**测试场景**：90%读操作，10%写操作

```java
// StampedLock乐观读
// 吞吐量：约500万ops/s

// ReentrantReadWriteLock
// 吞吐量：约300万ops/s

// 结论：读密集场景，StampedLock提升60%+
```

---

## 七、常见陷阱

### 7.1 ForkJoinPool陷阱

**陷阱1：阈值设置不当**
```java
// ❌ 阈值太小
private static final int THRESHOLD = 10; // 过度拆分，开销大

// ✅ 合理阈值
private static final int THRESHOLD = 1000; // 根据实际测试调整
```

**陷阱2：阻塞操作**
```java
// ❌ 在compute中执行I/O
protected Long compute() {
    readFromDatabase(); // 阻塞工作线程！
    return result;
}
```

### 7.2 StampedLock陷阱

**陷阱1：忘记验证**
```java
// ❌ 乐观读不验证
long stamp = lock.tryOptimisticRead();
double result = x / y; // x、y可能不一致！

// ✅ 必须验证
long stamp = lock.tryOptimisticRead();
double localX = x, localY = y;
if (!lock.validate(stamp)) {
    // 重新读取...
}
double result = localX / localY;
```

**陷阱2：重入**
```java
// ❌ 死锁！StampedLock不可重入
long stamp = lock.writeLock();
anotherMethodNeedsLock(); // 再次获取锁 -> 死锁
```

### 7.3 Phaser陷阱

**陷阱1：忘记注销**
```java
// ❌ 线程结束未注销
phaser.arriveAndAwaitAdvance();
// return; // 忘记调用arriveAndDeregister()，Phaser无法终止

// ✅ 完成后注销
phaser.arriveAndAwaitAdvance();
phaser.arriveAndDeregister();
```

---

## 八、总结

### 核心要点

1. **ForkJoinPool**：分治算法的首选，理解工作窃取
2. **StampedLock**：读密集场景的性能利器，注意验证
3. **Phaser**：灵活的多阶段同步，支持动态参与者
4. **Exchanger**：两线程数据交换的专用工具

### 学习路径

```
基础 → 同步 → Lock → 原子类 → 集合 → 线程池 → CompletableFuture → 高级工具
```

恭喜你完成了Java并发编程的进阶学习！继续深入JDK源码，探索更多并发奥秘。
