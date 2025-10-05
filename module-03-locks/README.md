# Module 03: Lock框架

## 学习目标

完成本模块后，你将掌握：
- ✅ Lock接口与synchronized的区别
- ✅ ReentrantLock的使用（lock/unlock、tryLock、lockInterruptibly）
- ✅ 公平锁与非公平锁的区别
- ✅ Condition条件变量的使用（await/signal）
- ✅ ReadWriteLock读写锁模式
- ✅ Lock的最佳实践与常见陷阱

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解Lock框架理论

### 💻 演示代码（Demo）
1. **D01_ReentrantLockBasics** - ReentrantLock基础用法
2. **D02_ConditionVariable** - Condition条件变量详解
3. **D03_ReadWriteLock** - ReadWriteLock读写锁演示

### ✏️ 练习题（Exercises）
1. **E01_BankTransfer** 🟢 - 线程安全的银行转账
2. **E02_CustomBlockingQueue** 🟡 - 自定义阻塞队列
3. **E03_CacheWithReadWriteLock** 🟡 - 读写锁缓存实现

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行ReentrantLock基础示例
mvn exec:java -Dexec.mainClass="com.concurrency.locks.demo.D01_ReentrantLockBasics"

# 运行Condition示例
mvn exec:java -Dexec.mainClass="com.concurrency.locks.demo.D02_ConditionVariable"

# 运行ReadWriteLock示例
mvn exec:java -Dexec.mainClass="com.concurrency.locks.demo.D03_ReadWriteLock"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/locks/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=LocksTest
```

---

## 知识点清单

### 核心API

#### Lock接口
- `lock()` - 获取锁（阻塞）
- `unlock()` - 释放锁
- `tryLock()` - 尝试获取锁（非阻塞）
- `tryLock(long time, TimeUnit unit)` - 限时获取锁
- `lockInterruptibly()` - 可中断的锁获取
- `newCondition()` - 创建条件变量

#### ReentrantLock类
- 可重入锁（同一线程可多次获取）
- 支持公平/非公平模式
- 提供更灵活的锁控制

#### Condition接口
- `await()` - 等待（类似wait）
- `signal()` - 唤醒一个（类似notify）
- `signalAll()` - 唤醒所有（类似notifyAll）
- `await(long time, TimeUnit unit)` - 限时等待

#### ReadWriteLock接口
- `readLock()` - 获取读锁
- `writeLock()` - 获取写锁
- 读锁可共享，写锁互斥
- 适用于读多写少的场景

---

## Lock vs synchronized

| 特性 | Lock | synchronized |
|------|------|--------------|
| 锁的获取 | 手动lock() | 自动获取 |
| 锁的释放 | 手动unlock() | 自动释放 |
| 可中断性 | lockInterruptibly() | 不可中断 |
| 超时获取 | tryLock(timeout) | 不支持 |
| 条件变量 | 多个Condition | 一个监视器 |
| 公平性 | 可选公平/非公平 | 非公平 |
| 灵活性 | 高 | 低 |
| 使用复杂度 | 较复杂 | 简单 |

---

## 常见问题

**Q: 什么时候使用Lock而不是synchronized？**
A:
- 需要尝试获取锁（tryLock）
- 需要可中断的锁获取
- 需要超时控制
- 需要公平锁
- 需要多个条件变量

**Q: 如何确保Lock一定被释放？**
A: 始终在finally块中调用unlock()：
```java
lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock();  // 确保锁一定被释放
}
```

**Q: 公平锁和非公平锁有什么区别？**
A:
- **公平锁**：按照请求顺序获取锁，避免饥饿，但性能较低
- **非公平锁**：不保证顺序，可能插队，性能较高（默认）

**Q: ReadWriteLock适用于什么场景？**
A: 读多写少的场景，如缓存系统。多个读操作可并发执行，提高性能。

**Q: Condition和wait/notify有什么区别？**
A:
- Condition可以有多个，wait/notify只有一个监视器
- Condition提供更灵活的等待/通知机制
- Condition可以实现更精确的线程间通信

---

## 学习建议

1. **对比学习**：每个Lock特性都与synchronized对比理解
2. **必须掌握**：finally中unlock的习惯
3. **理解场景**：什么时候用Lock，什么时候用synchronized
4. **条件变量**：理解Condition如何实现精确的线程协作
5. **性能权衡**：理解公平锁的性能开销

---

## 最佳实践

### 1. 永远在finally中释放锁
```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();  // 确保释放
}
```

### 2. 使用tryLock避免死锁
```java
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁失败的处理
}
```

### 3. 优先使用ReadWriteLock优化读多写少场景
```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();
```

### 4. 使用Condition实现精确通知
```java
Lock lock = new ReentrantLock();
Condition condition = lock.newCondition();

// 等待
lock.lock();
try {
    while (!conditionMet) {
        condition.await();
    }
} finally {
    lock.unlock();
}

// 通知
lock.lock();
try {
    conditionMet = true;
    condition.signal();
} finally {
    lock.unlock();
}
```

---

## 常见陷阱

### 1. 忘记释放锁
```java
// ✗ 错误：没有finally
lock.lock();
doSomething();  // 如果抛异常，锁永远不会释放
lock.unlock();

// ✓ 正确
lock.lock();
try {
    doSomething();
} finally {
    lock.unlock();
}
```

### 2. 不匹配的lock/unlock
```java
// ✗ 错误：unlock次数不匹配
lock.lock();
lock.lock();  // 重入2次
lock.unlock();  // 只释放1次，锁未完全释放
```

### 3. 条件变量使用错误的锁
```java
// ✗ 错误：condition和lock不匹配
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

---

## 扩展阅读

- [Oracle并发教程 - Lock Objects](https://docs.oracle.com/javase/tutorial/essential/concurrency/newlocks.html)
- 《Java并发编程实战》第13-14章
- JDK源码：`java.util.concurrent.locks`包

---

## 下一步

完成本模块后，继续学习：
👉 **[Module 04: 线程池](../module-04-executors/)**

学习`ExecutorService`和线程池的使用，避免手动管理线程
