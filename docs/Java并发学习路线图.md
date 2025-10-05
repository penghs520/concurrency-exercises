# Java并发学习路线图

## 学习路径总览

```
第一阶段：基础入门（2周）
    ↓
第二阶段：工具掌握（2周）
    ↓
第三阶段：原理深入（1-2周）
    ↓
第四阶段：实战演练（2-3周）
    ↓
第五阶段：性能优化（持续）
```

---

## 第一阶段：基础入门（Week 1-2）

### 目标
掌握Java并发的基本概念和核心机制

### 学习内容

#### Week 1: 线程基础
- [ ] **线程的创建与启动**
  - 继承Thread vs 实现Runnable
  - Lambda表达式创建线程
  - Thread的常用方法

- [ ] **线程生命周期**
  - NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED
  - 状态转换图

- [ ] **线程协作**
  - join()方法
  - interrupt()中断机制
  - 优雅关闭线程

**实践模块**: `module-01-thread-basics`

#### Week 2: 同步机制
- [ ] **synchronized关键字**
  - 对象锁 vs 类锁
  - 同步方法 vs 同步代码块
  - 重入性

- [ ] **wait/notify机制**
  - 生产者-消费者模型
  - 等待/通知的正确使用

- [ ] **可见性与volatile**
  - happens-before原则
  - volatile的使用场景

**实践模块**: `module-02-synchronization`

### 阶段产出
- 能独立编写基本的多线程程序
- 理解线程安全的基本概念
- 掌握synchronized的使用

---

## 第二阶段：工具掌握（Week 3-4）

### 目标
熟练使用Java并发工具类

#### Week 3: Lock与原子类
- [ ] **Lock接口**
  - ReentrantLock使用
  - 公平锁 vs 非公平锁
  - Condition条件变量
  - tryLock超时机制

- [ ] **ReadWriteLock**
  - 读写分离
  - 锁降级

- [ ] **原子类**
  - AtomicInteger/Long/Boolean
  - AtomicReference
  - CAS原理
  - LongAdder性能优化

**实践模块**: `module-03-locks`, `module-04-atomic`

#### Week 4: 并发集合与线程池
- [ ] **并发集合**
  - ConcurrentHashMap原理
  - CopyOnWriteArrayList
  - BlockingQueue家族
  - ConcurrentLinkedQueue

- [ ] **线程池**
  - ThreadPoolExecutor核心参数
  - 四种预定义线程池
  - 拒绝策略
  - 线程池监控

**实践模块**: `module-05-concurrent-collections`, `module-06-thread-pool`

### 阶段产出
- 能根据场景选择合适的并发工具
- 掌握线程池的配置与使用
- 了解常见并发集合的特点

---

## 第三阶段：原理深入（Week 5-6）

### 目标
深入理解Java并发底层原理

### 学习内容
- [ ] **Java内存模型（JMM）**
  - 主内存与工作内存
  - happens-before规则
  - 内存屏障

- [ ] **锁的实现原理**
  - synchronized的JVM实现（monitorenter/monitorexit）
  - AQS (AbstractQueuedSynchronizer)
  - CAS与自旋
  - 锁优化：偏向锁、轻量级锁、重量级锁

- [ ] **并发集合原理**
  - ConcurrentHashMap的分段锁（JDK7）→ CAS+synchronized（JDK8）
  - COW (Copy-On-Write) 机制

- [ ] **高级并发工具**
  - CompletableFuture异步编程
  - ForkJoinPool工作窃取
  - StampedLock乐观读

**实践模块**: `module-07-completable-future`, `module-08-advanced`

### 阶段产出
- 理解并发工具的实现原理
- 能分析并发问题的根源
- 掌握JMM和happens-before

---

## 第四阶段：实战演练（Week 7-9）

### 目标
在真实场景中应用并发技术

### 项目实战

#### Week 7: 高并发秒杀系统
**项目**: `project-01-flash-sale`

- [ ] 实现库存扣减的并发控制
- [ ] 对比synchronized、Lock、分布式锁
- [ ] 性能测试与优化
- [ ] 处理超卖问题

**核心技能**:
- 悲观锁 vs 乐观锁
- 数据库行锁
- 缓存一致性

#### Week 8: 即时通讯服务器
**项目**: `project-02-im-server`

- [ ] 线程安全的会话管理
- [ ] 消息广播机制
- [ ] NIO + 线程池
- [ ] 背压处理

**核心技能**:
- ConcurrentHashMap应用
- 线程池调优
- 非阻塞I/O

#### Week 9: 分布式缓存
**项目**: `project-03-distributed-cache`

- [ ] LRU缓存实现
- [ ] 读写锁优化
- [ ] 缓存穿透/雪崩防护
- [ ] 热点数据处理

**核心技能**:
- ReadWriteLock应用
- 双重检查锁定
- 过期策略

### 阶段产出
- 完成3个真实场景项目
- 积累并发问题排查经验
- 形成系统设计思维

---

## 第五阶段：性能优化（持续提升）

### 进阶项目
- **任务调度系统** (`project-04-job-scheduler`)
  - 时间轮算法
  - 延迟队列
  - 任务窃取

- **数据处理管道** (`project-05-data-pipeline`)
  - Producer-Consumer模式
  - ForkJoin并行计算
  - Reactive Streams

### 性能调优
- [ ] **JMH基准测试**
  - 编写性能测试
  - 对比不同实现方案

- [ ] **并发问题诊断**
  - 死锁检测与分析
  - 线程Dump分析
  - CPU使用率异常排查

- [ ] **最佳实践**
  - 无状态设计
  - 线程池参数调优
  - 减少锁竞争

---

## 学习建议

### 时间分配
- **理论学习**: 30%
- **代码实践**: 50%
- **问题总结**: 20%

### 学习方法
1. **主动实践**：每个知识点都要自己敲代码
2. **对比实验**：尝试多种实现方案，性能测试对比
3. **问题驱动**：带着问题学习，例如"为什么会死锁？"
4. **源码阅读**：看JDK并发包的实现（如ReentrantLock）
5. **总结输出**：写技术博客或笔记

### 常见误区
❌ 只看理论不动手
❌ 死记API不理解原理
❌ 忽视线程安全问题
❌ 滥用synchronized导致性能问题
❌ 不做性能测试就下结论

### 推荐资源
📚 **书籍**
- 《Java并发编程实战》（必读）
- 《Java并发编程的艺术》
- 《深入理解Java虚拟机》（JMM章节）

🌐 **在线资源**
- Doug Lea的论文（AQS作者）
- OpenJDK源码
- Java并发官方教程

---

## 自我检测清单

### 基础级（通过则掌握第一阶段）
- [ ] 能解释什么是线程安全
- [ ] 能正确使用synchronized
- [ ] 理解wait/notify的工作原理
- [ ] 知道volatile的作用

### 中级（通过则掌握第二阶段）
- [ ] 能选择合适的Lock实现
- [ ] 能配置ThreadPoolExecutor
- [ ] 理解ConcurrentHashMap的优势
- [ ] 能实现生产者-消费者模式

### 高级（通过则掌握第三阶段）
- [ ] 能解释happens-before规则
- [ ] 理解AQS的实现原理
- [ ] 能分析死锁并解决
- [ ] 掌握CompletableFuture异步编程

### 专家级（通过则掌握第四、五阶段）
- [ ] 能设计高并发系统架构
- [ ] 能进行并发性能调优
- [ ] 能解决复杂的并发Bug
- [ ] 理解无锁数据结构

---

## 面试准备重点

### 高频考点
1. synchronized vs Lock
2. volatile原理
3. 线程池参数含义
4. ConcurrentHashMap实现
5. 死锁的四个必要条件
6. happens-before规则
7. AQS原理

### 手写代码题
- 单例模式（双重检查锁定）
- 生产者-消费者
- 读写锁实现
- 线程安全的计数器

---

**开始你的并发学习之旅，一步一个脚印！** 🚀
