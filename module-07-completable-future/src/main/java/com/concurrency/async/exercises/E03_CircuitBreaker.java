package com.concurrency.async.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 练习3: 简单熔断器实现
 *
 * 任务描述：
 * 实现一个简化版的熔断器（Circuit Breaker）模式，用于保护不稳定的外部服务。
 *
 * 熔断器状态：
 * - CLOSED（关闭）：正常调用服务
 * - OPEN（打开）：服务故障，直接返回失败，不调用服务
 * - HALF_OPEN（半开）：尝试恢复，允许少量请求测试服务
 *
 * 要求实现：
 * 1. 失败率超过50%时，打开熔断器
 * 2. 熔断器打开后，等待5秒再进入半开状态
 * 3. 半开状态下，如果请求成功则关闭熔断器，失败则重新打开
 * 4. 使用滑动窗口统计最近10次请求的成功/失败
 * 5. 熔断器打开时，立即返回降级响应（不调用实际服务）
 *
 * 难度：🔴 困难
 * 预计时间：40分钟
 */
public class E03_CircuitBreaker {

    public static void main(String[] args) throws Exception {
        E03_CircuitBreaker exercise = new E03_CircuitBreaker();
        SimpleCircuitBreaker breaker = exercise.new SimpleCircuitBreaker();

        System.out.println("=== 练习3: 熔断器模式 ===\n");

        // 测试场景1: 服务正常
        System.out.println("场景1: 服务正常");
        for (int i = 0; i < 5; i++) {
            CompletableFuture<String> result = breaker.call(() -> exercise.stableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get());
        }

        // 测试场景2: 服务故障，触发熔断
        System.out.println("\n场景2: 服务故障");
        for (int i = 0; i < 10; i++) {
            CompletableFuture<String> result = breaker.call(() -> exercise.unstableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get() + " (状态: " + breaker.getState() + ")");
        }

        // 测试场景3: 熔断器打开，请求被拒绝
        System.out.println("\n场景3: 熔断器打开");
        for (int i = 0; i < 3; i++) {
            CompletableFuture<String> result = breaker.call(() -> exercise.stableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get());
        }

        // 测试场景4: 等待恢复
        System.out.println("\n场景4: 等待5秒后尝试恢复...");
        Thread.sleep(5000);

        for (int i = 0; i < 5; i++) {
            CompletableFuture<String> result = breaker.call(() -> exercise.stableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get() + " (状态: " + breaker.getState() + ")");
        }

        System.out.println("\n最终状态: " + breaker.getState());
    }

    /**
     * TODO: 实现熔断器
     *
     * 提示：
     * 1. 维护三个状态：CLOSED, OPEN, HALF_OPEN
     * 2. 使用滑动窗口（数组或队列）记录最近N次请求结果
     * 3. 计算失败率，超过阈值则打开熔断器
     * 4. 记录熔断器打开时间，用于判断是否可以进入半开状态
     * 5. 使用AtomicInteger等原子类保证线程安全
     */
    class SimpleCircuitBreaker {
        private static final int WINDOW_SIZE = 10; // 滑动窗口大小
        private static final double FAILURE_THRESHOLD = 0.5; // 失败率阈值50%
        private static final long WAIT_DURATION_MS = 5000; // 等待5秒后尝试恢复

        // TODO: 添加必要的字段
        private volatile CircuitState state = CircuitState.CLOSED;
        private AtomicInteger failureCount = new AtomicInteger(0);
        private AtomicInteger successCount = new AtomicInteger(0);
        private AtomicLong lastFailureTime = new AtomicLong(0);

        // TODO: 实现滑动窗口
        // 提示: 可以使用 boolean[] 数组，true表示成功，false表示失败

        /**
         * TODO: 实现此方法
         *
         * @param supplier 实际的服务调用
         * @return CompletableFuture包装的结果
         */
        public CompletableFuture<String> call(java.util.function.Supplier<String> supplier) {
            // TODO: 实现熔断逻辑

            // 1. 检查当前状态
            // 2. 如果是OPEN状态，检查是否可以进入HALF_OPEN
            // 3. 如果是OPEN且未到恢复时间，直接返回降级响应
            // 4. 如果是CLOSED或HALF_OPEN，调用实际服务
            // 5. 根据结果更新统计和状态

            return CompletableFuture.supplyAsync(() -> {
                // 示例代码（不完整）
                if (state == CircuitState.OPEN) {
                    return "服务降级: 熔断器打开";
                }

                // TODO: 调用实际服务并处理结果
                try {
                    String result = supplier.get();
                    // TODO: 记录成功
                    return result;
                } catch (Exception e) {
                    // TODO: 记录失败
                    return "服务调用失败: " + e.getMessage();
                }
            });
        }

        /**
         * TODO: 记录成功
         */
        private void recordSuccess() {
            // TODO: 实现
        }

        /**
         * TODO: 记录失败
         */
        private void recordFailure() {
            // TODO: 实现
        }

        /**
         * TODO: 计算失败率
         */
        private double getFailureRate() {
            // TODO: 实现
            return 0.0;
        }

        /**
         * TODO: 转换到新状态
         */
        private void transitionTo(CircuitState newState) {
            // TODO: 实现
            System.out.println("  熔断器状态: " + state + " -> " + newState);
            this.state = newState;
        }

        public CircuitState getState() {
            return state;
        }
    }

    // ========== 熔断器状态 ==========
    enum CircuitState {
        CLOSED,    // 关闭：正常调用
        OPEN,      // 打开：拒绝调用
        HALF_OPEN  // 半开：尝试恢复
    }

    // ========== 模拟服务（不要修改） ==========

    /**
     * 稳定服务（总是成功）
     */
    private String stableService() {
        sleep(50);
        return "成功";
    }

    /**
     * 不稳定服务（50%失败率）
     */
    private String unstableService() {
        sleep(50);
        if (ThreadLocalRandom.current().nextBoolean()) {
            throw new RuntimeException("服务故障");
        }
        return "成功";
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
