package com.concurrency.async.demo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Demo 03: 组合多个Future
 *
 * 本示例演示：
 * 1. thenCombine - 合并两个Future的结果
 * 2. thenAcceptBoth - 消费两个结果
 * 3. runAfterBoth - 两个都完成后执行
 * 4. applyToEither - 任一完成即处理
 * 5. allOf - 等待所有完成
 * 6. anyOf - 等待任一完成
 */
public class D03_CombiningFutures {

    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws Exception {
        System.out.println("=== 组合多个CompletableFuture演示 ===\n");

        try {
            // 1. 组合两个Future - thenCombine
            demo1_ThenCombine();

            // 2. 消费两个结果 - thenAcceptBoth
            demo2_ThenAcceptBoth();

            // 3. 两个都完成后执行 - runAfterBoth
            demo3_RunAfterBoth();

            // 4. 任一完成即处理 - applyToEither
            demo4_ApplyToEither();

            // 5. 等待所有完成 - allOf
            demo5_AllOf();

            // 6. 等待任一完成 - anyOf
            demo6_AnyOf();

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        System.out.println("\n主线程执行完毕");
    }

    /**
     * Demo 1: thenCombine - 合并两个Future的结果
     */
    private static void demo1_ThenCombine() throws Exception {
        System.out.println("--- Demo 1: thenCombine ---");

        // 并行获取用户名和年龄，然后合并
        CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            System.out.println("  获取姓名: Alice");
            return "Alice";
        }, executor);

        CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            System.out.println("  获取年龄: 25");
            return 25;
        }, executor);

        // 合并两个结果
        CompletableFuture<String> combined = nameFuture.thenCombine(ageFuture, (name, age) -> {
            return name + " is " + age + " years old";
        });

        System.out.println("合并结果: " + combined.get());

        // 实际应用：并行调用多个API
        System.out.println("\n并行API调用:");
        CompletableFuture<String> userInfo = fetchUserProfile(1001)
                .thenCombine(fetchUserPreferences(1001), (profile, prefs) -> {
                    return "User: " + profile + ", Preferences: " + prefs;
                });

        System.out.println(userInfo.get());
    }

    /**
     * Demo 2: thenAcceptBoth - 消费两个结果
     */
    private static void demo2_ThenAcceptBoth() throws Exception {
        System.out.println("\n--- Demo 2: thenAcceptBoth ---");

        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello", executor);
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World", executor);

        // 消费两个结果，但不返回值
        CompletableFuture<Void> result = future1.thenAcceptBoth(future2, (s1, s2) -> {
            System.out.println("  接收到: " + s1 + " " + s2);
        });

        result.get();
    }

    /**
     * Demo 3: runAfterBoth - 两个都完成后执行
     */
    private static void demo3_RunAfterBoth() throws Exception {
        System.out.println("\n--- Demo 3: runAfterBoth ---");

        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> {
            sleep(100);
            System.out.println("  任务1完成");
        }, executor);

        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> {
            sleep(150);
            System.out.println("  任务2完成");
        }, executor);

        // 两个都完成后执行清理工作
        task1.runAfterBoth(task2, () -> {
            System.out.println("  所有任务完成，执行清理");
        }).get();
    }

    /**
     * Demo 4: applyToEither - 任一完成即处理
     */
    private static void demo4_ApplyToEither() throws Exception {
        System.out.println("\n--- Demo 4: applyToEither ---");

        // 模拟主备服务
        CompletableFuture<String> primaryService = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "主服务响应";
        }, executor);

        CompletableFuture<String> backupService = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "备用服务响应";
        }, executor);

        // 谁先返回就用谁
        CompletableFuture<String> fastest = primaryService.applyToEither(backupService, result -> {
            System.out.println("  最快响应: " + result);
            return result;
        });

        System.out.println("结果: " + fastest.get());

        // 实际应用：超时控制
        System.out.println("\n超时控制:");
        CompletableFuture<String> dataFuture = fetchDataSlowly();
        CompletableFuture<String> timeoutFuture = CompletableFuture.supplyAsync(() -> {
            sleep(500); // 500ms超时
            return "TIMEOUT";
        }, executor);

        String result = dataFuture.applyToEither(timeoutFuture, r -> r).get();
        System.out.println("  " + (result.equals("TIMEOUT") ? "请求超时" : "请求成功: " + result));
    }

    /**
     * Demo 5: allOf - 等待所有完成
     */
    private static void demo5_AllOf() throws Exception {
        System.out.println("\n--- Demo 5: allOf ---");

        // 批量异步任务
        List<CompletableFuture<String>> futures = Arrays.asList(
                fetchData("API-1", 100),
                fetchData("API-2", 150),
                fetchData("API-3", 200)
        );

        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // allOf返回CompletableFuture<Void>，需要手动收集结果
        allFutures.get();

        System.out.println("\n所有任务完成，收集结果:");
        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        results.forEach(r -> System.out.println("  " + r));

        // 或者使用thenApply收集结果
        System.out.println("\n使用thenApply收集:");
        CompletableFuture<List<String>> allResults = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        ).thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );

        System.out.println("  " + allResults.get());
    }

    /**
     * Demo 6: anyOf - 等待任一完成
     */
    private static void demo6_AnyOf() throws Exception {
        System.out.println("\n--- Demo 6: anyOf ---");

        CompletableFuture<String> task1 = fetchData("任务1", 300);
        CompletableFuture<String> task2 = fetchData("任务2", 100);
        CompletableFuture<String> task3 = fetchData("任务3", 200);

        // 等待任一完成
        CompletableFuture<Object> firstCompleted = CompletableFuture.anyOf(task1, task2, task3);

        System.out.println("最先完成: " + firstCompleted.get());

        // 实际应用：多数据源查询（取最快的）
        System.out.println("\n多数据源查询:");
        CompletableFuture<String> db1 = queryDatabase("DB1", 250);
        CompletableFuture<String> db2 = queryDatabase("DB2", 150);
        CompletableFuture<String> cache = queryDatabase("Cache", 50);

        CompletableFuture<Object> fastestResult = CompletableFuture.anyOf(db1, db2, cache);
        System.out.println("  " + fastestResult.get());
    }

    // ========== 辅助方法 ==========

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static CompletableFuture<String> fetchUserProfile(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Profile-" + userId;
        }, executor);
    }

    private static CompletableFuture<String> fetchUserPreferences(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(120);
            return "Prefs-" + userId;
        }, executor);
    }

    private static CompletableFuture<String> fetchDataSlowly() {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000); // 模拟慢速请求
            return "数据";
        }, executor);
    }

    private static CompletableFuture<String> fetchData(String name, int delay) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(delay);
            System.out.println("  " + name + " 完成 (耗时: " + delay + "ms)");
            return name + " 的数据";
        }, executor);
    }

    private static CompletableFuture<String> queryDatabase(String dbName, int delay) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(delay);
            return dbName + " 查询结果 (耗时: " + delay + "ms)";
        }, executor);
    }
}
