# 快速开始指南

## 目录结构

```
project-01-flash-sale/
├── README.md                    # 详细项目文档
├── QUICK_START.md              # 本文件 - 快速开始
├── PERFORMANCE_COMPARISON.md   # 性能对比报告
├── starter-code/               # 起始代码（有 bug）
├── solution/                   # 三个渐进式解决方案
│   ├── v1-synchronized/
│   ├── v2-reentrantlock/
│   └── v3-optimized/
└── benchmark/                  # JMH 性能测试
```

## 5 分钟快速体验

### 1. 看到并发 Bug（超卖问题）

```bash
cd starter-code
mvn clean compile exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
```

**预期输出**：
```
✗ 测试失败！
预期订单数: 100, 实际: 142
⚠ 发生超卖！超卖数量: 42
```

**原因**：check-then-act 竞态条件

---

### 2. 运行正确的解决方案

#### V1 - Synchronized（最简单）
```bash
cd solution/v1-synchronized
mvn clean compile exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
```

**预期输出**：
```
✓ 测试通过！无超卖，库存一致
吞吐量: ~757 TPS
```

#### V2 - ReentrantLock（更快）
```bash
cd solution/v2-reentrantlock
mvn clean compile exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
```

**预期输出**：
```
✓ 测试通过！无超卖，库存一致
吞吐量: ~800 TPS
```

#### V3 - Optimized（最快）
```bash
cd solution/v3-optimized
mvn clean compile exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
```

**预期输出**：
```
✓ 测试通过！无超卖，库存一致
吞吐量: ~934 TPS
```

---

### 3. 运行性能基准测试（可选）

```bash
cd benchmark
mvn clean package
java -jar target/benchmarks.jar
```

**快速测试**（减少迭代次数）：
```bash
java -jar target/benchmarks.jar -f 1 -wi 3 -i 5
```

---

## 学习路径

### 阶段 1: 理解问题（15 分钟）

1. **阅读** `starter-code/src/main/java/com/concurrency/project/flashsale/Main.java`
   - 找到 `BuggyFlashSaleService` 的实现
   - 理解为什么会超卖

2. **运行** starter-code，观察超卖现象

3. **思考**：
   - 为什么 `if (stock > 0)` 和 `stock--` 不是原子的？
   - 多线程如何导致竞态条件？

### 阶段 2: 实现解决方案（30 分钟）

1. **任务**：在 `starter-code/` 中实现 `FlashSaleService` 接口

2. **提示**：
   - 从最简单的 `synchronized` 开始
   - 确保 check 和 act 在同一个临界区
   - 运行测试验证正确性

3. **对比**：实现后，对比 `solution/v1-synchronized/` 的代码

### 阶段 3: 学习优化技巧（45 分钟）

1. **V1 → V2**：
   - 阅读 `solution/v2-reentrantlock/FlashSaleServiceV2.java`
   - 关键点：每个商品独立锁、tryLock 超时
   - 思考：为什么比 V1 快？

2. **V2 → V3**：
   - 阅读 `solution/v3-optimized/FlashSaleServiceV3.java`
   - 关键点：AtomicInteger、CAS、双重检查
   - 思考：无锁如何保证安全？

3. **对比运行**：
   ```bash
   # 在三个 solution 目录分别运行，对比耗时
   mvn exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
   ```

### 阶段 4: 深入测试（30 分钟）

1. **修改测试参数**：
   ```java
   // Main.java 中修改
   int threadCount = 10000;  // 提高并发度
   int stock = 1000;         // 增加库存
   ```

2. **观察性能变化**：
   - V1 在高并发下性能下降明显
   - V3 表现稳定

3. **运行 JMH 基准测试**：
   ```bash
   cd benchmark
   mvn clean package
   java -jar target/benchmarks.jar
   ```

---

## 核心代码对比

### Bug 代码（Starter）
```java
// ❌ 错误：不是原子操作
if (product.getStock() > 0) {       // 检查
    product.decreaseStock();        // 操作
    orderCount++;
    return "ORDER-" + ...;
}
```

### V1 - Synchronized
```java
// ✓ 正确：整个方法加锁
public synchronized String buy(...) {
    if (product.getStock() <= 0) return null;
    product.decreaseStock();
    ...
}
```

### V2 - ReentrantLock
```java
// ✓ 更好：细粒度锁 + 超时
ReentrantLock lock = lockMap.get(productId);
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        if (product.getStock() <= 0) return null;
        product.decreaseStock();
        ...
    } finally {
        lock.unlock();
    }
}
```

### V3 - Optimized
```java
// ✓ 最优：CAS + 无锁检查
AtomicInteger stock = stockMap.get(productId);
if (stock.get() <= 0) return null;  // 无锁快速失败

while (true) {
    int current = stock.get();
    if (current <= 0) return null;
    if (stock.compareAndSet(current, current - 1)) {
        break;  // CAS 成功
    }
}
// 短暂加锁生成订单
```

---

## 常见问题 FAQ

### Q1: 为什么 Starter Code 会超卖？
**A**: `if` 检查和 `stock--` 操作之间有时间窗口，多线程可能同时通过检查。

### Q2: synchronized 和 ReentrantLock 哪个更好？
**A**:
- **简单场景**：synchronized（代码简单）
- **复杂场景**：ReentrantLock（功能更强）
- **高并发**：考虑无锁方案（AtomicInteger）

### Q3: V3 为什么不需要锁？
**A**: 库存扣减使用 AtomicInteger 的 CAS 操作，底层是 CPU 原语，无需锁。但订单生成仍需锁保护。

### Q4: 如何选择合适的版本？
**A**:
| 场景 | 推荐版本 | 原因 |
|------|---------|------|
| 学习练习 | V1 | 简单易懂 |
| 生产环境（低并发） | V1/V2 | 稳定可靠 |
| 生产环境（高并发） | V3 | 性能最优 |
| 多机部署 | 需要分布式锁 | 单机版本不适用 |

### Q5: 如何进一步优化？
**A**:
1. 分段库存（多个库存分片）
2. Redis 分布式锁
3. 消息队列异步处理
4. 数据库乐观锁

---

## 性能数据速览

| 版本 | 吞吐量 | 耗时 | 正确性 |
|------|--------|------|--------|
| Starter (Bug) | ~1200 TPS | 110ms | ✗ 超卖 42 件 |
| V1 Synchronized | ~757 TPS | 132ms | ✓ 正确 |
| V2 ReentrantLock | ~800 TPS | 125ms | ✓ 正确 |
| V3 Optimized | ~934 TPS | 107ms | ✓ 正确 |

**性能提升**：V3 比 V1 快 23%

---

## 下一步学习

1. **阅读详细文档**：`README.md`
2. **查看性能报告**：`PERFORMANCE_COMPARISON.md`
3. **尝试扩展**：
   - 实现 Redis 版本
   - 添加限流功能
   - 集成 Spring Boot

4. **相关资源**：
   - 《Java 并发编程实战》第 2、3、13 章
   - [JMH 官方文档](https://openjdk.org/projects/code-tools/jmh/)
   - [AtomicInteger 源码分析](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicInteger.html)

---

**祝学习愉快！遇到问题请查看 README.md 中的详细说明。**
