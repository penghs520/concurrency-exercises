# Module 06: 线程池

## 学习目标

完成本模块后，你将掌握：
- ✅ ThreadPoolExecutor的工作原理与核心参数
- ✅ 线程池的任务提交与执行流程
- ✅ 拒绝策略的选择与自定义
- ✅ Executors工厂方法与最佳实践
- ✅ ScheduledThreadPoolExecutor定时任务调度
- ✅ 线程池的监控、调优与优雅关闭

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解线程池理论与最佳实践

### 💻 演示代码（Demo）
1. **D01_ThreadPoolBasics** - 线程池创建与参数配置
2. **D02_RejectionPolicies** - 拒绝策略详解
3. **D03_ScheduledExecutor** - 定时任务调度

### ✏️ 练习题（Exercises）
1. **E01_DynamicThreadPool** 🟢 - 动态可调整线程池
2. **E02_TaskScheduler** 🟡 - 自定义任务调度器
3. **E03_MonitoredThreadPool** 🔴 - 带监控的线程池 ⭐

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行线程池基础示例
mvn exec:java -Dexec.mainClass="com.concurrency.pool.demo.D01_ThreadPoolBasics"

# 运行拒绝策略示例
mvn exec:java -Dexec.mainClass="com.concurrency.pool.demo.D02_RejectionPolicies"

# 运行定时任务示例
mvn exec:java -Dexec.mainClass="com.concurrency.pool.demo.D03_ScheduledExecutor"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/pool/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=ThreadPoolTest
```

---

## 知识点清单

### 核心API
- `ThreadPoolExecutor` 类
  - 构造参数：corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory、handler
  - `execute()` / `submit()` - 任务提交
  - `shutdown()` / `shutdownNow()` - 优雅关闭
  - `getActiveCount()` / `getCompletedTaskCount()` - 监控方法

- `Executors` 工厂类
  - `newFixedThreadPool()` - 固定大小线程池
  - `newCachedThreadPool()` - 缓存线程池
  - `newSingleThreadExecutor()` - 单线程池
  - `newScheduledThreadPool()` - 定时任务线程池

- `RejectedExecutionHandler` 拒绝策略
  - `AbortPolicy` - 抛异常（默认）
  - `CallerRunsPolicy` - 调用者执行
  - `DiscardPolicy` - 丢弃任务
  - `DiscardOldestPolicy` - 丢弃最老任务

- `ScheduledThreadPoolExecutor` 定时任务
  - `schedule()` - 延迟执行
  - `scheduleAtFixedRate()` - 固定频率
  - `scheduleWithFixedDelay()` - 固定延迟

### 重要概念
- **核心线程（Core Threads）**：线程池保持存活的最小线程数
- **最大线程（Maximum Threads）**：线程池允许的最大线程数
- **工作队列（Work Queue）**：存储待执行任务的阻塞队列
- **线程工厂（Thread Factory）**：创建新线程的工厂
- **拒绝策略（Rejection Policy）**：队列满时的处理策略

---

## 常见问题

**Q: 线程池的执行流程是什么？**
A:
1. 如果运行线程数 < corePoolSize，创建新线程执行任务
2. 如果运行线程数 >= corePoolSize，将任务加入队列
3. 如果队列满且运行线程数 < maximumPoolSize，创建新线程
4. 如果队列满且运行线程数 >= maximumPoolSize，执行拒绝策略

**Q: 如何选择线程池大小？**
A:
- **CPU密集型**：线程数 = CPU核心数 + 1
- **IO密集型**：线程数 = CPU核心数 * (1 + IO时间/CPU时间)
- **混合型**：需要根据实际情况测试调优

**Q: 为什么不推荐使用Executors工厂方法？**
A:
- `newFixedThreadPool`和`newSingleThreadExecutor`使用无界队列，可能导致OOM
- `newCachedThreadPool`允许创建Integer.MAX_VALUE个线程，可能耗尽资源
- 推荐：手动创建ThreadPoolExecutor，明确指定参数

**Q: submit()和execute()的区别？**
A:
- `execute()`：执行Runnable任务，无返回值
- `submit()`：执行Callable/Runnable任务，返回Future对象，可获取结果和异常

**Q: 如何优雅关闭线程池？**
A:
```java
// 1. 停止接收新任务
executor.shutdown();

// 2. 等待任务完成
if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
    // 3. 超时后强制关闭
    executor.shutdownNow();
    // 4. 再次等待
    executor.awaitTermination(60, TimeUnit.SECONDS);
}
```

---

## 学习建议

1. **理解执行流程**：画出ThreadPoolExecutor的任务提交流程图
2. **参数调优实验**：修改核心参数，观察线程池行为变化
3. **拒绝策略选择**：理解各种拒绝策略的适用场景
4. **监控实践**：实现自定义ThreadFactory和监控日志
5. **性能对比**：对比同步执行、手动创建线程、线程池执行的性能差异

---

## 扩展阅读

- [Oracle并发教程 - Thread Pools](https://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html)
- 《Java并发编程实战》第6-8章
- JDK源码：`java.util.concurrent.ThreadPoolExecutor`
- 阿里巴巴Java开发手册 - 并发处理规范

---

## 下一步

完成本模块后，继续学习：
👉 **[Module 07: 并发工具类](../module-07-concurrent-utils/)**

学习`CountDownLatch`、`CyclicBarrier`、`Semaphore`等高级并发工具
