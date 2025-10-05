# 秒杀系统项目索引

> **项目难度**: ⭐⭐⭐ (3/5)
> **核心技能**: 并发编程、锁优化、性能调优
> **预计学时**: 3-8 小时

## 📖 文档导航

### 快速入门
- **[QUICK_START.md](QUICK_START.md)** - 5分钟快速体验 ⚡
  - 最快上手方式
  - 核心代码对比
  - 常见问题 FAQ

### 详细学习
- **[README.md](README.md)** - 完整项目文档 📚
  - 项目背景和目标
  - 学习路径指南
  - 常见陷阱解析
  - 扩展思考

### 性能分析
- **[PERFORMANCE_COMPARISON.md](PERFORMANCE_COMPARISON.md)** - 性能对比报告 📊
  - 三个版本实测数据
  - 性能提升分析
  - JMH 基准测试

### 架构设计
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - 系统架构设计 🏗️
  - 版本演进架构
  - 并发流程图
  - 内存模型分析
  - 分布式扩展

### 项目总结
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - 项目完成总结 ✅
  - 文件清单
  - 测试结果
  - 关键成果

## 🚀 快速开始

### 第一步：观察并发 Bug
```bash
cd starter-code
mvn clean compile exec:java
```
**预期结果**: 发生超卖，超卖数量约 40 件

### 第二步：运行正确解决方案
```bash
# V1 - Synchronized (最简单)
cd solution/v1-synchronized
mvn exec:java

# V2 - ReentrantLock (更快)
cd solution/v2-reentrantlock
mvn exec:java

# V3 - Optimized (最快)
cd solution/v3-optimized
mvn exec:java
```

### 第三步：性能基准测试
```bash
cd benchmark
mvn clean package
java -jar target/benchmarks.jar
```

## 📂 项目结构

```
project-01-flash-sale/
│
├── 📄 文档 (6个)
│   ├── INDEX.md                      # 本文件 - 项目索引
│   ├── README.md                     # 详细项目文档
│   ├── QUICK_START.md                # 快速开始指南
│   ├── PERFORMANCE_COMPARISON.md     # 性能对比报告
│   ├── ARCHITECTURE.md               # 架构设计文档
│   └── PROJECT_SUMMARY.md            # 项目总结
│
├── 📦 起始代码
│   └── starter-code/                 # 包含 bug 的示例代码
│       ├── FlashSaleService.java     # 待实现接口
│       └── Main.java                 # 测试入口（有 bug）
│
├── 🔧 解决方案
│   ├── v1-synchronized/              # 版本1: synchronized
│   ├── v2-reentrantlock/             # 版本2: ReentrantLock
│   └── v3-optimized/                 # 版本3: AtomicInteger + CAS
│
└── 📊 性能测试
    └── benchmark/                     # JMH 基准测试
```

## 🎯 学习目标

通过本项目，你将掌握：

### 并发问题诊断
- ✅ 理解竞态条件 (Race Condition)
- ✅ 识别 check-then-act 模式的危险
- ✅ 使用多线程测试暴露 bug

### 同步机制对比
- ✅ synchronized 的使用和局限
- ✅ ReentrantLock 的高级特性
- ✅ 锁粒度对性能的影响

### 性能优化技巧
- ✅ 细粒度锁 vs 全局锁
- ✅ 无锁数据结构 (AtomicInteger)
- ✅ CAS 原子操作原理
- ✅ 快速失败策略

### 基准测试
- ✅ JMH 微基准测试
- ✅ 吞吐量 vs 延迟分析
- ✅ 并发度影响评估

## 📈 性能对比速览

| 版本 | 实现方式 | 吞吐量 | 耗时 | 提升 | 正确性 |
|------|---------|--------|------|------|--------|
| Starter | 无锁 | ~1200 TPS | 110ms | - | ✗ 超卖 42 |
| V1 | synchronized | 757 TPS | 132ms | 基准 | ✓ 正确 |
| V2 | ReentrantLock | 800 TPS | 125ms | +6% | ✓ 正确 |
| V3 | AtomicInteger | 934 TPS | 107ms | +23% | ✓ 正确 |

## 💡 核心知识点

### 1. 竞态条件示例
```java
// ❌ 错误：非原子操作
if (stock > 0) {    // Thread-1, Thread-2 同时通过
    stock--;        // 都执行扣减 → 超卖！
}

// ✅ 正确：原子操作
synchronized (lock) {
    if (stock > 0) stock--;
}
```

### 2. 三种同步机制

**synchronized (V1)**
```java
public synchronized String buy(...) {
    // 全局锁，简单可靠，性能较低
}
```

**ReentrantLock (V2)**
```java
ReentrantLock lock = lockMap.get(productId);
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        // 细粒度锁，支持超时
    } finally {
        lock.unlock();
    }
}
```

**AtomicInteger (V3)**
```java
while (true) {
    int current = stock.get();
    if (current <= 0) return null;
    if (stock.compareAndSet(current, current-1)) {
        break;  // CAS 成功，无锁高性能
    }
}
```

### 3. 性能优化路径

```
V1: 全局锁
    1000 线程 → 1 把锁 → 高竞争
    ↓
V2: 细粒度锁
    1000 线程 → N 个商品 → 每个商品 1 把锁 → 中竞争
    ↓
V3: 无锁 + CAS
    90% 无锁快速失败 + 10% CAS 扣减 → 低竞争
```

## 📚 推荐学习顺序

### 阶段 1: 理解问题 (30分钟)
1. 阅读 [QUICK_START.md](QUICK_START.md)
2. 运行 starter-code，观察超卖
3. 理解竞态条件原因

### 阶段 2: 基础实现 (1小时)
1. 学习 V1 - synchronized 实现
2. 自己尝试实现 FlashSaleService
3. 运行测试验证正确性

### 阶段 3: 性能优化 (2小时)
1. 学习 V2 - ReentrantLock 优化
2. 学习 V3 - AtomicInteger 优化
3. 对比三个版本的性能差异

### 阶段 4: 深入研究 (2-4小时)
1. 阅读 [ARCHITECTURE.md](ARCHITECTURE.md)
2. 运行 JMH 基准测试
3. 思考分布式场景扩展

## 🔗 相关资源

### 书籍
- 《Java 并发编程实战》第 2、3、13 章
- 《深入理解 Java 虚拟机》锁优化章节

### 文档
- [ReentrantLock JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html)
- [AtomicInteger JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicInteger.html)
- [JMH 官方文档](https://openjdk.org/projects/code-tools/jmh/)

### 实战案例
- [美团技术博客 - 高并发秒杀系统设计](https://tech.meituan.com/2017/03/16/flash-sale.html)

## 🛠️ 技术栈

- **Java**: 8+
- **并发框架**: java.util.concurrent
- **构建工具**: Maven
- **测试框架**: JUnit
- **性能测试**: JMH (Java Microbenchmark Harness)

## ✨ 扩展方向

完成基础项目后，可以尝试：

### 1. 分布式秒杀
- [ ] Redis 分布式锁 (Redisson)
- [ ] 数据库乐观锁（版本号）
- [ ] 分布式事务 (Seata)

### 2. 高级优化
- [ ] 分段库存（减少竞争）
- [ ] 消息队列削峰（Kafka/RabbitMQ）
- [ ] 多级缓存（本地 + Redis）
- [ ] 限流降级（Sentinel/Hystrix）

### 3. 工程实践
- [ ] Spring Boot 集成
- [ ] 数据库持久化（MySQL/PostgreSQL）
- [ ] 监控告警（Prometheus + Grafana）
- [ ] 压力测试（JMeter/Gatling）

## 🎓 学习建议

### 适合人群
- Java 开发者（了解基础语法）
- 学习并发编程的同学
- 准备面试高并发场景
- 实现生产级秒杀系统

### 前置知识
- Java 基础（多线程、集合框架）
- 了解 Maven 基本使用
- 理解线程安全概念

### 学习技巧
1. **先运行，后理解** - 看到效果再深入原理
2. **对比学习** - 三个版本对比看差异
3. **动手实践** - 自己实现一遍才能真正掌握
4. **性能测试** - 用数据说话，验证优化效果

## 📞 问题反馈

遇到问题时：
1. 查看 [README.md](README.md) 的常见陷阱章节
2. 检查代码是否正确编译运行
3. 对比解决方案中的实现
4. 阅读详细的代码注释

## 🏆 学习成果检验

完成项目后，你应该能够：
- [ ] 解释竞态条件的成因
- [ ] 说出 synchronized、ReentrantLock、AtomicInteger 的区别
- [ ] 分析不同锁粒度对性能的影响
- [ ] 使用 JMH 进行性能测试
- [ ] 设计一个防超卖的秒杀系统
- [ ] 思考分布式场景的解决方案

---

**开始学习**: 建议从 [QUICK_START.md](QUICK_START.md) 开始
**深入研究**: 阅读 [README.md](README.md) 和 [ARCHITECTURE.md](ARCHITECTURE.md)
**性能分析**: 查看 [PERFORMANCE_COMPARISON.md](PERFORMANCE_COMPARISON.md)

祝学习愉快！🚀
