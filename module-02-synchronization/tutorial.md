# Module 02: 同步机制教程

## 1. 为什么需要同步？

### 1.1 线程安全问题

当多个线程同时访问共享数据时，如果不加控制，会导致数据不一致：

```java
public class Counter {
    private int count = 0;

    public void increment() {
        count++;  // 非原子操作！包含三步：读取、加1、写回
    }
}
```

**问题分析**：
- 线程A读取count=0
- 线程B读取count=0
- 线程A执行count+1，写回count=1
- 线程B执行count+1，写回count=1
- 结果：两次increment，但count只增加了1

### 1.2 竞态条件（Race Condition）

多个线程的执行结果依赖于它们执行的相对时序。

---

## 2. synchronized关键字

### 2.1 基本用法

**方式1：同步实例方法**
```java
public synchronized void increment() {
    count++;  // 锁住整个方法
}
```
等价于：
```java
public void increment() {
    synchronized(this) {
        count++;
    }
}
```

**方式2：同步静态方法**
```java
public static synchronized void staticMethod() {
    // 锁的是Class对象
}
```
等价于：
```java
public static void staticMethod() {
    synchronized(Counter.class) {
        // ...
    }
}
```

**方式3：同步代码块**
```java
public void increment() {
    synchronized(lockObject) {  // 可以指定任意对象作为锁
        count++;
    }
}
```

### 2.2 synchronized的特性

#### ① 互斥性（Mutual Exclusion）
同一时刻，只有一个线程可以持有锁，其他线程必须等待。

#### ② 可见性（Visibility）
- 线程A释放锁时，会将修改的变量刷新到主内存
- 线程B获取锁时，会从主内存读取最新值

#### ③ 可重入性（Reentrant）
同一个线程可以多次获取同一把锁：

```java
public synchronized void outer() {
    inner();  // 同一线程，可以再次获取this锁
}

public synchronized void inner() {
    // ...
}
```

### 2.3 锁的粒度

**粗粒度锁（简单但性能差）**：
```java
public synchronized void method() {
    // 大量代码
}
```

**细粒度锁（复杂但性能好）**：
```java
public void method() {
    // 非临界区代码
    synchronized(this) {
        // 只保护必要的临界区
    }
    // 非临界区代码
}
```

**最佳实践**：缩小同步范围，只锁必要的代码。

---

## 3. wait/notify机制

### 3.1 基本概念

- `wait()`：释放锁，进入等待状态
- `notify()`：唤醒一个等待线程
- `notifyAll()`：唤醒所有等待线程

**关键点**：
1. 必须在`synchronized`块中调用
2. 调用wait会释放锁，被notify后需要重新竞争锁
3. wait可能被虚假唤醒（spurious wakeup），需要在while循环中检查条件

### 3.2 标准使用模式

**等待方（消费者）**：
```java
synchronized(lock) {
    while (!condition) {  // 使用while，不是if！
        lock.wait();
    }
    // 执行业务逻辑
}
```

**通知方（生产者）**：
```java
synchronized(lock) {
    // 改变条件
    condition = true;
    lock.notifyAll();  // 推荐使用notifyAll
}
```

### 3.3 生产者-消费者示例

```java
public class BoundedBuffer<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    // 生产者
    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();  // 队列满，等待
        }
        queue.add(item);
        notifyAll();  // 唤醒消费者
    }

    // 消费者
    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();  // 队列空，等待
        }
        T item = queue.remove();
        notifyAll();  // 唤醒生产者
        return item;
    }
}
```

### 3.4 为什么用while不是if？

**错误示例**：
```java
synchronized(lock) {
    if (!condition) {
        lock.wait();
    }
    // 可能condition已经变化了！
}
```

**原因**：
1. **虚假唤醒**：wait可能在没有notify的情况下返回
2. **竞争条件**：被notify后到重新获得锁之间，条件可能又被其他线程改变

---

## 4. 死锁

### 4.1 死锁的四个必要条件

1. **互斥条件**：资源不能被共享
2. **占有并等待**：持有资源的同时等待其他资源
3. **不可剥夺**：不能强制抢占资源
4. **循环等待**：存在线程-资源的循环等待链

### 4.2 典型死锁场景

```java
public class DeadlockDemo {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public void method1() {
        synchronized(lock1) {           // 线程A获得lock1
            synchronized(lock2) {       // 等待lock2
                // ...
            }
        }
    }

    public void method2() {
        synchronized(lock2) {           // 线程B获得lock2
            synchronized(lock1) {       // 等待lock1
                // ...
            }
        }
    }
}
```

### 4.3 避免死锁的策略

#### ① 锁排序（Lock Ordering）
```java
// 始终按照锁的hashCode顺序获取
private void transferMoney(Account from, Account to, int amount) {
    Account first = from.hashCode() < to.hashCode() ? from : to;
    Account second = first == from ? to : from;

    synchronized(first) {
        synchronized(second) {
            // 转账逻辑
        }
    }
}
```

#### ② 使用超时（后续Lock章节学习）
```java
if (lock.tryLock(timeout, TimeUnit.SECONDS)) {
    try {
        // ...
    } finally {
        lock.unlock();
    }
}
```

#### ③ 避免嵌套锁
尽量避免在持有锁的情况下再去获取另一个锁。

#### ④ 使用死锁检测工具
- `jstack` 查看线程堆栈
- `jconsole` / `VisualVM` 图形化工具

### 4.4 检测死锁

**使用jstack**：
```bash
# 找到Java进程PID
jps

# 生成线程dump
jstack <pid> > thread_dump.txt

# 查找"Found one Java-level deadlock"
```

---

## 5. 其他并发问题

### 5.1 活锁（Livelock）

线程不断响应对方的动作，但没有任何进展。

**示例**：两人在走廊相遇，同时向左让路，然后同时向右让路，如此循环。

**解决方案**：引入随机性或退让机制。

### 5.2 饥饿（Starvation）

某些线程长期得不到执行机会。

**原因**：
- 高优先级线程总是抢占资源
- 锁的不公平竞争

**解决方案**：
- 使用公平锁（下一模块学习）
- 合理设置线程优先级

---

## 6. synchronized的底层原理

### 6.1 Monitor机制

每个Java对象都关联一个Monitor（监视器）：

```
Object Header
├── Mark Word（锁状态、hashCode等）
└── Class Pointer

Monitor
├── Owner（当前持有锁的线程）
├── EntryList（等待获取锁的线程队列）
└── WaitSet（调用wait()的线程集合）
```

### 6.2 字节码层面

**同步方法**：
```
public synchronized void increment();
  flags: ACC_PUBLIC, ACC_SYNCHRONIZED  // 方法标志
```

**同步代码块**：
```
0: aload_0
1: dup
2: astore_1
3: monitorenter       // 进入监视器
4: aload_0
5: dup
6: getfield      #2
9: iconst_1
10: iadd
11: putfield      #2
14: aload_1
15: monitorexit       // 退出监视器
```

### 6.3 锁优化（JDK 6+）

1. **偏向锁**：大部分情况下，锁总是被同一线程获取
2. **轻量级锁**：使用CAS避免互斥量（Mutex）开销
3. **重量级锁**：依赖操作系统的Mutex
4. **锁消除**：JIT编译器发现不可能有竞争时，消除锁
5. **锁粗化**：合并相邻的同步块

---

## 7. 最佳实践

### 7.1 不要在锁内做耗时操作
```java
// ❌ 错误
synchronized(lock) {
    // 网络IO、磁盘IO等耗时操作
    networkRequest();
}

// ✅ 正确
Object data = fetchData();  // 在锁外准备数据
synchronized(lock) {
    // 只在锁内做最少的必要操作
    updateSharedState(data);
}
```

### 7.2 使用私有锁对象
```java
// ❌ 不推荐
public class MyClass {
    public synchronized void method() { }  // 锁的是this，外部可访问
}

// ✅ 推荐
public class MyClass {
    private final Object lock = new Object();

    public void method() {
        synchronized(lock) { }  // 私有锁，外部无法访问
    }
}
```

### 7.3 优先使用notifyAll()
```java
// 使用notifyAll()避免信号丢失
synchronized(lock) {
    condition = true;
    lock.notifyAll();  // 而不是notify()
}
```

### 7.4 文档化锁策略
```java
/**
 * 线程安全的计数器
 *
 * 锁策略：
 * - 所有公共方法通过内部锁（this）同步
 * - count变量只在持有锁的情况下访问
 */
public class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }
}
```

---

## 8. 何时使用synchronized vs Lock？

| 特性 | synchronized | Lock（下一模块） |
|-----|-------------|-----------------|
| 使用简单 | ✅ | ❌ |
| 自动释放锁 | ✅ | ❌（需手动unlock） |
| 可中断 | ❌ | ✅ |
| 尝试获取锁 | ❌ | ✅（tryLock） |
| 读写分离 | ❌ | ✅（ReadWriteLock） |
| 公平锁 | ❌ | ✅ |
| 条件变量 | 1个（wait/notify） | 多个（Condition） |

**建议**：
- 简单场景：优先使用synchronized（JVM优化好）
- 需要高级功能：使用Lock

---

## 9. 总结

### 核心要点
1. **synchronized**提供互斥、可见性、可重入
2. **wait/notify**用于线程协作，必须在同步块中使用
3. **死锁**的四个条件，破坏任意一个即可避免
4. **缩小锁范围**，避免在锁内做耗时操作
5. **使用while检查条件**，防止虚假唤醒

### 常见陷阱
- ❌ 在if而非while中检查条件
- ❌ 使用notify()导致信号丢失
- ❌ 在锁内做IO操作
- ❌ 锁的是可变对象（如String）

---

## 10. 练习建议

1. **E01_BoundedBuffer**：巩固wait/notify使用
2. **E02_ReadWriteLock**：理解读写分离思想
3. **E03_PhilosophersDinner**：死锁避免策略（面试常见）

完成练习后，对比参考答案，理解不同实现的优缺点。

---

下一模块将学习`Lock`接口和`ReentrantLock`，它们提供了比synchronized更灵活的锁控制。
