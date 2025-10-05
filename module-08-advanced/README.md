# Module 08: 高级主题

## 学习目标

完成本模块后，你将掌握：
- ✅ ForkJoinPool框架与工作窃取算法
- ✅ RecursiveTask与RecursiveAction的使用
- ✅ StampedLock的乐观读锁机制
- ✅ Phaser的多阶段同步控制
- ✅ Exchanger的线程间数据交换
- ✅ 高级并发工具的选择与应用场景

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解高级并发工具理论

### 💻 演示代码（Demo）
1. **D01_ForkJoin** - ForkJoin框架并行计算
2. **D02_StampedLock** - StampedLock乐观锁
3. **D03_Phaser** - Phaser多阶段同步

### ✏️ 练习题（Exercises）
1. **E01_ParallelMergeSort** 🟡 - 并行归并排序
2. **E02_ConcurrentSkipList** 🔴 - 并发跳表实现
3. **E03_DataAggregator** 🟡 - 并行数据聚合器 ⭐

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行ForkJoin示例
mvn exec:java -Dexec.mainClass="com.concurrency.advanced.demo.D01_ForkJoin"

# 运行StampedLock示例
mvn exec:java -Dexec.mainClass="com.concurrency.advanced.demo.D02_StampedLock"

# 运行Phaser示例
mvn exec:java -Dexec.mainClass="com.concurrency.advanced.demo.D03_Phaser"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/advanced/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=AdvancedTest
```

---

## 知识点清单

### 核心API

#### ForkJoinPool
- `ForkJoinPool` - 专门用于分治任务的线程池
- `RecursiveTask<V>` - 有返回值的递归任务
- `RecursiveAction` - 无返回值的递归任务
- `fork()` - 异步执行子任务
- `join()` - 等待子任务完成并获取结果
- `invoke()` - 同步执行任务

#### StampedLock
- `writeLock()` / `unlockWrite(stamp)` - 写锁
- `readLock()` / `unlockRead(stamp)` - 悲观读锁
- `tryOptimisticRead()` - 乐观读（无锁）
- `validate(stamp)` - 验证乐观读是否有效
- `tryConvertToWriteLock(stamp)` - 锁升级

#### Phaser
- `register()` / `arriveAndAwaitAdvance()` - 注册并等待
- `arrive()` / `arriveAndDeregister()` - 到达但不等待
- `getPhase()` - 获取当前阶段
- `onAdvance()` - 阶段完成时的回调

#### Exchanger
- `exchange(V x)` - 交换数据（阻塞）
- `exchange(V x, timeout)` - 限时交换

---

## 常见问题

**Q: ForkJoinPool和普通线程池的区别？**
A:
- ForkJoinPool使用**工作窃取算法**：空闲线程会从其他线程的队列尾部窃取任务
- 普通线程池使用全局队列，所有线程竞争同一个队列
- ForkJoinPool适合递归分治任务，普通线程池适合独立任务

**Q: 什么时候使用StampedLock的乐观读？**
A:
- 读操作远多于写操作的场景
- 读操作耗时短，验证失败的代价小
- 需要更高的读性能（避免读锁的开销）

**Q: Phaser和CyclicBarrier的区别？**
A:
- Phaser支持动态增加/减少参与者
- Phaser支持多阶段（CyclicBarrier只有一个阶段）
- Phaser更灵活，但使用更复杂

**Q: 何时使用Exchanger？**
A:
- 两个线程需要交换数据的场景
- 生产者-消费者的对称场景（一对一交换）
- 遗传算法中的配对交叉

---

## 学习建议

1. **ForkJoin框架**：
   - 理解分治思想（Divide and Conquer）
   - 掌握任务粒度控制（避免过度拆分）
   - 注意避免阻塞操作（会影响工作窃取效率）

2. **StampedLock**：
   - 先掌握悲观读写锁，再学习乐观读
   - 注意乐观读的验证模式
   - 对比ReentrantReadWriteLock的性能差异

3. **Phaser**：
   - 从CyclicBarrier过渡到Phaser
   - 理解阶段的概念和动态调整
   - 注意onAdvance方法的使用

4. **性能对比**：
   - 编写基准测试对比不同工具的性能
   - 理解不同场景下的最佳选择

---

## 扩展阅读

- [ForkJoin框架官方文档](https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html)
- [Doug Lea - A Java Fork/Join Framework](http://gee.cs.oswego.edu/dl/papers/fj.pdf)
- 《Java并发编程实战》第11、13章
- JDK源码：`java.util.concurrent.ForkJoinPool`

---

## 下一步

完成本模块后，你已掌握Java并发编程的核心知识！

建议：
- 🔄 回顾之前的模块，巩固基础
- 📚 阅读经典并发编程书籍
- 💼 在实际项目中应用所学知识
- 🎯 深入研究JDK并发源码

---

## 性能优化建议

### ForkJoinPool最佳实践
```java
// ✅ 好的做法
class GoodTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 1000; // 合理的阈值

    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // 直接计算，避免过度拆分
            return sequentialCompute();
        }
        // 拆分任务
        ForkJoinTask<Long> left = new GoodTask(...).fork();
        ForkJoinTask<Long> right = new GoodTask(...);
        return right.compute() + left.join();
    }
}

// ❌ 避免的做法
class BadTask extends RecursiveTask<Long> {
    protected Long compute() {
        if (end - start <= 1) { // 阈值太小，过度拆分
            return (long) array[start];
        }
        // ... 导致大量小任务，调度开销大
    }
}
```

### StampedLock使用模式
```java
// 乐观读模式
long stamp = lock.tryOptimisticRead();
// 读取数据
double x = this.x, y = this.y;
if (!lock.validate(stamp)) { // 验证失败
    stamp = lock.readLock(); // 升级为悲观读
    try {
        x = this.x;
        y = this.y;
    } finally {
        lock.unlockRead(stamp);
    }
}
```
