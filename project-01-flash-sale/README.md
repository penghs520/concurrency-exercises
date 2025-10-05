# Project 01: 秒杀系统 (Flash Sale System)

**项目难度**: ⭐⭐⭐ (3/5)
**并发挑战**: 超卖防护、库存一致性、高并发性能优化
**技术栈**: synchronized → ReentrantLock → 细粒度锁 → 原子操作
**性能要求**: 支持 10000+ QPS

## 项目背景

秒杀系统是电商领域最具挑战性的高并发场景之一。在短时间内，大量用户同时抢购有限的商品，系统需要：
- **防止超卖**：确保库存不会被扣成负数
- **保证一致性**：库存扣减和订单生成必须原子性完成
- **高性能**：在高并发下保持低延迟和高吞吐
- **公平性**：先到先得，避免用户体验问题

## 学习目标

通过本项目，你将掌握：

1. **并发问题诊断**
   - 理解竞态条件 (Race Condition) 如何导致超卖
   - 使用多线程测试暴露并发 bug
   - 分析 check-then-act 模式的危险性

2. **同步机制对比**
   - synchronized 关键字的使用和局限
   - ReentrantLock 的高级特性（tryLock、公平锁）
   - 锁粒度对性能的影响

3. **性能优化技巧**
   - 细粒度锁：锁分段减少竞争
   - 无锁数据结构：ConcurrentHashMap、AtomicInteger
   - 预热缓存：避免冷启动问题
   - 减少锁持有时间

4. **基准测试**
   - 使用 JMH 进行微基准测试
   - 对比不同方案的吞吐量和延迟
   - 理解并发度对性能的影响

## 核心并发问题演示

### 问题 1: 竞态条件导致超卖

```java
// 错误示例：非原子操作
if (stock > 0) {           // 线程 A、B 同时检查，都通过
    stock--;               // A、B 都执行扣减，库存变负！
    createOrder();
}
```

**场景重现**：1000 个线程抢购 100 件商品，结果生成了 150 个订单

### 问题 2: 锁粒度过粗导致性能差

```java
// 全局锁：所有商品共用一把锁
synchronized (this) {      // 商品 A 和商品 B 的秒杀互相阻塞
    // 处理秒杀逻辑
}
```

**性能影响**：TPS 从 50000 下降到 5000

### 问题 3: 数据库并发更新失败

```java
// 乐观锁失败率高
UPDATE product SET stock = stock - 1
WHERE id = ? AND stock > 0  // 高并发下大量更新失败
```

## 项目结构

```
project-01-flash-sale/
├── README.md                          # 项目说明文档
├── starter-code/                      # 起始代码（学员填空）
│   ├── pom.xml                        # Maven 配置
│   └── src/main/java/com/concurrency/project/flashsale/
│       ├── Product.java               # 商品实体
│       ├── Order.java                 # 订单实体
│       ├── FlashSaleService.java      # 秒杀服务接口（待实现）
│       └── Main.java                  # 测试入口
├── solution/                          # 三个渐进式解决方案
│   ├── v1-synchronized/               # 版本1：基础 synchronized
│   ├── v2-reentrantlock/              # 版本2：ReentrantLock 优化
│   └── v3-optimized/                  # 版本3：生产级优化
└── benchmark/                         # 性能基准测试
    └── src/main/java/com/concurrency/project/flashsale/
        └── PerformanceBenchmark.java  # JMH 基准测试
```

## 渐进式实现方案

### Version 1: synchronized 基础版本

**实现方式**：
```java
public synchronized String buy(long userId, long productId) {
    // 检查库存
    if (stock <= 0) return null;

    // 扣减库存
    stock--;

    // 生成订单
    return createOrder(userId, productId);
}
```

**优点**：
- 实现简单，代码清晰
- 绝对防止超卖
- JVM 自动管理锁

**缺点**：
- 全局锁，所有商品共享
- 吞吐量低（约 5000 TPS）
- 无法设置超时

**适用场景**：商品种类少、并发度低的场景

---

### Version 2: ReentrantLock 优化版本

**实现方式**：
```java
private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

public String buy(long userId, long productId) {
    ReentrantLock lock = locks.computeIfAbsent(productId,
        k -> new ReentrantLock());

    if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
        return null;  // 超时快速失败
    }

    try {
        // 秒杀逻辑
    } finally {
        lock.unlock();
    }
}
```

**优点**：
- 细粒度锁（每个商品独立锁）
- 支持超时机制（tryLock）
- 可中断、可设置公平性
- 吞吐量提升 3-5 倍

**缺点**：
- 需要手动管理锁
- 仍有锁竞争开销
- 单 JVM 限制

**适用场景**：中等并发、多商品秒杀

---

### Version 3: 生产级优化版本

**实现方式**：
```java
// 无锁数据结构
private final ConcurrentHashMap<Long, AtomicInteger> stockMap;

// 分段锁
private final Map<Long, ReentrantLock> locks;

public String buy(long userId, long productId) {
    AtomicInteger stock = stockMap.get(productId);

    // 快速失败：无锁检查
    if (stock.get() <= 0) return null;

    // 细粒度锁保护
    ReentrantLock lock = locks.get(productId);
    if (!lock.tryLock(50, TimeUnit.MILLISECONDS)) {
        return null;
    }

    try {
        // CAS 扣减库存
        int current = stock.get();
        if (current > 0 && stock.compareAndSet(current, current - 1)) {
            return createOrder(userId, productId);
        }
        return null;
    } finally {
        lock.unlock();
    }
}
```

**优化点**：
1. **双重检查** (Double-Check)：无锁快速失败
2. **原子操作**：AtomicInteger 的 CAS
3. **缓存预热**：提前加载热点数据
4. **锁分段**：降低锁冲突概率

**性能指标**：
- 吞吐量：10000+ TPS
- P99 延迟：< 5ms
- 零超卖

**适用场景**：生产环境高并发秒杀

## 快速开始

### 1. 从 starter-code 开始

```bash
cd starter-code
mvn clean compile
mvn exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
```

**任务**：实现 `FlashSaleService` 接口，防止超卖

**测试场景**：
- 1000 个线程同时抢购
- 商品库存 100 件
- 预期结果：恰好生成 100 个订单

### 2. 对比三个解决方案

```bash
# 运行 v1
cd solution/v1-synchronized
mvn clean test

# 运行 v2
cd ../v2-reentrantlock
mvn clean test

# 运行 v3
cd ../v3-optimized
mvn clean test
```

### 3. 运行性能基准测试

```bash
cd benchmark
mvn clean package
java -jar target/benchmarks.jar
```

**基准测试对比**：
```
Benchmark                                  Mode  Cnt   Score   Error  Units
v1_synchronized.buy                       thrpt   25   5234   ±  123  ops/s
v2_reentrantlock.buy                      thrpt   25  18456   ±  456  ops/s
v3_optimized.buy                          thrpt   25  52347   ± 1234  ops/s
```

## 常见陷阱与解决方案

### 陷阱 1: Check-Then-Act 竞态条件

```java
// ❌ 错误：检查和操作不是原子的
if (stock > 0) {       // T1 和 T2 同时通过检查
    stock--;           // 都执行扣减
}

// ✅ 正确：原子操作
synchronized (lock) {
    if (stock > 0) {
        stock--;
    }
}
```

### 陷阱 2: 锁粒度过粗

```java
// ❌ 错误：全局锁
public synchronized String buy(...) { }

// ✅ 正确：每个商品独立锁
private final Map<Long, Lock> locks = ...;
Lock lock = locks.get(productId);
lock.lock();
try { ... } finally { lock.unlock(); }
```

### 陷阱 3: 忘记释放锁

```java
// ❌ 错误：异常导致锁未释放
lock.lock();
if (stock > 0) {
    throw new RuntimeException();  // 锁泄漏！
}
lock.unlock();

// ✅ 正确：finally 保证释放
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();
}
```

### 陷阱 4: 死锁风险

```java
// ❌ 错误：可能死锁
lock1.lock();
lock2.lock();  // 如果另一个线程先锁 lock2，再锁 lock1

// ✅ 正确：tryLock 超时
if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
            try { ... } finally { lock2.unlock(); }
        }
    } finally { lock1.unlock(); }
}
```

## 性能测试指南

### 测试场景设计

| 场景 | 并发数 | 库存 | 目标 TPS | 预期结果 |
|------|--------|------|----------|----------|
| 低并发 | 10 | 1000 | 1000+ | 所有版本通过 |
| 中并发 | 100 | 1000 | 5000+ | v2/v3 通过 |
| 高并发 | 1000 | 1000 | 10000+ | v3 通过 |
| 极限压测 | 10000 | 100 | 50000+ | v3 优化 |

### 关键指标

1. **吞吐量 (Throughput)**: 每秒处理的秒杀请求数
2. **延迟 (Latency)**: P50、P95、P99 延迟
3. **正确性**: 订单数 = 库存数（零超卖）
4. **CPU 使用率**: 锁竞争导致的 CPU 浪费

### 压测命令

```bash
# 使用 JMeter
jmeter -n -t flashsale.jmx -l result.jtl

# 使用 wrk
wrk -t10 -c1000 -d30s http://localhost:8080/flashsale/buy

# 使用 JMH（推荐）
java -jar benchmarks.jar -f 1 -wi 5 -i 10 -t 100
```

## 扩展思考

完成基础实现后，思考以下问题：

1. **分布式环境**：如何在多台服务器间同步库存？
   - Redis 分布式锁（Redisson）
   - 数据库乐观锁（版本号）
   - 消息队列削峰

2. **数据库一致性**：如何保证内存库存和数据库一致？
   - 定时同步
   - 事务补偿
   - 最终一致性

3. **限流降级**：如何保护系统不被打垮？
   - 令牌桶限流
   - 熔断降级
   - 队列缓冲

4. **防刷策略**：如何防止恶意刷单？
   - 用户频率限制
   - 验证码
   - 风控系统

## 学习资源

- **书籍**：《Java 并发编程实战》第 2、3、13 章
- **文档**：[ReentrantLock JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html)
- **文章**：[高并发秒杀系统设计](https://tech.meituan.com/2017/03/16/flash-sale.html)

## 作业提交

完成以下任务：

1. 实现 starter-code 中的 `FlashSaleService`
2. 运行三个版本的测试，截图结果
3. 运行 JMH 基准测试，记录性能数据
4. 回答：为什么 v3 比 v1 快 10 倍？
5. （可选）实现 Redis 分布式锁版本

祝学习愉快！遇到问题请查看各版本的详细注释。
