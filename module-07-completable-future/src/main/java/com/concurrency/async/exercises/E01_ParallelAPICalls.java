package com.concurrency.async.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 练习1: 并行API调用
 *
 * 任务描述：
 * 你正在开发一个用户信息聚合服务，需要从多个微服务并行获取数据，
 * 然后合并成完整的用户信息页面。
 *
 * 要求实现：
 * 1. 并行调用3个API：
 *    - getUserBasicInfo(userId) - 基本信息（100ms）
 *    - getUserOrders(userId) - 订单列表（150ms）
 *    - getUserRecommendations(userId) - 推荐商品（200ms）
 *
 * 2. 合并所有结果到UserDashboard对象
 *
 * 3. 如果任何一个API调用失败，使用默认值继续
 *
 * 4. 总耗时应接近最慢的API（200ms），而非累加（450ms）
 *
 * 5. 添加超时控制：如果某个API超过300ms未响应，使用默认值
 *
 * 难度：🟢 简单
 * 预计时间：20分钟
 */
public class E01_ParallelAPICalls {

    public static void main(String[] args) throws Exception {
        E01_ParallelAPICalls exercise = new E01_ParallelAPICalls();

        System.out.println("=== 练习1: 并行API调用 ===\n");

        long startTime = System.currentTimeMillis();

        // TODO: 实现并行调用
        UserDashboard dashboard = exercise.fetchUserDashboard(1001).get();

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n结果: " + dashboard);
        System.out.println("总耗时: " + duration + "ms");

        // 验证
        if (duration < 250) {
            System.out.println("✓ 成功：耗时符合预期（并行执行）");
        } else {
            System.out.println("✗ 失败：耗时过长，可能是串行执行");
        }
    }

    /**
     * TODO: 实现此方法
     *
     * 提示：
     * 1. 使用 CompletableFuture.supplyAsync() 并行调用3个API
     * 2. 使用 thenCombine() 或 allOf() 合并结果
     * 3. 使用 exceptionally() 处理异常
     * 4. 考虑使用自定义线程池
     *
     * @param userId 用户ID
     * @return 包含所有信息的UserDashboard
     */
    public CompletableFuture<UserDashboard> fetchUserDashboard(int userId) {
        // TODO: 在这里实现你的代码

        // 示例实现（串行，错误示范）：
        return CompletableFuture.supplyAsync(() -> {
            UserBasicInfo basicInfo = getUserBasicInfo(userId);
            UserOrders orders = getUserOrders(userId);
            UserRecommendations recommendations = getUserRecommendations(userId);
            return new UserDashboard(basicInfo, orders, recommendations);
        });

        // 你的任务：改为并行实现
    }

    // ========== 模拟API（不要修改） ==========

    /**
     * 模拟获取用户基本信息
     */
    private UserBasicInfo getUserBasicInfo(int userId) {
        sleep(100);
        if (ThreadLocalRandom.current().nextInt(10) == 0) {
            throw new RuntimeException("BasicInfo API 失败");
        }
        return new UserBasicInfo(userId, "User-" + userId, "user" + userId + "@example.com");
    }

    /**
     * 模拟获取用户订单
     */
    private UserOrders getUserOrders(int userId) {
        sleep(150);
        if (ThreadLocalRandom.current().nextInt(10) == 0) {
            throw new RuntimeException("Orders API 失败");
        }
        return new UserOrders(userId, 5);
    }

    /**
     * 模拟获取推荐商品
     */
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
