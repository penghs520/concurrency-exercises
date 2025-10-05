# CompletableFuture 异步编程教程

## 一、什么是CompletableFuture？

### 1.1 传统Future的局限性

在Java 5中引入的`Future`接口提供了异步计算的基本功能：

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Hello";
});

// 阻塞等待结果
String result = future.get(); // 只能阻塞获取
```

**Future的问题**：
- 无法手动完成
- 无法链式调用
- 无法组合多个Future
- 无法处理异常
- 只能通过阻塞或轮询获取结果

### 1.2 CompletableFuture的优势

`CompletableFuture`（Java 8引入）是`Future`的增强版：

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenApply(s -> s + " World")
  .thenApply(s -> s.toUpperCase())
  .exceptionally(ex -> "Error: " + ex.getMessage());

// 非阻塞获取结果
future.thenAccept(System.out::println);
```

**CompletableFuture的特点**：
- ✅ 支持链式调用
- ✅ 可以组合多个异步操作
- ✅ 内置异常处理
- ✅ 支持回调
- ✅ 可以手动完成

---

## 二、创建CompletableFuture

### 2.1 静态工厂方法

#### completedFuture - 已完成的Future
```java
// 创建一个已经完成的Future
CompletableFuture<String> future = CompletableFuture.completedFuture("Hello");
System.out.println(future.get()); // 立即返回：Hello
```

**使用场景**：返回缓存结果、测试、提供默认值

#### supplyAsync - 异步执行有返回值的任务
```java
// 使用默认ForkJoinPool
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("执行线程: " + Thread.currentThread().getName());
    return "Result";
});

// 使用自定义线程池（推荐）
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    return "Result from custom pool";
}, executor);
```

**适用**：需要返回结果的异步任务

#### runAsync - 异步执行无返回值的任务
```java
// 无返回值的异步任务
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("执行异步任务");
    // 发送邮件、记录日志等
});
```

**适用**：不需要返回结果的后台任务

### 2.2 手动创建与完成

```java
CompletableFuture<String> future = new CompletableFuture<>();

// 在另一个线程手动完成
new Thread(() -> {
    try {
        Thread.sleep(1000);
        future.complete("Manual result"); // 正常完成
        // future.completeExceptionally(new Exception("Error")); // 异常完成
    } catch (Exception e) {
        future.completeExceptionally(e);
    }
}).start();

// 等待结果
System.out.println(future.get());
```

**使用场景**：集成回调API、自定义异步逻辑

---

## 三、转换结果（Transformation）

### 3.1 thenApply - 转换结果

```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 10)
    .thenApply(x -> x * 2)      // 20
    .thenApply(x -> x + 5)      // 25
    .thenApply(x -> x / 5);     // 5

System.out.println(future.join()); // 5
```

**特点**：
- 接收上一步的结果作为参数
- 返回新的结果
- 同步执行（在完成Future的线程中）

### 3.2 thenApplyAsync - 异步转换

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello")
    .thenApplyAsync(s -> {
        System.out.println("转换线程: " + Thread.currentThread().getName());
        return s.toUpperCase();
    });
```

**区别**：
- `thenApply`: 在完成上一步的线程中执行
- `thenApplyAsync`: 在ForkJoinPool（或自定义线程池）中执行

### 3.3 thenCompose - 扁平化转换

```java
// 假设有两个返回CompletableFuture的方法
CompletableFuture<User> getUserById(int id) { ... }
CompletableFuture<List<Order>> getOrdersByUser(User user) { ... }

// ✗ 使用thenApply会导致嵌套
CompletableFuture<CompletableFuture<List<Order>>> nestedFuture =
    getUserById(123)
        .thenApply(user -> getOrdersByUser(user)); // 嵌套！

// ✓ 使用thenCompose扁平化
CompletableFuture<List<Order>> ordersFuture =
    getUserById(123)
        .thenCompose(user -> getOrdersByUser(user)); // 扁平化
```

**规则**：
- 如果函数返回普通值，用 `thenApply`
- 如果函数返回 `CompletableFuture`，用 `thenCompose`

**类比**：类似Stream的`map` vs `flatMap`

---

## 四、消费结果（Consumption）

### 4.1 thenAccept - 消费结果（无返回值）

```java
CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Hello")
    .thenAccept(s -> {
        System.out.println("接收到: " + s);
        // 保存到数据库、发送消息等
    });
```

**适用**：处理结果但不需要返回值

### 4.2 thenRun - 执行操作（不关心结果）

```java
CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Hello")
    .thenRun(() -> {
        System.out.println("任务完成！");
        // 清理资源、发送通知等
    });
```

**适用**：任务完成后的清理工作

### 4.3 三种方法对比

```java
CompletableFuture<Integer> base = CompletableFuture.supplyAsync(() -> 42);

// thenApply: (T) -> U，有参数，有返回值
CompletableFuture<String> f1 = base.thenApply(x -> "Result: " + x);

// thenAccept: (T) -> void，有参数，无返回值
CompletableFuture<Void> f2 = base.thenAccept(x -> System.out.println(x));

// thenRun: () -> void，无参数，无返回值
CompletableFuture<Void> f3 = base.thenRun(() -> System.out.println("Done"));
```

---

## 五、组合多个Future

### 5.1 thenCombine - 合并两个Future的结果

```java
CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> {
    return a + b; // 10 + 20 = 30
});

System.out.println(combined.join()); // 30
```

**特点**：两个Future都完成后，合并结果

**应用**：合并多个API调用的结果

### 5.2 thenAcceptBoth - 消费两个结果

```java
CompletableFuture<String> name = CompletableFuture.supplyAsync(() -> "Alice");
CompletableFuture<Integer> age = CompletableFuture.supplyAsync(() -> 25);

name.thenAcceptBoth(age, (n, a) -> {
    System.out.println(n + " is " + a + " years old");
});
```

**特点**：消费两个结果，无返回值

### 5.3 runAfterBoth - 两个都完成后执行

```java
CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> System.out.println("Task 1"));
CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> System.out.println("Task 2"));

task1.runAfterBoth(task2, () -> {
    System.out.println("Both tasks completed");
});
```

### 5.4 applyToEither - 任一完成即处理

```java
CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
    sleep(100);
    return "Fast service";
});

CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Slow service";
});

CompletableFuture<String> result = fast.applyToEither(slow, s -> {
    return "Winner: " + s;
});

System.out.println(result.join()); // Winner: Fast service
```

**应用**：主备服务切换、超时控制

### 5.5 allOf - 等待所有完成

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Task 1");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "Task 2");
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "Task 3");

// allOf返回CompletableFuture<Void>
CompletableFuture<Void> allFutures = CompletableFuture.allOf(f1, f2, f3);

// 等待所有完成
allFutures.join();

// 收集所有结果
List<String> results = Stream.of(f1, f2, f3)
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

**应用**：批量API调用、并行任务

### 5.6 anyOf - 等待任一完成

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Task 1";
});

CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    sleep(500);
    return "Task 2"; // 最快
});

CompletableFuture<Object> fastest = CompletableFuture.anyOf(f1, f2);
System.out.println(fastest.join()); // Task 2
```

**应用**：服务降级、超时控制

---

## 六、异常处理

### 6.1 exceptionally - 处理异常并返回默认值

```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("出错了！");
    }
    return 42;
}).exceptionally(ex -> {
    System.err.println("捕获异常: " + ex.getMessage());
    return -1; // 返回默认值
});

System.out.println(future.join()); // 正常: 42, 异常: -1
```

**特点**：
- 只在发生异常时调用
- 返回默认值
- 吞掉异常，返回正常结果

### 6.2 handle - 同时处理结果和异常

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
}).handle((result, ex) -> {
    if (ex != null) {
        return "Error handled: " + ex.getMessage();
    }
    return "Result: " + result;
});
```

**特点**：
- 总是被调用（无论是否异常）
- 可以访问结果和异常
- 可以转换结果或处理异常

### 6.3 whenComplete - 完成时回调（不改变结果）

```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 42)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            System.err.println("失败: " + ex.getMessage());
        } else {
            System.out.println("成功: " + result);
        }
        // 记录日志、清理资源等
    });

// whenComplete不改变结果，仍返回原始值或异常
System.out.println(future.join()); // 42
```

**特点**：
- 类似`finally`块
- 不改变结果（仍返回原始结果或异常）
- 用于日志、资源清理等副作用

### 6.4 三种异常处理方法对比

| 方法 | 调用时机 | 返回值 | 是否改变结果 |
|------|---------|--------|-------------|
| `exceptionally` | 仅异常时 | 提供默认值 | 是（转换异常为值） |
| `handle` | 总是调用 | 转换结果或异常 | 是 |
| `whenComplete` | 总是调用 | 无（void） | 否（保持原结果） |

### 6.5 异常传播

```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Stage 1 error");
})
.thenApply(x -> x * 2) // 不会执行
.thenApply(x -> x + 1) // 不会执行
.exceptionally(ex -> {
    System.err.println("捕获: " + ex.getMessage());
    return -1;
});
```

**特点**：异常会跳过所有中间步骤，直到遇到异常处理器

---

## 七、自定义线程池

### 7.1 为什么要自定义线程池？

默认情况下，`CompletableFuture`使用`ForkJoinPool.commonPool()`：

**问题**：
- 共享线程池，可能被其他任务占用
- 默认线程数 = CPU核心数
- 无法控制队列大小、拒绝策略等

### 7.2 创建自定义线程池

```java
// 创建固定大小的线程池
ExecutorService executor = Executors.newFixedThreadPool(
    10, // 线程数
    new ThreadFactory() {
        private AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("MyAsync-" + counter.incrementAndGet());
            t.setDaemon(true); // 设置为守护线程
            return t;
        }
    }
);

// 使用自定义线程池
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("线程: " + Thread.currentThread().getName());
    return "Result";
}, executor);

// 记得关闭线程池
executor.shutdown();
```

### 7.3 自定义线程池配置

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                      // 核心线程数
    20,                      // 最大线程数
    60L,                     // 空闲线程存活时间
    TimeUnit.SECONDS,        // 时间单位
    new LinkedBlockingQueue<>(100),  // 任务队列
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);
```

### 7.4 最佳实践

```java
// ✓ 为不同类型的任务创建独立的线程池
ExecutorService ioExecutor = Executors.newFixedThreadPool(100); // IO密集
ExecutorService cpuExecutor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() // CPU密集
);

// IO密集型任务
CompletableFuture.supplyAsync(() -> readFromDatabase(), ioExecutor);

// CPU密集型任务
CompletableFuture.supplyAsync(() -> complexCalculation(), cpuExecutor);
```

---

## 八、实战模式

### 8.1 并行API调用

```java
// 并行调用多个API
CompletableFuture<User> userFuture = fetchUser();
CompletableFuture<List<Order>> ordersFuture = fetchOrders();
CompletableFuture<Profile> profileFuture = fetchProfile();

// 合并结果
CompletableFuture<Dashboard> dashboard = userFuture
    .thenCombine(ordersFuture, (user, orders) -> new UserWithOrders(user, orders))
    .thenCombine(profileFuture, (uo, profile) -> new Dashboard(uo.user, uo.orders, profile));
```

### 8.2 异步处理管道

```java
// 数据处理流水线
CompletableFuture<Report> report =
    loadDataAsync()                    // 1. 加载数据
        .thenCompose(data -> validateAsync(data))  // 2. 验证
        .thenCompose(data -> transformAsync(data)) // 3. 转换
        .thenCompose(data -> enrichAsync(data))    // 4. 丰富数据
        .thenApply(data -> generateReport(data))   // 5. 生成报告
        .exceptionally(ex -> createErrorReport(ex)); // 异常处理
```

### 8.3 超时控制（Java 9+）

```java
// Java 9+
CompletableFuture<String> future = fetchDataAsync()
    .orTimeout(3, TimeUnit.SECONDS)  // 超时3秒
    .exceptionally(ex -> {
        if (ex instanceof TimeoutException) {
            return "Default value";
        }
        throw new RuntimeException(ex);
    });

// Java 8兼容方案
CompletableFuture<String> withTimeout = CompletableFuture.supplyAsync(() -> {
    try {
        return fetchDataAsync().get(3, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        return "Default value";
    }
});
```

### 8.4 重试机制

```java
public CompletableFuture<String> fetchWithRetry(int maxRetries) {
    return CompletableFuture.supplyAsync(() -> fetchData())
        .exceptionally(ex -> {
            if (maxRetries > 0) {
                System.out.println("重试中... 剩余次数: " + maxRetries);
                return fetchWithRetry(maxRetries - 1).join();
            }
            throw new RuntimeException("重试失败", ex);
        });
}
```

### 8.5 缓存模式

```java
private final Map<String, CompletableFuture<User>> cache = new ConcurrentHashMap<>();

public CompletableFuture<User> getUser(String id) {
    return cache.computeIfAbsent(id, key ->
        CompletableFuture.supplyAsync(() -> fetchUserFromDB(key))
    );
}
```

---

## 九、最佳实践

### 1. 避免阻塞

```java
// ✗ 不好：阻塞主线程
String result = future.get();

// ✓ 好：使用回调
future.thenAccept(result -> handleResult(result));

// ✓ 好：在Web框架中返回CompletableFuture
@GetAsync
public CompletableFuture<User> getUser(@PathVariable String id) {
    return userService.getUserAsync(id);
}
```

### 2. 使用自定义线程池

```java
// ✗ 不好：使用默认线程池
CompletableFuture.supplyAsync(() -> task());

// ✓ 好：使用自定义线程池
CompletableFuture.supplyAsync(() -> task(), customExecutor);
```

### 3. 正确处理异常

```java
// ✗ 不好：吞掉异常
future.exceptionally(ex -> null);

// ✓ 好：记录日志并返回合理默认值
future.exceptionally(ex -> {
    logger.error("Error occurred", ex);
    metrics.recordError(ex);
    return getDefaultValue();
});
```

### 4. 给线程命名

```java
ThreadFactory factory = new ThreadFactory() {
    private AtomicInteger counter = new AtomicInteger();

    public Thread newThread(Runnable r) {
        return new Thread(r, "AsyncTask-" + counter.incrementAndGet());
    }
};

ExecutorService executor = Executors.newFixedThreadPool(10, factory);
```

### 5. 及时关闭线程池

```java
try {
    // 执行任务
    CompletableFuture.supplyAsync(() -> task(), executor).join();
} finally {
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
}
```

### 6. 避免嵌套CompletableFuture

```java
// ✗ 不好：嵌套
CompletableFuture<CompletableFuture<User>> nested =
    future.thenApply(id -> getUserAsync(id));

// ✓ 好：使用thenCompose扁平化
CompletableFuture<User> flat =
    future.thenCompose(id -> getUserAsync(id));
```

---

## 十、性能优化

### 10.1 减少上下文切换

```java
// 同步方法：在同一线程执行
future.thenApply(x -> transform(x))
      .thenApply(x -> process(x));

// 异步方法：切换线程（有开销）
future.thenApplyAsync(x -> transform(x))
      .thenApplyAsync(x -> process(x));
```

**建议**：
- CPU密集型：使用同步方法（`thenApply`）
- IO密集型：使用异步方法（`thenApplyAsync`）

### 10.2 合理设置线程池大小

```java
// CPU密集型：线程数 = CPU核心数 + 1
int cpuCount = Runtime.getRuntime().availableProcessors();
ExecutorService cpuPool = Executors.newFixedThreadPool(cpuCount + 1);

// IO密集型：线程数 = CPU核心数 * 2（或更多）
ExecutorService ioPool = Executors.newFixedThreadPool(cpuCount * 2);
```

### 10.3 避免过度并行

```java
// ✗ 不好：创建过多任务
List<CompletableFuture<String>> futures = ids.stream()
    .map(id -> CompletableFuture.supplyAsync(() -> fetch(id)))
    .collect(Collectors.toList());

// ✓ 好：限制并发数
ExecutorService limitedExecutor = Executors.newFixedThreadPool(10);
List<CompletableFuture<String>> futures = ids.stream()
    .map(id -> CompletableFuture.supplyAsync(() -> fetch(id), limitedExecutor))
    .collect(Collectors.toList());
```

---

## 十一、常见陷阱

### 陷阱1: join() vs get()

```java
// get() 抛出检查异常
try {
    String result = future.get();
} catch (InterruptedException | ExecutionException e) {
    // 必须处理
}

// join() 抛出非检查异常
String result = future.join(); // 更简洁
```

**建议**：链式调用中使用`join()`更简洁

### 陷阱2: 忘记处理异常

```java
// ✗ 异常会被吞掉
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Error");
}); // 异常丢失！

// ✓ 添加异常处理
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Error");
}).exceptionally(ex -> {
    logger.error("Error", ex);
    return defaultValue;
});
```

### 陷阱3: 线程池未关闭

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture.supplyAsync(() -> task(), executor);
// 忘记关闭 → JVM无法退出！

// 正确做法
executor.shutdown();
```

### 陷阱4: 阻塞导致死锁

```java
// ✗ 可能死锁
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
    return f2.join(); // 等待f2
});

CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    return f1.join(); // 等待f1
});
```

---

## 十二、与其他技术对比

### CompletableFuture vs Callback

**回调地狱**：
```java
// 传统回调
fetchUser(id, user -> {
    fetchOrders(user, orders -> {
        fetchProfile(user, profile -> {
            // 嵌套3层！
        });
    });
});
```

**CompletableFuture**：
```java
// 链式调用
fetchUser(id)
    .thenCompose(user -> fetchOrders(user))
    .thenCompose(user -> fetchProfile(user))
    .thenAccept(profile -> handleProfile(profile));
```

### CompletableFuture vs RxJava

- **CompletableFuture**: 单个异步值
- **RxJava**: 异步数据流（多个值）

---

## 十三、常见面试题

**Q1: CompletableFuture和Future的区别？**
A:
- `Future`: 只能阻塞获取，无法回调
- `CompletableFuture`: 支持链式调用、组合、异常处理

**Q2: thenApply和thenCompose的区别？**
A:
- `thenApply`: 转换值，函数返回普通类型
- `thenCompose`: 扁平化，函数返回`CompletableFuture`

**Q3: 如何实现超时控制？**
A: Java 9+使用`orTimeout()`, Java 8使用`get(timeout)`或自定义实现

**Q4: 异常在哪里抛出？**
A: 在调用`get()`/`join()`时，或在异常处理器中捕获

**Q5: 默认使用什么线程池？**
A: `ForkJoinPool.commonPool()`，建议自定义线程池

---

## 总结

本教程学习了：
- ✅ CompletableFuture的创建方式
- ✅ 链式调用与转换（thenApply、thenCompose）
- ✅ 组合多个Future（thenCombine、allOf、anyOf）
- ✅ 异常处理（exceptionally、handle、whenComplete）
- ✅ 自定义线程池的使用
- ✅ 实战模式与最佳实践

**下一步**：完成本模块的练习题，实战应用CompletableFuture

---

## 参考资料

- [CompletableFuture JavaDoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
- 《Java并发编程实战》第6章
- [Baeldung - CompletableFuture Guide](https://www.baeldung.com/java-completablefuture)
