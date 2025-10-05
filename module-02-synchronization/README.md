# Module 02: 同步机制

## 学习目标

完成本模块后，你将掌握：
- ✅ synchronized关键字的使用（方法同步、代码块同步）
- ✅ 对象监视器（Monitor）和锁的概念
- ✅ wait/notify/notifyAll机制
- ✅ 死锁的产生、检测与避免
- ✅ 线程间的协作模式（生产者-消费者）

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解同步机制理论

### 💻 演示代码（Demo）
1. **D01_SynchronizedBasics** - synchronized基础用法
2. **D02_WaitNotify** - wait/notify机制详解
3. **D03_Deadlock** - 死锁演示与分析

### ✏️ 练习题（Exercises）
1. **E01_BoundedBuffer** 🟢 - 有界缓冲区实现
2. **E02_ReadWriteLock** 🟡 - 简易读写锁
3. **E03_PhilosophersDinner** 🔴 - 哲学家就餐问题 ⭐

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行synchronized示例
mvn exec:java -Dexec.mainClass="com.concurrency.sync.demo.D01_SynchronizedBasics"

# 运行wait/notify示例
mvn exec:java -Dexec.mainClass="com.concurrency.sync.demo.D02_WaitNotify"

# 运行死锁示例
mvn exec:java -Dexec.mainClass="com.concurrency.sync.demo.D03_Deadlock"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/sync/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=SynchronizationTest
```

---

## 知识点清单

### 核心API
- `synchronized` 关键字
  - 同步方法
  - 同步代码块
  - 类锁 vs 对象锁

- `Object` 类的监视器方法
  - `wait()` - 释放锁并等待
  - `wait(long timeout)` - 限时等待
  - `notify()` - 唤醒一个等待线程
  - `notifyAll()` - 唤醒所有等待线程

### 重要概念
- **监视器（Monitor）**：Java的内置锁机制
- **临界区（Critical Section）**：需要互斥访问的代码区域
- **死锁（Deadlock）**：多个线程相互等待对方释放资源
- **活锁（Livelock）**：线程持续响应但无法前进
- **饥饿（Starvation）**：线程长期得不到执行机会

---

## 常见问题

**Q: synchronized锁的是什么？**
A:
- 同步方法：锁的是对象实例（非static）或Class对象（static）
- 同步代码块：锁的是括号中指定的对象

**Q: wait()必须在synchronized中调用吗？**
A: 是的，否则会抛出`IllegalMonitorStateException`。wait/notify必须在持有对象锁的情况下调用。

**Q: notify()和notifyAll()的区别？**
A:
- `notify()`：随机唤醒一个等待线程
- `notifyAll()`：唤醒所有等待线程（推荐使用，避免信号丢失）

**Q: 如何避免死锁？**
A:
1. 避免嵌套锁
2. 按顺序获取锁（锁排序）
3. 使用超时机制（tryLock）
4. 死锁检测与恢复

---

## 学习建议

1. **理解锁的本质**：每个Java对象都有一个监视器
2. **wait/notify典型场景**：生产者-消费者、有界缓冲区
3. **死锁实验**：故意制造死锁，使用`jstack`分析
4. **性能考虑**：synchronized是重量级锁，避免过度同步
5. **最佳实践**：尽量缩小同步范围，只保护必要的临界区

---

## 扩展阅读

- [Oracle并发教程 - Synchronization](https://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html)
- 《Java并发编程实战》第2-4章
- JDK源码：`java.lang.Object`的wait/notify实现

---

## 下一步

完成本模块后，继续学习：
👉 **[Module 03: Lock框架](../module-03-locks/)**

学习`ReentrantLock`等显式锁，提供比synchronized更灵活的控制
