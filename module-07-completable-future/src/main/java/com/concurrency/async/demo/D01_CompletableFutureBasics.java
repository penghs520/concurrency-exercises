package com.concurrency.async.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demo 01: CompletableFuture基础
 *
 * 本示例演示：
 * 1. CompletableFuture的创建方式
 * 2. 链式调用（thenApply、thenAccept、thenRun）
 * 3. 同步 vs 异步执行
 * 4. thenApply vs thenCompose
 */
public class D01_CompletableFutureBasics {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture基础演示 ===\n");

        // 1. 创建CompletableFuture
        demo1_CreateFuture();
        Thread.sleep(500);

        // 2. 链式调用
        demo2_Chaining();
        Thread.sleep(500);

        // 3. 同步 vs 异步
        demo3_SyncVsAsync();
        Thread.sleep(1000);

        // 4. thenApply vs thenCompose
        demo4_ApplyVsCompose();

        System.out.println("\n主线程执行完毕");
    }

    /**
     * Demo 1: 创建CompletableFuture的多种方式
     */
    private static void demo1_CreateFuture() throws Exception {
        System.out.println("--- Demo 1: 创建CompletableFuture ---");

        // 方式1: completedFuture - 已完成的Future
        CompletableFuture<String> completed = CompletableFuture.completedFuture("立即完成的结果");
        System.out.println("completedFuture: " + completed.get());

        // 方式2: supplyAsync - 异步执行有返回值的任务
        CompletableFuture<String> supplyFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("  supplyAsync线程: " + Thread.currentThread().getName());
            return "异步计算结果";
        });
        System.out.println("supplyAsync: " + supplyFuture.get());

        // 方式3: runAsync - 异步执行无返回值的任务
        CompletableFuture<Void> runFuture = CompletableFuture.runAsync(() -> {
            System.out.println("  runAsync线程: " + Thread.currentThread().getName());
            System.out.println("  执行无返回值的异步任务");
        });
        runFuture.get(); // 等待完成

        // 方式4: 手动创建并完成
        CompletableFuture<String> manual = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(100);
                manual.complete("手动完成的结果");
            } catch (InterruptedException e) {
                manual.completeExceptionally(e);
            }
        }).start();
        System.out.println("手动完成: " + manual.get());
    }

    /**
     * Demo 2: 链式调用
     */
    private static void demo2_Chaining() throws Exception {
        System.out.println("\n--- Demo 2: 链式调用 ---");

        // thenApply: 转换结果（有参数，有返回值）
        CompletableFuture<Integer> applyChain = CompletableFuture.supplyAsync(() -> 10)
                .thenApply(x -> {
                    System.out.println("  第一步: " + x + " * 2");
                    return x * 2; // 20
                })
                .thenApply(x -> {
                    System.out.println("  第二步: " + x + " + 5");
                    return x + 5; // 25
                })
                .thenApply(x -> {
                    System.out.println("  第三步: " + x + " / 5");
                    return x / 5; // 5
                });

        System.out.println("thenApply最终结果: " + applyChain.get());

        // thenAccept: 消费结果（有参数，无返回值）
        CompletableFuture<Void> acceptChain = CompletableFuture.supplyAsync(() -> "Hello")
                .thenAccept(s -> System.out.println("  接收到: " + s + " World"));
        acceptChain.get();

        // thenRun: 执行操作（无参数，无返回值）
        CompletableFuture<Void> runChain = CompletableFuture.supplyAsync(() -> "完成")
                .thenRun(() -> System.out.println("  任务完成，执行清理工作"));
        runChain.get();
    }

    /**
     * Demo 3: 同步 vs 异步执行
     */
    private static void demo3_SyncVsAsync() throws Exception {
        System.out.println("\n--- Demo 3: 同步 vs 异步执行 ---");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 同步执行：在完成上一步的线程中执行
            System.out.println("同步执行（thenApply）：");
            CompletableFuture<String> syncFuture = CompletableFuture.supplyAsync(() -> {
                printThread("  supplyAsync");
                return "Hello";
            }, executor).thenApply(s -> {
                printThread("  thenApply");
                return s + " World";
            });
            System.out.println("  结果: " + syncFuture.get());

            // 异步执行：在线程池中执行
            System.out.println("\n异步执行（thenApplyAsync）：");
            CompletableFuture<String> asyncFuture = CompletableFuture.supplyAsync(() -> {
                printThread("  supplyAsync");
                return "Hello";
            }, executor).thenApplyAsync(s -> {
                printThread("  thenApplyAsync");
                return s + " Async";
            }, executor);
            System.out.println("  结果: " + asyncFuture.get());

        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    /**
     * Demo 4: thenApply vs thenCompose
     */
    private static void demo4_ApplyVsCompose() throws Exception {
        System.out.println("\n--- Demo 4: thenApply vs thenCompose ---");

        // 模拟两个异步操作
        CompletableFuture<String> getUserName = CompletableFuture.supplyAsync(() -> {
            System.out.println("  获取用户名...");
            return "Alice";
        });

        CompletableFuture<String> getUserEmail = CompletableFuture.supplyAsync(() -> {
            System.out.println("  获取邮箱...");
            return "alice@example.com";
        });

        // 错误示例：thenApply导致嵌套Future
        System.out.println("\nthenApply（会导致嵌套）：");
        CompletableFuture<CompletableFuture<String>> nestedFuture =
                getUserName.thenApply(name -> {
                    return fetchUserEmail(name); // 返回CompletableFuture
                });
        // 类型是 CompletableFuture<CompletableFuture<String>>，需要两次get
        System.out.println("  嵌套结果: " + nestedFuture.get().get());

        // 正确示例：thenCompose扁平化
        System.out.println("\nthenCompose（扁平化）：");
        CompletableFuture<String> flatFuture =
                getUserName.thenCompose(name -> {
                    return fetchUserEmail(name); // 返回CompletableFuture，自动扁平化
                });
        // 类型是 CompletableFuture<String>，只需一次get
        System.out.println("  扁平化结果: " + flatFuture.get());

        // 实际应用：链式异步调用
        System.out.println("\n链式异步调用：");
        CompletableFuture<String> pipeline = CompletableFuture.supplyAsync(() -> 1001)
                .thenCompose(userId -> fetchUserById(userId))
                .thenCompose(user -> fetchOrdersByUser(user))
                .thenApply(orders -> "用户有 " + orders + " 个订单");

        System.out.println("  " + pipeline.get());
    }

    // ========== 辅助方法 ==========

    private static void printThread(String stage) {
        System.out.println(stage + " 线程: " + Thread.currentThread().getName());
    }

    private static CompletableFuture<String> fetchUserEmail(String name) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("  查询 " + name + " 的邮箱...");
            return name.toLowerCase() + "@example.com";
        });
    }

    private static CompletableFuture<String> fetchUserById(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("  根据ID " + userId + " 查询用户...");
            return "User-" + userId;
        });
    }

    private static CompletableFuture<Integer> fetchOrdersByUser(String user) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("  查询 " + user + " 的订单...");
            return 5; // 模拟返回5个订单
        });
    }
}
