# Java并发编程系统学习项目

> 还在愁找不到适合自己的并发练习题？来这就对了！从理论到实战，系统掌握Java并发编程核心技术，你也可以是一个并发大师！

> 这个项目是Claude Code生成的，消耗了40$ tokens，质量很不错，我自己也会走一遍所有的练习，遇到错误的地方会及时更正，也欢迎每个人提提PR，点点Star。

## 项目简介
这个项目包含了**8个知识点模块**，覆盖Java并发核心理论和API，每个模块都提供了理论教程、Demo、2~3个练习题和答案，以及**5个实战项目**，提供真实业务场景的并发解决方案，并且每个场景都提供了多套解决方案。

通过理论 +练习 +  实战，效果非常好，希望你可以在一至两周内熟练掌握所有Java并发相关的知识，真正成为一个Java并发高手，而不只是学了就忘。

### 适合人群
- 想系统学习Java并发编程的开发者
- 需要在实际项目中应用并发技术的工程师
- 准备面试的Java开发人员

---

## 快速开始

### 环境要求
- **JDK**: 17+
- **Maven**: 3.6+
- **IDE**: IntelliJ IDEA / VS Code

### 安装与运行
```bash
# 克隆项目
git clone <your-repo-url>

# 编译项目
mvn clean compile

# 运行测试
mvn test

# 运行性能基准测试
mvn exec:java -Dexec.mainClass="org.openjdk.jmh.Main"
```

---

## 学习路线图

### 📚 Part 1: 知识点模块（按顺序学习）

#### 🟢 基础阶段
1. **[module-01-thread-basics](module-01-thread-basics/)** - 线程基础
   - 线程创建与启动、生命周期、中断机制
   - **练习**：多线程下载器、线程协调

2. **[module-02-synchronization](module-02-synchronization/)** - 同步机制
   - synchronized、wait/notify、死锁分析
   - **练习**：有界缓冲区、读写锁实现、哲学家就餐

3. **[module-03-locks](module-03-locks/)** - Lock框架
   - ReentrantLock、ReadWriteLock、Condition
   - **练习**：公平/非公平锁对比、自定义阻塞队列

#### 🟡 中级阶段
4. **[module-04-atomic](module-04-atomic/)** - 原子类
   - AtomicInteger、AtomicReference、CAS原理
   - **练习**：点击计数器、无锁栈

5. **[module-05-concurrent-collections](module-05-concurrent-collections/)** - 并发集合
   - ConcurrentHashMap、BlockingQueue、CopyOnWriteArrayList
   - **练习**：线程安全缓存、工作队列

6. **[module-06-thread-pool](module-06-thread-pool/)** - 线程池
   - ThreadPoolExecutor原理、参数调优、自定义线程池
   - **练习**：动态线程池、任务调度

#### 🔴 高级阶段
7. **[module-07-completable-future](module-07-completable-future/)** - 异步编程
   - CompletableFuture链式调用、异常处理
   - **练习**：并行API调用、异步管道

8. **[module-08-advanced](module-08-advanced/)** - 高级主题
   - ForkJoin、StampedLock、Phaser
   - **练习**：并行归并排序、并发跳表

---

### 🎯 Part 2: 实战项目（综合应用）

#### 项目1: 秒杀系统 ⭐⭐⭐
**[project-01-flash-sale](projects/project-01-flash-sale/)**
- **场景**：高并发秒杀抢购
- **并发挑战**：超卖防护、库存一致性
- **技术栈**：synchronized → ReentrantLock → 分布式锁
- **性能要求**：支持10000+ QPS

#### 项目2: 即时通讯服务器 ⭐⭐⭐⭐
**[project-02-im-server](projects/project-02-im-server/)**
- **场景**：WebSocket聊天服务器
- **并发挑战**：会话管理、消息广播、背压处理
- **技术栈**：NIO、线程池、ConcurrentHashMap
- **性能要求**：10k连接、消息延迟<100ms

#### 项目3: 分布式缓存 ⭐⭐⭐⭐
**[project-03-distributed-cache](projects/project-03-distributed-cache/)**
- **场景**：高性能本地缓存系统
- **并发挑战**：缓存穿透、缓存雪崩、热点数据
- **技术栈**：ConcurrentHashMap、ReadWriteLock、LRU
- **性能要求**：读99.9% < 1ms

#### 项目4: 任务调度系统 ⭐⭐⭐⭐⭐
**[project-04-job-scheduler](projects/project-04-job-scheduler/)**
- **场景**：分布式任务调度框架
- **并发挑战**：延迟任务、周期任务、任务窃取
- **技术栈**：时间轮、DelayQueue、ForkJoinPool
- **性能要求**：支持百万级定时任务

#### 项目5: 数据处理管道 ⭐⭐⭐⭐⭐
**[project-05-data-pipeline](projects/project-05-data-pipeline/)**
- **场景**：大数据ETL流水线
- **并发挑战**：背压控制、批量处理、异常恢复
- **技术栈**：Producer-Consumer、ForkJoin、CompletableFuture
- **性能要求**：吞吐量100MB/s

---

## 项目结构

```
concurrency-exercises/
├── pom.xml                           # Maven配置
├── README.md                         # 项目总览（本文件）
├── docs/                             # 学习文档
│   ├── Java并发学习路线图.md
│   ├── 并发基础理论.md
│   ├── Java内存模型.md
│   └── 常见并发问题诊断.md
│
├── module-01-thread-basics/          # 知识点模块
│   ├── README.md
│   ├── tutorial.md
│   ├── demo/
│   ├── exercises/
│   └── solutions/
├── module-02-synchronization/
├── ... (其他模块)
│
├── projects/                         # 实战项目
│   ├── project-01-flash-sale/
│   │   ├── README.md
│   │   ├── starter-code/
│   │   ├── solution/
│   │   └── benchmark/
│   ├── project-02-im-server/
│   └── ... (其他项目)
│
└── integration/                      # 整合资源
    ├── knowledge-to-project-map.md   # 知识点与项目对照表
    └── troubleshooting/              # 问题诊断
        ├── 常见死锁场景.md
        └── 性能调优checklist.md
```

---

## 核心技术栈

### 并发工具
- ✅ Thread、Runnable、Callable
- ✅ synchronized、volatile、final
- ✅ Lock、ReentrantLock、ReadWriteLock
- ✅ Atomic*、LongAdder
- ✅ ConcurrentHashMap、BlockingQueue
- ✅ ThreadPoolExecutor、ForkJoinPool
- ✅ CompletableFuture、Future
- ✅ Semaphore、CountDownLatch、CyclicBarrier
- ✅ Phaser、Exchanger、StampedLock

### 测试与工具
- JUnit 5：单元测试
- JMH：性能基准测试
- SpotBugs：并发问题静态检测
- AssertJ：流式断言

---


**开始你的并发编程之旅吧！** 🚀
