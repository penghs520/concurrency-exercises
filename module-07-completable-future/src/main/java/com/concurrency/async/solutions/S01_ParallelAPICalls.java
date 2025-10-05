package com.concurrency.async.solutions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 练习1参考答案: 并行API调用
 *
 * 实现要点：
 * 1. 使用自定义线程池（避免共用ForkJoinPool）
 * 2. 并行启动3个API调用
 * 3. 使用thenCombine合并结果
 * 4. 使用exceptionally处理异常，提供默认值
 */
public class S01_ParallelAPICalls {

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public static void main(String[] args) throws Exception {
        S01_ParallelAPICalls solution = new S01_ParallelAPICalls();

        System.out.println("=== 练习1参考答案: 并行API调用 ===\n");

        try {
            long startTime = System.currentTimeMillis();

            // 并行调用
            UserDashboard dashboard = solution.fetchUserDashboard(1001).get();

            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\n结果: " + dashboard);
            System.out.println("总耗时: " + duration + "ms");

            if (duration < 250) {
                System.out.println("✓ 成功：耗时符合预期（并行执行）");
            } else {
                System.out.println("✗ 失败：耗时过长");
            }

        } finally {
            solution.shutdown();
        }
    }

    /**
     * 方案1: 使用thenCombine逐步合并（推荐）
     */
    public CompletableFuture<UserDashboard> fetchUserDashboard(int userId) {
        // 并行启动3个API调用
        CompletableFuture<UserBasicInfo> basicInfoFuture = CompletableFuture
                .supplyAsync(() -> getUserBasicInfo(userId), executor)
                .exceptionally(ex -> {
                    System.err.println("BasicInfo API 失败，使用默认值: " + ex.getMessage());
                    return new UserBasicInfo(userId, "Unknown", "unknown@example.com");
                });

        CompletableFuture<UserOrders> ordersFuture = CompletableFuture
                .supplyAsync(() -> getUserOrders(userId), executor)
                .exceptionally(ex -> {
                    System.err.println("Orders API 失败，使用默认值: " + ex.getMessage());
                    return new UserOrders(userId, 0);
                });

        CompletableFuture<UserRecommendations> recommendationsFuture = CompletableFuture
                .supplyAsync(() -> getUserRecommendations(userId), executor)
                .exceptionally(ex -> {
                    System.err.println("Recommendations API 失败，使用默认值: " + ex.getMessage());
                    return new UserRecommendations(userId, "暂无推荐");
                });

        // 合并结果
        return basicInfoFuture
                .thenCombine(ordersFuture, (basicInfo, orders) -> new Object[]{basicInfo, orders})
                .thenCombine(recommendationsFuture, (arr, recommendations) -> {
                    UserBasicInfo basicInfo = (UserBasicInfo) arr[0];
                    UserOrders orders = (UserOrders) arr[1];
                    return new UserDashboard(basicInfo, orders, recommendations);
                });
    }

    /**
     * 方案2: 使用allOf + Stream（更简洁）
     */
    public CompletableFuture<UserDashboard> fetchUserDashboardV2(int userId) {
        CompletableFuture<UserBasicInfo> basicInfoFuture = CompletableFuture
                .supplyAsync(() -> getUserBasicInfo(userId), executor)
                .exceptionally(ex -> new UserBasicInfo(userId, "Unknown", "unknown@example.com"));

        CompletableFuture<UserOrders> ordersFuture = CompletableFuture
                .supplyAsync(() -> getUserOrders(userId), executor)
                .exceptionally(ex -> new UserOrders(userId, 0));

        CompletableFuture<UserRecommendations> recommendationsFuture = CompletableFuture
                .supplyAsync(() -> getUserRecommendations(userId), executor)
                .exceptionally(ex -> new UserRecommendations(userId, "暂无推荐"));

        // 等待所有完成后合并
        return CompletableFuture.allOf(basicInfoFuture, ordersFuture, recommendationsFuture)
                .thenApply(v -> new UserDashboard(
                        basicInfoFuture.join(),
                        ordersFuture.join(),
                        recommendationsFuture.join()
                ));
    }

    /**
     * 方案3: 使用handle统一异常处理
     */
    public CompletableFuture<UserDashboard> fetchUserDashboardV3(int userId) {
        CompletableFuture<UserBasicInfo> basicInfoFuture = CompletableFuture
                .supplyAsync(() -> getUserBasicInfo(userId), executor)
                .handle((result, ex) -> ex == null ? result :
                        new UserBasicInfo(userId, "Unknown", "unknown@example.com"));

        CompletableFuture<UserOrders> ordersFuture = CompletableFuture
                .supplyAsync(() -> getUserOrders(userId), executor)
                .handle((result, ex) -> ex == null ? result :
                        new UserOrders(userId, 0));

        CompletableFuture<UserRecommendations> recommendationsFuture = CompletableFuture
                .supplyAsync(() -> getUserRecommendations(userId), executor)
                .handle((result, ex) -> ex == null ? result :
                        new UserRecommendations(userId, "暂无推荐"));

        return CompletableFuture.allOf(basicInfoFuture, ordersFuture, recommendationsFuture)
                .thenApply(v -> new UserDashboard(
                        basicInfoFuture.join(),
                        ordersFuture.join(),
                        recommendationsFuture.join()
                ));
    }

    // ========== 模拟API ==========

    private UserBasicInfo getUserBasicInfo(int userId) {
        sleep(100);
        if (ThreadLocalRandom.current().nextInt(10) == 0) {
            throw new RuntimeException("BasicInfo API 失败");
        }
        return new UserBasicInfo(userId, "User-" + userId, "user" + userId + "@example.com");
    }

    private UserOrders getUserOrders(int userId) {
        sleep(150);
        if (ThreadLocalRandom.current().nextInt(10) == 0) {
            throw new RuntimeException("Orders API 失败");
        }
        return new UserOrders(userId, 5);
    }

    private UserRecommendations getUserRecommendations(int userId) {
        sleep(200);
        if (ThreadLocalRandom.current().nextInt(10) == 0) {
            throw new RuntimeException("Recommendations API 失败");
        }
        return new UserRecommendations(userId, "商品A, 商品B, 商品C");
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 数据模型 ==========

    static class UserBasicInfo {
        int userId;
        String name;
        String email;

        UserBasicInfo(int userId, String name, String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }

        @Override
        public String toString() {
            return "BasicInfo{userId=" + userId + ", name='" + name + "', email='" + email + "'}";
        }
    }

    static class UserOrders {
        int userId;
        int orderCount;

        UserOrders(int userId, int orderCount) {
            this.userId = userId;
            this.orderCount = orderCount;
        }

        @Override
        public String toString() {
            return "Orders{userId=" + userId + ", count=" + orderCount + "}";
        }
    }

    static class UserRecommendations {
        int userId;
        String items;

        UserRecommendations(int userId, String items) {
            this.userId = userId;
            this.items = items;
        }

        @Override
        public String toString() {
            return "Recommendations{userId=" + userId + ", items='" + items + "'}";
        }
    }

    static class UserDashboard {
        UserBasicInfo basicInfo;
        UserOrders orders;
        UserRecommendations recommendations;

        UserDashboard(UserBasicInfo basicInfo, UserOrders orders, UserRecommendations recommendations) {
            this.basicInfo = basicInfo;
            this.orders = orders;
            this.recommendations = recommendations;
        }

        @Override
        public String toString() {
            return "UserDashboard{\n" +
                    "  " + basicInfo + "\n" +
                    "  " + orders + "\n" +
                    "  " + recommendations + "\n" +
                    "}";
        }
    }
}
