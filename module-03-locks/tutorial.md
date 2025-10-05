# Lock框架教程

## 一、为什么需要Lock？

### 1.1 synchronized的局限性

synchronized虽然简单易用，但存在一些限制：

1. **无法中断**：一旦阻塞在synchronized上，无法响应中断
2. **无法超时**：不能设置获取锁的超时时间
3. **无法尝试**：不能尝试获取锁而不阻塞
4. **公平性**：无法实现公平锁
5. **单一条件**：只有一个wait/notify队列

### 1.2 Lock接口的优势

Java 5引入了`java.util.concurrent.locks`包，提供了更灵活的锁机制：

```java
public interface Lock {
    void lock();                                      // 获取锁（阻塞）
    void unlock();                                    // 释放锁
    boolean tryLock();                                // 尝试获取锁（非阻塞）
    boolean tryLock(long time, TimeUnit unit);        // 限时获取锁
    void lockInterruptibly() throws InterruptedException;  // 可中断
    Condition newCondition();                         // 创建条件变量
}
```

**优势**：
- 更灵活的锁获取方式
- 可中断、可超时
- 支持公平锁
- 支持多个条件变量

---

## 二、ReentrantLock基础

### 2.1 什么是可重入锁？

**可重入（Reentrant）**：同一线程可以多次获取同一个锁。

```java
Lock lock = new ReentrantLock();

lock.lock();
try {
    System.out.println("第一次获取锁");

    lock.lock();  // 同一线程可以再次获取
    try {
        System.out.println("第二次获取锁（重入）");
    } finally {
        lock.unlock();  // 第二次释放
    }
} finally {
    lock.unlock();  // 第一次释放
}
```

**重要**：加锁几次就要解锁几次！

### 2.2 基本用法

#### 标准模式
```java
Lock lock = new ReentrantLock();

lock.lock();  // 获取锁
try {
    // 临界区代码
    // 访问共享资源
} finally {
    lock.unlock();  // 必须在finally中释放锁
}
```

**为什么用finally？**
- 确保即使发生异常也能释放锁
- 避免死锁

#### 错误示例
```java
// ✗ 错误1：忘记释放锁
lock.lock();
doSomething();  // 如果抛异常，锁永远不会释放
lock.unlock();

// ✗ 错误2：没有用finally
lock.lock();
try {
    doSomething();
}
lock.unlock();  // 如果try中抛异常，这行代码不会执行

// ✗ 错误3：不匹配的lock/unlock
lock.lock();
lock.lock();  // 重入2次
lock.unlock();  // 只释放1次，锁泄漏！
```

### 2.3 ReentrantLock vs synchronized

```java
// synchronized方式
synchronized (lock) {
    // 临界区
}

// ReentrantLock方式
lock.lock();
try {
    // 临界区
} finally {
    lock.unlock();
}
```

| 特性 | ReentrantLock | synchronized |
|------|---------------|--------------|
| 代码复杂度 | 较高（需要手动unlock） | 低（自动释放） |
| 灵活性 | 高 | 低 |
| 可中断性 | 支持 | 不支持 |
| 超时控制 | 支持 | 不支持 |
| 公平性 | 可选 | 不可选 |
| 条件变量 | 多个 | 一个 |
| 性能 | 相近 | 相近 |

**选择建议**：
- 简单场景：优先使用synchronized
- 需要高级特性：使用ReentrantLock

---

## 三、Lock的高级特性

### 3.1 tryLock - 尝试获取锁

#### 非阻塞尝试
```java
Lock lock = new ReentrantLock();

if (lock.tryLock()) {  // 立即返回，不阻塞
    try {
        System.out.println("成功获取锁");
        // 执行业务逻辑
    } finally {
        lock.unlock();
    }
} else {
    System.out.println("无法获取锁，执行替代方案");
    // 执行降级逻辑
}
```

#### 限时尝试
```java
// 最多等待1秒
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        System.out.println("成功获取锁");
    } finally {
        lock.unlock();
    }
} else {
    System.out.println("超时，未获取到锁");
}
```

**使用场景**：
- 避免死锁（超时后放弃）
- 降级处理（获取不到锁时执行其他逻辑）
- 性能优化（避免长时间阻塞）

### 3.2 lockInterruptibly - 可中断的锁

```java
Lock lock = new ReentrantLock();

Thread t = new Thread(() -> {
    try {
        lock.lockInterruptibly();  // 可被中断
        try {
            System.out.println("获取到锁，执行任务");
            Thread.sleep(10000);  // 模拟长时间任务
        } finally {
            lock.unlock();
        }
    } catch (InterruptedException e) {
        System.out.println("等待锁时被中断");
    }
});

t.start();
Thread.sleep(100);
t.interrupt();  // 中断线程
```

**对比**：
```java
// lock()：不可中断，即使调用interrupt()也会继续等待
lock.lock();

// lockInterruptibly()：可中断，调用interrupt()后抛出InterruptedException
lock.lockInterruptibly();
```

**使用场景**：
- 需要响应中断的任务
- 可取消的操作

### 3.3 公平锁 vs 非公平锁

#### 非公平锁（默认）
```java
Lock lock = new ReentrantLock();  // 默认非公平
// 或
Lock lock = new ReentrantLock(false);  // 显式指定非公平
```

**特点**：
- 允许"插队"
- 性能更高
- 可能导致线程饥饿

#### 公平锁
```java
Lock lock = new ReentrantLock(true);  // 公平锁
```

**特点**：
- 按照请求顺序获取锁（FIFO）
- 避免线程饥饿
- 性能较低（需要维护队列）

#### 演示
```java
public class FairVsUnfair {
    public static void main(String[] args) {
        testLock(new ReentrantLock(false), "非公平锁");
        testLock(new ReentrantLock(true), "公平锁");
    }

    private static void testLock(Lock lock, String name) {
        System.out.println("\n=== " + name + " ===");

        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < 2; j++) {
                    lock.lock();
                    try {
                        System.out.println("线程" + threadId + " 获取锁");
                    } finally {
                        lock.unlock();
                    }
                }
            }).start();
        }
    }
}
```

**选择建议**：
- 默认使用非公平锁（性能更好）
- 需要严格公平性时使用公平锁

---

## 四、Condition条件变量

### 4.1 Condition vs wait/notify

**synchronized + wait/notify**：
```java
synchronized (lock) {
    while (!condition) {
        lock.wait();  // 等待
    }
    // 执行业务
}

synchronized (lock) {
    condition = true;
    lock.notify();  // 通知
}
```

**Lock + Condition**：
```java
Lock lock = new ReentrantLock();
Condition condition = lock.newCondition();

lock.lock();
try {
    while (!conditionMet) {
        condition.await();  // 等待
    }
    // 执行业务
} finally {
    lock.unlock();
}

lock.lock();
try {
    conditionMet = true;
    condition.signal();  // 通知
} finally {
    lock.unlock();
}
```

### 4.2 Condition的优势

**1. 多个条件变量**

```java
Lock lock = new ReentrantLock();
Condition notFull = lock.newCondition();   // 条件1：非满
Condition notEmpty = lock.newCondition();  // 条件2：非空

// 生产者：等待"非满"条件
lock.lock();
try {
    while (isFull()) {
        notFull.await();  // 等待非满
    }
    addItem();
    notEmpty.signal();  // 通知非空
} finally {
    lock.unlock();
}

// 消费者：等待"非空"条件
lock.lock();
try {
    while (isEmpty()) {
        notEmpty.await();  // 等待非空
    }
    removeItem();
    notFull.signal();  // 通知非满
} finally {
    lock.unlock();
}
```

**优势**：
- 精确通知：生产者只唤醒消费者，消费者只唤醒生产者
- 避免"惊群效应"：不会唤醒不需要的线程

**2. 更丰富的API**

```java
Condition condition = lock.newCondition();

// 等待方法
condition.await();                           // 无限等待
condition.await(1, TimeUnit.SECONDS);        // 限时等待
condition.awaitUninterruptibly();            // 不可中断等待
condition.awaitUntil(new Date(deadline));    // 等待到指定时间

// 通知方法
condition.signal();      // 唤醒一个等待线程
condition.signalAll();   // 唤醒所有等待线程
```

### 4.3 生产者-消费者模式

```java
public class BoundedBuffer<T> {
    private final Queue<T> queue;
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    // 生产者：添加元素
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // 等待队列非满
            while (queue.size() == capacity) {
                notFull.await();
            }

            queue.add(item);
            System.out.println("生产: " + item);

            // 通知消费者：队列非空
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // 消费者：取出元素
    public T take() throws InterruptedException {
        lock.lock();
        try {
            // 等待队列非空
            while (queue.isEmpty()) {
                notEmpty.await();
            }

            T item = queue.remove();
            System.out.println("消费: " + item);

            // 通知生产者：队列非满
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

### 4.4 Condition最佳实践

**1. 使用while而不是if**
```java
// ✗ 错误：使用if（虚假唤醒）
if (queue.isEmpty()) {
    condition.await();
}

// ✓ 正确：使用while
while (queue.isEmpty()) {
    condition.await();
}
```

**为什么用while？**
- 防止虚假唤醒（spurious wakeup）
- 确保条件真正满足

**2. await前必须持有锁**
```java
// ✗ 错误：未持有锁
condition.await();  // IllegalMonitorStateException

// ✓ 正确：在lock/unlock之间
lock.lock();
try {
    condition.await();
} finally {
    lock.unlock();
}
```

**3. 优先使用signalAll**
```java
// signal()：只唤醒一个线程
condition.signal();

// signalAll()：唤醒所有等待线程（更安全）
condition.signalAll();
```

---

## 五、ReadWriteLock读写锁

### 5.1 为什么需要读写锁？

**问题**：使用普通锁，读和写操作都是互斥的。

```java
Lock lock = new ReentrantLock();

// 读操作
lock.lock();
try {
    return data;  // 只读，不修改数据
} finally {
    lock.unlock();
}

// 写操作
lock.lock();
try {
    data = newValue;  // 修改数据
} finally {
    lock.unlock();
}
```

**问题**：
- 多个读操作可以并发执行，但普通锁强制串行
- 读多写少的场景性能低下

**解决方案**：ReadWriteLock

### 5.2 读写锁的规则

```
读锁（共享锁）：
- 多个线程可以同时持有读锁
- 读-读：不互斥（可并发）
- 读-写：互斥（写操作需等待所有读锁释放）

写锁（排他锁）：
- 只有一个线程可以持有写锁
- 写-读：互斥（读操作需等待写锁释放）
- 写-写：互斥（写操作之间串行）
```

### 5.3 基本用法

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SharedResource {
    private int data;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    // 读操作：使用读锁
    public int read() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 读取: " + data);
            return data;
        } finally {
            readLock.unlock();
        }
    }

    // 写操作：使用写锁
    public void write(int value) {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 写入: " + value);
            data = value;
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 5.4 读写锁的性能优势

**场景**：10个线程，9个读1个写

```java
// 使用普通锁：所有操作串行
Lock lock = new ReentrantLock();
// 性能：10个操作全部串行执行

// 使用读写锁：读操作并发
ReadWriteLock rwLock = new ReentrantReadWriteLock();
// 性能：9个读操作并发执行，1个写操作独占
```

**性能提升**：读多写少的场景下，性能显著提升。

### 5.5 锁降级

ReentrantReadWriteLock支持**锁降级**（Write → Read），不支持锁升级（Read → Write）。

```java
// ✓ 锁降级（Write → Read）
writeLock.lock();
try {
    // 修改数据
    data = newValue;

    // 降级为读锁
    readLock.lock();  // 持有写锁时获取读锁
} finally {
    writeLock.unlock();  // 释放写锁（仍持有读锁）
}
try {
    // 使用读锁读取数据
    return data;
} finally {
    readLock.unlock();
}

// ✗ 锁升级（Read → Write）- 会死锁！
readLock.lock();
try {
    writeLock.lock();  // 死锁！读锁无法升级为写锁
    try {
        data = newValue;
    } finally {
        writeLock.unlock();
    }
} finally {
    readLock.unlock();
}
```

### 5.6 缓存系统示例

```java
public class Cache<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    // 读取缓存
    public V get(K key) {
        readLock.lock();
        try {
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    // 写入缓存
    public void put(K key, V value) {
        writeLock.lock();
        try {
            map.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    // 清空缓存
    public void clear() {
        writeLock.lock();
        try {
            map.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
```

---

## 六、Lock的最佳实践

### 1. 永远在finally中unlock
```java
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();  // 确保释放
}
```

### 2. 避免在lock和try之间执行代码
```java
// ✗ 不好
lock.lock();
System.out.println("获取锁");  // 如果这里抛异常，锁没释放
try {
    // 业务逻辑
} finally {
    lock.unlock();
}

// ✓ 好
lock.lock();
try {
    System.out.println("获取锁");
    // 业务逻辑
} finally {
    lock.unlock();
}
```

### 3. tryLock时检查返回值
```java
// ✗ 错误：没检查返回值
lock.tryLock();
try {
    // 可能未获取到锁就执行了
} finally {
    lock.unlock();
}

// ✓ 正确
if (lock.tryLock()) {
    try {
        // 确保获取到锁
    } finally {
        lock.unlock();
    }
}
```

### 4. 缩小锁的范围
```java
// ✗ 不好：锁范围过大
lock.lock();
try {
    prepareData();      // 不需要锁保护
    modifySharedData(); // 需要锁保护
    logResult();        // 不需要锁保护
} finally {
    lock.unlock();
}

// ✓ 好：只锁关键代码
prepareData();
lock.lock();
try {
    modifySharedData();
} finally {
    lock.unlock();
}
logResult();
```

### 5. 使用tryLock避免死锁
```java
// 转账操作：避免死锁
public boolean transfer(Account from, Account to, int amount) {
    if (from.lock.tryLock()) {
        try {
            if (to.lock.tryLock()) {
                try {
                    // 执行转账
                    from.balance -= amount;
                    to.balance += amount;
                    return true;
                } finally {
                    to.lock.unlock();
                }
            }
        } finally {
            from.lock.unlock();
        }
    }
    return false;  // 获取锁失败
}
```

---

## 七、性能考虑

### 7.1 Lock vs synchronized 性能

**Java 6+**：两者性能相近
- JDK 6引入了锁优化（偏向锁、轻量级锁、自旋锁）
- synchronized性能已大幅提升

**选择依据**：
- 简单场景：synchronized（代码简洁）
- 需要高级特性：Lock（功能丰富）

### 7.2 公平锁的代价

```java
// 非公平锁（默认）
Lock lock = new ReentrantLock();  // 高性能

// 公平锁
Lock lock = new ReentrantLock(true);  // 性能较低
```

**公平锁的代价**：
- 需要维护FIFO队列
- 线程切换更频繁
- 吞吐量降低

**建议**：除非明确需要公平性，否则使用非公平锁。

### 7.3 读写锁的适用场景

**适用**：读多写少
- 读操作 >> 写操作
- 读操作耗时较长

**不适用**：写操作频繁
- 读操作 ≈ 写操作
- 读写锁的开销 > 性能提升

---

## 八、常见陷阱与问题

### 1. 忘记unlock导致死锁
```java
// ✗ 致命错误
lock.lock();
doSomething();  // 如果抛异常，unlock永远不会执行
lock.unlock();

// ✓ 正确
lock.lock();
try {
    doSomething();
} finally {
    lock.unlock();
}
```

### 2. 重入次数不匹配
```java
lock.lock();
lock.lock();  // 重入2次
// ... 业务逻辑
lock.unlock();  // 只解锁1次，锁泄漏！
```

### 3. 在不同对象上await/signal
```java
Lock lock1 = new ReentrantLock();
Lock lock2 = new ReentrantLock();
Condition condition = lock1.newCondition();

lock2.lock();  // 错误的锁
try {
    condition.await();  // IllegalMonitorStateException
} finally {
    lock2.unlock();
}
```

### 4. 读写锁的锁升级
```java
// ✗ 死锁：读锁无法升级为写锁
readLock.lock();
try {
    writeLock.lock();  // 死锁！
    try {
        // ...
    } finally {
        writeLock.unlock();
    }
} finally {
    readLock.unlock();
}
```

### 5. tryLock后未检查返回值
```java
// ✗ 错误
lock.tryLock();  // 可能返回false
try {
    // 可能在未获取锁的情况下执行
} finally {
    lock.unlock();  // 可能抛出IllegalMonitorStateException
}

// ✓ 正确
if (lock.tryLock()) {
    try {
        // 确保获取到锁
    } finally {
        lock.unlock();
    }
}
```

---

## 九、实战案例

### 案例1：银行转账（避免死锁）

```java
public class BankAccount {
    private int balance;
    private final Lock lock = new ReentrantLock();

    public boolean transfer(BankAccount to, int amount) {
        // 使用tryLock避免死锁
        if (this.lock.tryLock()) {
            try {
                if (to.lock.tryLock()) {
                    try {
                        if (this.balance >= amount) {
                            this.balance -= amount;
                            to.balance += amount;
                            return true;
                        }
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                this.lock.unlock();
            }
        }
        return false;
    }
}
```

### 案例2：阻塞队列（Condition）

```java
public class BlockingQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            T item = queue.remove();
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

### 案例3：缓存系统（ReadWriteLock）

```java
public class CachedData {
    private Object data;
    private boolean cacheValid;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public Object getData() {
        rwLock.readLock().lock();
        try {
            if (!cacheValid) {
                // 需要更新缓存，释放读锁
                rwLock.readLock().unlock();
                rwLock.writeLock().lock();
                try {
                    // 双重检查
                    if (!cacheValid) {
                        data = loadDataFromDB();
                        cacheValid = true;
                    }
                    // 锁降级：持有写锁时获取读锁
                    rwLock.readLock().lock();
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
            return data;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private Object loadDataFromDB() {
        // 从数据库加载数据
        return new Object();
    }
}
```

---

## 十、总结

### 核心要点

1. **Lock vs synchronized**
   - synchronized：简单、自动释放
   - Lock：灵活、需手动释放

2. **ReentrantLock特性**
   - 可重入
   - 支持公平/非公平
   - tryLock/lockInterruptibly

3. **Condition优势**
   - 多个条件变量
   - 精确通知

4. **ReadWriteLock**
   - 读多写少场景
   - 读锁共享，写锁独占

5. **最佳实践**
   - finally中unlock
   - 缩小锁范围
   - 避免嵌套锁
   - 使用tryLock防死锁

### 使用场景选择

| 场景 | 推荐方案 |
|------|----------|
| 简单互斥 | synchronized |
| 需要超时/中断 | ReentrantLock |
| 精确线程协作 | Condition |
| 读多写少 | ReadWriteLock |
| 避免死锁 | tryLock |

### 下一步学习

掌握Lock框架后，继续学习：
- 线程池（ExecutorService）
- 并发集合（ConcurrentHashMap）
- 原子类（AtomicInteger）

---

## 参考资料

- [Java Lock接口文档](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/locks/Lock.html)
- 《Java并发编程实战》第13-14章
- [Doug Lea的并发编程论文](http://gee.cs.oswego.edu/dl/papers/aqs.pdf)
