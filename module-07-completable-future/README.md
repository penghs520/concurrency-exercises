# Module 07: 异步编程 - CompletableFuture

## 学习目标

完成本模块后，你将掌握：
- ✅ CompletableFuture的创建与基本使用
- ✅ 异步方法（supplyAsync、runAsync）
- ✅ 链式调用（thenApply、thenCompose、thenCombine）
- ✅ 异常处理（exceptionally、handle、whenComplete）
- ✅ 组合多个Future（allOf、anyOf）
- ✅ 自定义线程池的使用
- ✅ 异步编程最佳实践

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解CompletableFuture详细教程

### 💻 演示代码（Demo）
1. **D01_CompletableFutureBasics** - CompletableFuture创建与链式调用
2. **D02_ErrorHandling** - 异常处理模式
3. **D03_CombiningFutures** - 组合多个Future

### ✏️ 练习题（Exercises）
1. **E01_ParallelAPICalls** 🟢 - 模拟并行API调用
2. **E02_AsyncPipeline** 🟡 - 异步数据处理管道
3. **E03_CircuitBreaker** 🔴 - 简单熔断器实现

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行CompletableFuture基础示例
mvn exec:java -Dexec.mainClass="com.concurrency.async.demo.D01_CompletableFutureBasics"

# 运行异常处理示例
mvn exec:java -Dexec.mainClass="com.concurrency.async.demo.D02_ErrorHandling"

# 运行组合Future示例
mvn exec:java -Dexec.mainClass="com.concurrency.async.demo.D03_CombiningFutures"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/async/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=CompletableFutureTest
```

---

## 知识点清单

### 核心API

#### 创建CompletableFuture
- `CompletableFuture.completedFuture(value)` - 已完成的Future
- `CompletableFuture.supplyAsync(supplier)` - 异步执行有返回值
- `CompletableFuture.runAsync(runnable)` - 异步执行无返回值
- `new CompletableFuture<>()` - 手动创建

#### 转换结果（Transformation）
- `thenApply(function)` - 同步转换
- `thenApplyAsync(function)` - 异步转换
- `thenCompose(function)` - 扁平化转换（避免嵌套Future）

#### 消费结果（Consumption）
- `thenAccept(consumer)` - 消费结果，无返回值
- `thenRun(runnable)` - 执行操作，不关心结果

#### 组合多个Future
- `thenCombine(other, biFunction)` - 合并两个Future结果
- `thenAcceptBoth(other, biConsumer)` - 消费两个结果
- `runAfterBoth(other, runnable)` - 都完成后执行
- `applyToEither(other, function)` - 任一完成即处理
- `acceptEither(other, consumer)` - 任一完成即消费
- `runAfterEither(other, runnable)` - 任一完成即执行

#### 异常处理
- `exceptionally(function)` - 处理异常，返回默认值
- `handle(biFunction)` - 同时处理结果和异常
- `whenComplete(biConsumer)` - 完成时回调（不改变结果）

#### 组合大量Future
- `CompletableFuture.allOf(futures...)` - 等待所有完成
- `CompletableFuture.anyOf(futures...)` - 等待任一完成

#### 获取结果
- `get()` - 阻塞获取结果（可能抛出异常）
- `get(timeout, unit)` - 限时阻塞获取
- `join()` - 阻塞获取（抛出非检查异常）
- `getNow(defaultValue)` - 立即获取（未完成返回默认值）

---

## CompletableFuture方法对比

### thenApply vs thenCompose
```java
// thenApply: 转换结果（函数返回值）
CompletableFuture<String> f1 = future.thenApply(x -> x.toString());

// thenCompose: 扁平化（函数返回CompletableFuture）
CompletableFuture<String> f2 = future.thenCompose(x -> fetchDataAsync(x));
```

### thenApply vs thenAccept vs thenRun
```java
// thenApply: 有参数，有返回值
future.thenApply(x -> x * 2);

// thenAccept: 有参数，无返回值
future.thenAccept(x -> System.out.println(x));

// thenRun: 无参数，无返回值
future.thenRun(() -> System.out.println("Done"));
```

### 同步 vs 异步方法
```java
// 同步：在当前线程执行
future.thenApply(x -> transform(x));

// 异步：在ForkJoinPool执行
future.thenApplyAsync(x -> transform(x));

// 异步：在自定义线程池执行
future.thenApplyAsync(x -> transform(x), customExecutor);
```

---

## 常见问题

**Q: CompletableFuture和Future的区别？**
A:
- `Future`: 只能阻塞获取结果，无法链式调用
- `CompletableFuture`: 支持链式调用、组合、异常处理、回调

**Q: 什么时候使用thenApply vs thenCompose？**
A:
- `thenApply`: 转换结果值（如 String -> Integer）
- `thenCompose`: 调用返回Future的方法（避免 `CompletableFuture<CompletableFuture<T>>`）

**Q: 异常会在哪里抛出？**
A: 异常在调用`get()`或`join()`时抛出，或通过`exceptionally`/`handle`处理

**Q: 默认使用什么线程池？**
A: `ForkJoinPool.commonPool()`，建议自定义线程池避免共用

**Q: 如何避免阻塞？**
A: 使用回调方法（`thenApply`、`thenAccept`等）而不是`get()`/`join()`

---

## 最佳实践

### 1. 使用自定义线程池
```java
// ✗ 不推荐：使用默认ForkJoinPool
CompletableFuture.supplyAsync(() -> fetchData());

// ✓ 推荐：使用自定义线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture.supplyAsync(() -> fetchData(), executor);
```

### 2. 异常处理
```java
// ✓ 使用exceptionally提供默认值
future.exceptionally(ex -> {
    logger.error("Error occurred", ex);
    return defaultValue;
});

// ✓ 使用handle同时处理结果和异常
future.handle((result, ex) -> {
    if (ex != null) {
        return handleError(ex);
    }
    return processResult(result);
});
```

### 3. 避免阻塞主线程
```java
// ✗ 不推荐：阻塞等待
String result = future.get();

// ✓ 推荐：使用回调
future.thenAccept(result -> handleResult(result));
```

### 4. 组合多个异步操作
```java
// ✓ 使用thenCompose避免嵌套
CompletableFuture<User> userFuture =
    fetchUserId()
        .thenCompose(id -> fetchUser(id))
        .thenCompose(user -> enrichUserData(user));
```

### 5. 超时处理（Java 9+）
```java
// Java 9+支持超时
future.orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(ex -> handleTimeout(ex));
```

---

## 学习建议

1. **按顺序学习**：Tutorial → Demo1 → Demo2 → Demo3 → Exercise1 → Exercise2 → Exercise3
2. **理解执行模型**：掌握哪些操作在哪个线程执行
3. **画出执行流程**：对于复杂的链式调用，画出执行顺序图
4. **对比传统方式**：与回调地狱、Future.get()对比，体会优势
5. **实际应用**：思考在实际项目中的应用场景（API调用、文件处理等）

---

## 典型应用场景

1. **并行API调用** - 同时调用多个外部服务
2. **异步数据处理** - 流式数据处理管道
3. **缓存预热** - 异步加载缓存数据
4. **异步日志** - 非阻塞日志记录
5. **超时控制** - 带超时的异步操作
6. **熔断降级** - 服务降级与熔断

---

## 扩展阅读

- [CompletableFuture官方文档](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
- 《Java并发编程实战》第6章
- [CompletableFuture最佳实践](https://www.baeldung.com/java-completablefuture)

---

## 下一步

完成本模块后，继续学习：
👉 **Module 08: 并发工具类**

学习CountDownLatch、CyclicBarrier、Semaphore等高级并发工具
