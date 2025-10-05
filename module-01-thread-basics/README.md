# Module 01: 线程基础

## 学习目标

完成本模块后，你将掌握：
- ✅ 线程的创建与启动方式
- ✅ 线程的生命周期与状态转换
- ✅ 线程的中断机制与优雅关闭
- ✅ 线程间的协作（join、yield、sleep）
- ✅ 守护线程与用户线程的区别

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解线程基础理论

### 💻 演示代码（Demo）
1. **D01_ThreadCreation** - 线程创建的4种方式
2. **D02_ThreadLifecycle** - 线程生命周期与状态
3. **D03_ThreadInterrupt** - 中断机制详解

### ✏️ 练习题（Exercises）
1. **E01_MultiThreadDownloader** 🟢 - 多线程文件下载器
2. **E02_ThreadCoordination** 🟡 - 线程协调与顺序执行

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行线程创建示例
mvn exec:java -Dexec.mainClass="com.concurrency.basics.demo.D01_ThreadCreation"

# 运行线程生命周期示例
mvn exec:java -Dexec.mainClass="com.concurrency.basics.demo.D02_ThreadLifecycle"

# 运行线程中断示例
mvn exec:java -Dexec.mainClass="com.concurrency.basics.demo.D03_ThreadInterrupt"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/basics/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=ThreadBasicsTest
```

---

## 知识点清单

### 核心API
- `Thread` 类
  - `start()` / `run()`
  - `sleep()` / `yield()` / `join()`
  - `interrupt()` / `isInterrupted()`
  - `setDaemon()` / `isDaemon()`
  - `getName()` / `setName()`

- `Runnable` 接口
  - `run()` 方法

- `Callable<V>` 接口（预习）
  - `call()` 方法，可返回结果

### 线程状态（Thread.State）
1. **NEW** - 新建
2. **RUNNABLE** - 就绪/运行
3. **BLOCKED** - 阻塞
4. **WAITING** - 等待
5. **TIMED_WAITING** - 限时等待
6. **TERMINATED** - 终止

---

## 常见问题

**Q: start() 和 run() 的区别？**
A: `start()` 启动新线程执行`run()`方法，`run()` 只是普通方法调用，不会创建新线程。

**Q: 如何优雅地停止线程？**
A: 使用中断机制（`interrupt()`）+ 检查标志位，避免使用已废弃的`stop()`方法。

**Q: 守护线程有什么用？**
A: 守护线程（Daemon Thread）用于后台服务，当所有用户线程结束时，守护线程会自动终止（如GC线程）。

**Q: sleep() 和 wait() 的区别？**
A:
- `sleep()`: 不释放锁，到时间自动唤醒
- `wait()`: 释放锁，需要`notify()`唤醒（下个模块学习）

---

## 学习建议

1. **按顺序学习**：Demo1 → Demo2 → Demo3 → Exercise1 → Exercise2
2. **动手实践**：每个Demo都要自己运行一遍，观察输出
3. **理解状态转换**：画出线程状态转换图
4. **独立完成练习**：先不看答案，尝试自己实现
5. **对比方案**：对比你的实现和参考答案的差异

---

## 扩展阅读

- [Oracle官方线程教程](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- 《Java并发编程实战》第1-3章
- JDK源码：`java.lang.Thread`

---

## 下一步

完成本模块后，继续学习：
👉 **[Module 02: 同步机制](../module-02-synchronization/)**

学习`synchronized`关键字和`wait/notify`机制
