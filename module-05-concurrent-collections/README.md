# Module 05: 并发集合

## 学习目标

完成本模块后，你将掌握：
- ✅ ConcurrentHashMap的原理与使用（分段锁、CAS）
- ✅ BlockingQueue家族的特性与应用
- ✅ CopyOnWriteArrayList的读写分离机制
- ✅ ConcurrentSkipListMap/Set的跳表结构
- ✅ 如何根据场景选择合适的并发集合
- ✅ 并发集合的性能特征

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解并发集合理论

### 💻 演示代码（Demo）
1. **D01_ConcurrentHashMap** - 线程安全的Map操作
2. **D02_BlockingQueue** - 生产者-消费者模式
3. **D03_CopyOnWriteArrayList** - 读多写少场景

### ✏️ 练习题（Exercises）
1. **E01_ThreadSafeCache** 🟢 - LRU缓存实现
2. **E02_WorkQueue** 🟡 - 任务队列系统
3. **E03_EventBus** 🟡 - 简易事件总线

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行ConcurrentHashMap示例
mvn exec:java -Dexec.mainClass="com.concurrency.collections.demo.D01_ConcurrentHashMap"

# 运行BlockingQueue示例
mvn exec:java -Dexec.mainClass="com.concurrency.collections.demo.D02_BlockingQueue"

# 运行CopyOnWriteArrayList示例
mvn exec:java -Dexec.mainClass="com.concurrency.collections.demo.D03_CopyOnWriteArrayList"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/collections/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=ConcurrentCollectionsTest
```

---

## 知识点清单

### 并发Map
- **ConcurrentHashMap**
  - `put(K, V)` - 线程安全的插入
  - `get(K)` - 无锁读取
  - `putIfAbsent(K, V)` - 原子操作
  - `compute(K, BiFunction)` - 原子计算
  - `merge(K, V, BiFunction)` - 原子合并

- **ConcurrentSkipListMap**
  - 基于跳表的有序Map
  - `O(log n)` 时间复杂度
  - 支持范围查询

### 阻塞队列（BlockingQueue）
- **ArrayBlockingQueue**
  - 有界队列（数组实现）
  - 单锁实现

- **LinkedBlockingQueue**
  - 可选有界队列（链表实现）
  - 双锁实现（put/take分离）

- **PriorityBlockingQueue**
  - 优先级队列
  - 无界，自动扩容

- **DelayQueue**
  - 延迟队列
  - 元素需实现Delayed接口

- **SynchronousQueue**
  - 零容量队列
  - 直接交换

### 写时复制集合
- **CopyOnWriteArrayList**
  - 读不加锁
  - 写时复制整个数组
  - 适合读多写少

- **CopyOnWriteArraySet**
  - 基于CopyOnWriteArrayList
  - 保证元素唯一性

### 并发Set
- **ConcurrentHashMap.KeySetView**
  - 通过`newKeySet()`创建
  - 线程安全的Set

- **ConcurrentSkipListSet**
  - 基于ConcurrentSkipListMap
  - 有序Set

---

## 常见问题

**Q: ConcurrentHashMap和Hashtable的区别？**
A:
- `Hashtable`: 全表锁，性能差
- `ConcurrentHashMap`: 分段锁/CAS，高并发性能好
- `ConcurrentHashMap`: 不允许null键值
- 推荐使用`ConcurrentHashMap`

**Q: 什么时候用CopyOnWriteArrayList？**
A:
- 读操作远多于写操作
- 集合数据量不大
- 可以容忍短暂的数据不一致（最终一致性）
- 典型场景：监听器列表、配置项

**Q: BlockingQueue的put()和offer()区别？**
A:
- `put()`: 队列满时阻塞等待
- `offer()`: 队列满时立即返回false
- `offer(timeout)`: 队列满时等待指定时间

**Q: 如何选择BlockingQueue实现？**
A:
- 需要有界队列：`ArrayBlockingQueue`
- 高吞吐量：`LinkedBlockingQueue`
- 需要优先级：`PriorityBlockingQueue`
- 延迟任务：`DelayQueue`
- 直接交换：`SynchronousQueue`

---

## 性能对比

### ConcurrentHashMap vs 同步Map
```
操作         | ConcurrentHashMap | Collections.synchronizedMap
-----------|------------------|---------------------------
读操作      | 无锁/CAS          | 全表锁
写操作      | 分段锁/CAS        | 全表锁
迭代器      | 弱一致性          | fail-fast
null键值    | 不允许            | HashMap允许
并发性能    | ⭐⭐⭐⭐⭐         | ⭐⭐
```

### BlockingQueue性能特征
```
实现                    | 内存占用 | 吞吐量 | 有界性
-----------------------|---------|--------|-------
ArrayBlockingQueue     | 低      | 中     | 有界
LinkedBlockingQueue    | 高      | 高     | 可选
PriorityBlockingQueue  | 中      | 低     | 无界
```

---

## 学习建议

1. **理解数据结构**：每种集合的底层实现决定了性能特征
2. **场景匹配**：根据读写比例、是否有序、是否有界选择集合
3. **性能测试**：在真实场景中对比不同集合的性能
4. **避免陷阱**：
   - CopyOnWrite系列不适合写频繁场景
   - BlockingQueue避免无界队列导致OOM
   - 迭代时注意弱一致性

---

## 扩展阅读

- [Oracle并发集合指南](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html)
- 《Java并发编程实战》第5章
- [ConcurrentHashMap源码分析](https://www.ibm.com/developerworks/cn/java/java-lo-concurrenthashmap/)
- [Doug Lea的并发编程](http://gee.cs.oswego.edu/dl/cpj/index.html)

---

## 下一步

完成本模块后，继续学习：
👉 **[Module 06: 线程池](../module-06-thread-pool/)**

学习Executor框架和线程池的使用
