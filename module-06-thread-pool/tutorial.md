# 线程池教程

## 一、为什么需要线程池？

### 1.1 线程的问题

**直接创建线程的缺点：**
1. **创建/销毁开销大**：每次new Thread()都有系统开销
2. **资源无限制**：无限创建线程可能耗尽系统资源
3. **管理困难**：缺乏统一的任务管理和监控
4. **响应时间慢**：任务到达时才创建线程，延迟高

```java
// 不推荐：每个任务创建一个线程
for (int i = 0; i < 10000; i++) {
    new Thread(() -> {
        // 处理任务
    }).start();
}
// 问题：可能创建上万个线程，系统崩溃！
```

### 1.2 线程池的优势

**线程池（Thread Pool）的好处：**
1. **降低资源消耗**：重用已创建的线程，减少创建/销毁开销
2. **提高响应速度**：任务到达时，线程已就绪，无需等待创建
3. **提高可管理性**：统一管理线程，提供监控、调优能力
4. **控制并发度**：限制最大线程数，避免资源耗尽

```java
// 推荐：使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10000; i++) {
    executor.execute(() -> {
        // 处理任务
    });
}
executor.shutdown();
// 优点：只创建10个线程，重复使用！
```

---

## 二、ThreadPoolExecutor核心原理

### 2.1 类继承结构

```
Executor (接口)
    └── ExecutorService (接口)
            └── AbstractExecutorService (抽象类)
                    ├── ThreadPoolExecutor (核心实现)
                    └── ScheduledThreadPoolExecutor (定时任务)
```

### 2.2 核心构造参数

```java
public ThreadPoolExecutor(
    int corePoolSize,                   // 核心线程数
    int maximumPoolSize,                // 最大线程数
    long keepAliveTime,                 // 空闲线程存活时间
    TimeUnit unit,                      // 时间单位
    BlockingQueue<Runnable> workQueue,  // 工作队列
    ThreadFactory threadFactory,        // 线程工厂
    RejectedExecutionHandler handler    // 拒绝策略
)
```

**参数详解：**

#### 1. corePoolSize（核心线程数）
- 线程池保持存活的最小线程数
- 即使线程空闲也不会销毁（除非设置`allowCoreThreadTimeOut`）
- 默认情况下，核心线程会一直存活

#### 2. maximumPoolSize（最大线程数）
- 线程池允许创建的最大线程数
- 当队列满时，会创建新线程直到达到此上限
- 必须 >= corePoolSize

#### 3. keepAliveTime（空闲线程存活时间）
- 当线程数 > corePoolSize时，空闲线程的最大存活时间
- 超过此时间的空闲线程会被销毁，直到线程数 = corePoolSize

#### 4. workQueue（工作队列）
- 存储等待执行的任务
- 常用队列类型：
  - **ArrayBlockingQueue**：有界队列，必须指定容量
  - **LinkedBlockingQueue**：可选有界，默认Integer.MAX_VALUE（无界）
  - **SynchronousQueue**：不存储任务，直接交给线程
  - **PriorityBlockingQueue**：优先级队列

#### 5. threadFactory（线程工厂）
- 创建新线程的工厂
- 可自定义线程名称、优先级、守护状态等
- 默认使用`Executors.defaultThreadFactory()`

#### 6. handler（拒绝策略）
- 当队列满且线程数达到maximumPoolSize时的处理策略
- 4种内置策略（见2.4节）

### 2.3 任务执行流程

```
                 提交任务
                    ↓
          当前线程数 < corePoolSize?
                    ↓ 是
            创建核心线程执行任务
                    ↓ 否
            工作队列是否已满?
                    ↓ 否
            任务加入工作队列
                    ↓ 是
      当前线程数 < maximumPoolSize?
                    ↓ 是
            创建非核心线程执行任务
                    ↓ 否
            执行拒绝策略
```

**示例说明：**
```java
// 配置：核心2，最大5，队列容量3
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,  // 核心线程数
    5,  // 最大线程数
    60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(3)
);

// 提交10个任务的执行过程：
// 任务1-2：创建2个核心线程执行
// 任务3-5：核心线程忙，放入队列（队列容量3）
// 任务6-8：队列满，创建3个非核心线程执行（总线程数达到5）
// 任务9-10：线程数已达上限，执行拒绝策略
```

### 2.4 拒绝策略

当队列满且线程数达到maximumPoolSize时，执行拒绝策略：

#### 1. AbortPolicy（默认）
- **行为**：抛出`RejectedExecutionException`异常
- **适用**：希望明确感知任务被拒绝的场景
```java
new ThreadPoolExecutor.AbortPolicy()
```

#### 2. CallerRunsPolicy
- **行为**：由调用线程（提交任务的线程）执行任务
- **优点**：不丢弃任务，提供降级策略
- **缺点**：可能阻塞调用线程
```java
new ThreadPoolExecutor.CallerRunsPolicy()
```

#### 3. DiscardPolicy
- **行为**：静默丢弃任务，不抛异常
- **适用**：任务允许丢失的场景
```java
new ThreadPoolExecutor.DiscardPolicy()
```

#### 4. DiscardOldestPolicy
- **行为**：丢弃队列中最老的任务，然后重试提交
- **适用**：后来的任务优先级更高的场景
```java
new ThreadPoolExecutor.DiscardOldestPolicy()
```

#### 5. 自定义拒绝策略
```java
RejectedExecutionHandler customHandler = (r, executor) -> {
    // 记录日志
    System.err.println("任务被拒绝: " + r.toString());
    // 可以选择：存入数据库、发送MQ、重试等
};
```

---

## 三、Executors工厂方法

### 3.1 newFixedThreadPool
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
```

**等价配置：**
```java
new ThreadPoolExecutor(
    5, 5,  // 核心和最大线程数相同
    0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>()  // 无界队列
)
```

**特点：**
- 线程数固定
- 适用于负载较重的服务器

**风险：**
- 使用无界队列，任务堆积可能导致OOM

### 3.2 newCachedThreadPool
```java
ExecutorService executor = Executors.newCachedThreadPool();
```

**等价配置：**
```java
new ThreadPoolExecutor(
    0, Integer.MAX_VALUE,  // 核心0，最大无限
    60L, TimeUnit.SECONDS,
    new SynchronousQueue<>()  // 不存储任务
)
```

**特点：**
- 线程数不固定，按需创建
- 空闲线程60秒后回收
- 适用于大量短期异步任务

**风险：**
- 可能创建过多线程，耗尽系统资源

### 3.3 newSingleThreadExecutor
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
```

**等价配置：**
```java
new ThreadPoolExecutor(
    1, 1,  // 单线程
    0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>()
)
```

**特点：**
- 单线程顺序执行任务
- 适用于需要顺序执行的场景

**风险：**
- 使用无界队列，任务堆积可能导致OOM

### 3.4 newScheduledThreadPool
```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
```

**特点：**
- 支持定时和周期性任务
- 详见第四章

### 3.5 为什么不推荐使用Executors？

**阿里巴巴Java开发手册强制规定：**
> 线程池不允许使用Executors去创建，而是通过ThreadPoolExecutor的方式，这样的处理方式让写的同学更加明确线程池的运行规则，规避资源耗尽的风险。

**原因：**
1. `newFixedThreadPool`和`newSingleThreadExecutor`：使用无界队列，可能堆积大量任务导致OOM
2. `newCachedThreadPool`：允许创建Integer.MAX_VALUE个线程，可能耗尽资源
3. 参数不透明，不利于调优和问题排查

**推荐做法：**
```java
// 手动创建，明确参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                          // 核心线程数
    20,                          // 最大线程数
    60L, TimeUnit.SECONDS,       // 空闲线程存活时间
    new ArrayBlockingQueue<>(100),  // 有界队列
    new ThreadFactoryBuilder()
        .setNameFormat("my-pool-%d")
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

---

## 四、ScheduledThreadPoolExecutor

### 4.1 定时任务API

#### 1. schedule（延迟执行）
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

// 延迟3秒执行
scheduler.schedule(() -> {
    System.out.println("3秒后执行");
}, 3, TimeUnit.SECONDS);
```

#### 2. scheduleAtFixedRate（固定频率）
```java
// 初始延迟1秒，之后每隔2秒执行一次
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("每2秒执行一次");
}, 1, 2, TimeUnit.SECONDS);
```

**特点：**
- 按固定频率执行，不考虑任务执行时间
- 如果任务执行时间 > 周期，下次任务会立即执行

```
任务开始时间: 0s    2s    4s    6s
任务执行时长: [1s]  [1s]  [3s]     [1s]
实际周期:     0s -> 2s -> 4s -> 7s -> 9s
                              ↑ 任务执行了3秒，超过周期2秒
```

#### 3. scheduleWithFixedDelay（固定延迟）
```java
// 初始延迟1秒，任务结束后延迟2秒再执行下一次
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("任务结束后等待2秒");
}, 1, 2, TimeUnit.SECONDS);
```

**特点：**
- 任务结束后，固定延迟再执行下一次
- 不受任务执行时间影响

```
任务开始时间: 0s    3s    6s    11s
任务执行时长: [1s]  [1s]  [3s]      [1s]
实际周期:     0s -> 3s -> 6s -> 11s -> 14s
                              ↑ 任务执行3秒 + 延迟2秒
```

### 4.2 对比总结

| 方法 | 说明 | 周期计算 | 适用场景 |
|------|------|----------|----------|
| `schedule` | 延迟执行一次 | - | 延迟任务 |
| `scheduleAtFixedRate` | 固定频率 | 上次开始时间 + 周期 | 数据同步、心跳 |
| `scheduleWithFixedDelay` | 固定延迟 | 上次结束时间 + 延迟 | 轮询、定时清理 |

---

## 五、线程池的监控与调优

### 5.1 监控指标

```java
ThreadPoolExecutor executor = ...;

// 获取监控数据
int coreSize = executor.getCorePoolSize();           // 核心线程数
int poolSize = executor.getPoolSize();               // 当前线程数
int activeCount = executor.getActiveCount();         // 活动线程数
long taskCount = executor.getTaskCount();            // 总任务数
long completedCount = executor.getCompletedTaskCount(); // 已完成任务数
int queueSize = executor.getQueue().size();          // 队列中的任务数

// 计算指标
double utilization = (double) activeCount / poolSize;  // 线程利用率
double queueUtilization = (double) queueSize / queueCapacity; // 队列利用率
```

### 5.2 自定义ThreadFactory

```java
ThreadFactory factory = new ThreadFactory() {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("MyPool-" + threadNumber.getAndIncrement());
        t.setDaemon(false);  // 非守护线程
        t.setPriority(Thread.NORM_PRIORITY);

        // 设置未捕获异常处理器
        t.setUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("线程异常: " + thread.getName());
            throwable.printStackTrace();
        });

        return t;
    }
};
```

### 5.3 扩展ThreadPoolExecutor

```java
public class MonitoredThreadPool extends ThreadPoolExecutor {

    public MonitoredThreadPool(int corePoolSize, int maximumPoolSize,
                               long keepAliveTime, TimeUnit unit,
                               BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        System.out.println(t.getName() + " 开始执行任务");
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        System.out.println("任务执行完成");
        if (t != null) {
            System.err.println("任务执行异常: " + t.getMessage());
        }
    }

    @Override
    protected void terminated() {
        super.terminated();
        System.out.println("线程池已终止");
    }
}
```

### 5.4 线程池大小如何设置？

#### CPU密集型任务
```java
// 计算密集型：线程数 = CPU核心数 + 1
int cpuCount = Runtime.getRuntime().availableProcessors();
int threadCount = cpuCount + 1;
```

#### IO密集型任务
```java
// IO密集型：线程数 = CPU核心数 * (1 + IO时间/CPU时间)
// 假设IO时间是CPU时间的2倍
int cpuCount = Runtime.getRuntime().availableProcessors();
int threadCount = cpuCount * (1 + 2);  // cpuCount * 3
```

#### 混合型任务
- 通过压测确定最佳线程数
- 监控CPU使用率、响应时间、吞吐量
- 动态调整参数

**动态调整示例：**
```java
ThreadPoolExecutor executor = ...;

// 运行时动态调整
executor.setCorePoolSize(20);
executor.setMaximumPoolSize(50);
executor.setKeepAliveTime(120, TimeUnit.SECONDS);
```

---

## 六、线程池的优雅关闭

### 6.1 关闭方法对比

| 方法 | 说明 | 是否接受新任务 | 是否等待已提交任务 | 是否中断运行中的任务 |
|------|------|----------------|-------------------|---------------------|
| `shutdown()` | 温和关闭 | 否 | 是 | 否 |
| `shutdownNow()` | 立即关闭 | 否 | 否 | 是 |

### 6.2 优雅关闭模板

```java
public void gracefulShutdown(ExecutorService executor) {
    // 1. 停止接收新任务
    executor.shutdown();

    try {
        // 2. 等待已提交任务完成（最多等待60秒）
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            // 3. 超时后强制关闭
            List<Runnable> droppedTasks = executor.shutdownNow();
            System.err.println("丢弃的任务数: " + droppedTasks.size());

            // 4. 再次等待
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("线程池无法终止");
            }
        }
    } catch (InterruptedException e) {
        // 5. 当前线程被中断，强制关闭线程池
        executor.shutdownNow();
        // 恢复中断状态
        Thread.currentThread().interrupt();
    }
}
```

### 6.3 关闭流程图

```
调用 shutdown()
    ↓
等待 awaitTermination(60s)
    ↓
是否超时?
    ↓ 是
调用 shutdownNow()
    ↓
再次等待 awaitTermination(60s)
    ↓
是否超时?
    ↓ 是
记录错误日志
```

---

## 七、常见陷阱与最佳实践

### 7.1 常见陷阱

#### 1. 使用Executors创建线程池
```java
// 错误：可能导致OOM
ExecutorService executor = Executors.newFixedThreadPool(10);

// 正确：明确指定有界队列
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100)
);
```

#### 2. 忘记关闭线程池
```java
// 错误：程序无法退出
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.execute(() -> {});
// 忘记调用 executor.shutdown()

// 正确：使用try-with-resources或finally
try {
    executor.execute(() -> {});
} finally {
    executor.shutdown();
}
```

#### 3. 提交任务后不处理异常
```java
// 错误：异常被吞掉
executor.execute(() -> {
    throw new RuntimeException("错误");  // 异常不会传播
});

// 正确：使用submit()并处理Future
Future<?> future = executor.submit(() -> {
    throw new RuntimeException("错误");
});

try {
    future.get();  // 抛出异常
} catch (ExecutionException e) {
    Throwable cause = e.getCause();  // 获取真实异常
}
```

#### 4. 核心线程数设置过小
```java
// 错误：核心线程太少，大部分任务在队列中等待
new ThreadPoolExecutor(1, 100, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000));

// 正确：根据业务需求合理设置
new ThreadPoolExecutor(10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100));
```

### 7.2 最佳实践

#### 1. 给线程池命名
```java
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("order-pool-%d")  // 业务相关的名称
    .build();

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    namedThreadFactory  // 使用自定义ThreadFactory
);
```

**优点：**
- 便于监控和问题排查
- jstack线程dump时易于识别

#### 2. 设置拒绝策略
```java
// 根据业务选择合适的拒绝策略
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 调用者执行，提供降级
);
```

#### 3. 监控线程池状态
```java
// 定期打印线程池状态
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    System.out.println("活动线程: " + executor.getActiveCount());
    System.out.println("队列大小: " + executor.getQueue().size());
    System.out.println("已完成任务: " + executor.getCompletedTaskCount());
}, 0, 10, TimeUnit.SECONDS);
```

#### 4. 区分不同业务的线程池
```java
// 错误：所有业务共用一个线程池
ExecutorService sharedExecutor = ...;

// 正确：不同业务使用不同线程池
ExecutorService orderExecutor = ...;   // 订单处理
ExecutorService paymentExecutor = ...; // 支付处理
ExecutorService emailExecutor = ...;   // 邮件发送
```

**优点：**
- 业务隔离，互不影响
- 便于监控和调优
- 避免慢任务影响快任务

#### 5. 合理设置队列大小
```java
// 根据内存和业务需求设置队列大小
// 队列大小 = QPS * 平均处理时间 * 安全系数

// 例如：QPS=1000，平均处理时间=100ms，安全系数=2
int queueSize = 1000 * 100 / 1000 * 2 = 200;

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(queueSize)
);
```

---

## 八、线程池原理深入

### 8.1 核心数据结构

```java
// ThreadPoolExecutor核心字段
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
// ctl: 高3位存储运行状态，低29位存储线程数

private final BlockingQueue<Runnable> workQueue;    // 工作队列
private final HashSet<Worker> workers = new HashSet<>();  // 工作线程集合
private final ReentrantLock mainLock = new ReentrantLock();  // 主锁
```

### 8.2 线程池状态

```java
// 5种运行状态
private static final int RUNNING    = -1 << COUNT_BITS;  // 接受新任务，处理队列任务
private static final int SHUTDOWN   =  0 << COUNT_BITS;  // 不接受新任务，处理队列任务
private static final int STOP       =  1 << COUNT_BITS;  // 不接受新任务，不处理队列，中断运行任务
private static final int TIDYING    =  2 << COUNT_BITS;  // 所有任务已终止，线程数为0
private static final int TERMINATED =  3 << COUNT_BITS;  // terminated()方法执行完毕
```

**状态转换：**
```
RUNNING -> SHUTDOWN (调用shutdown())
RUNNING -> STOP (调用shutdownNow())
SHUTDOWN -> TIDYING (队列和线程池都为空)
STOP -> TIDYING (线程池为空)
TIDYING -> TERMINATED (terminated()方法完成)
```

### 8.3 Worker线程

```java
// Worker：工作线程的封装
private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
    final Thread thread;      // 工作线程
    Runnable firstTask;       // 初始任务

    Worker(Runnable firstTask) {
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }

    public void run() {
        runWorker(this);  // 执行任务循环
    }
}
```

**runWorker核心逻辑：**
```java
final void runWorker(Worker w) {
    Runnable task = w.firstTask;
    w.firstTask = null;

    while (task != null || (task = getTask()) != null) {
        // 1. 加锁（保证shutdown时不中断正在执行的任务）
        w.lock();

        try {
            // 2. 执行前钩子
            beforeExecute(w.thread, task);

            // 3. 执行任务
            task.run();

            // 4. 执行后钩子
            afterExecute(task, null);
        } finally {
            // 5. 释放锁
            w.unlock();
        }
    }

    // 6. 线程退出
    processWorkerExit(w);
}
```

---

## 九、总结

### 核心要点

1. **优先使用线程池**：避免频繁创建/销毁线程
2. **手动创建线程池**：不使用Executors，明确指定参数
3. **合理设置参数**：根据业务类型（CPU/IO密集）调整线程数
4. **监控与调优**：定期监控线程池状态，动态调整参数
5. **优雅关闭**：使用shutdown() + awaitTermination()

### 推荐配置模板

```java
// CPU密集型
ThreadPoolExecutor cpuExecutor = new ThreadPoolExecutor(
    Runtime.getRuntime().availableProcessors() + 1,
    Runtime.getRuntime().availableProcessors() + 1,
    0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadFactoryBuilder().setNameFormat("cpu-pool-%d").build(),
    new ThreadPoolExecutor.AbortPolicy()
);

// IO密集型
ThreadPoolExecutor ioExecutor = new ThreadPoolExecutor(
    Runtime.getRuntime().availableProcessors() * 2,
    Runtime.getRuntime().availableProcessors() * 4,
    60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(200),
    new ThreadFactoryBuilder().setNameFormat("io-pool-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

### 学习路径

1. 理解线程池的必要性和优势
2. 掌握ThreadPoolExecutor的核心参数
3. 熟悉任务执行流程和拒绝策略
4. 学习定时任务调度
5. 实践监控、调优和优雅关闭
6. 深入源码理解内部原理

---

## 扩展阅读

- Doug Lea的论文《A Java Fork/Join Framework》
- 《Java并发编程实战》第6-8章
- JDK源码：`java.util.concurrent.ThreadPoolExecutor`
- 美团技术团队：《Java线程池实现原理及其在美团业务中的实践》
