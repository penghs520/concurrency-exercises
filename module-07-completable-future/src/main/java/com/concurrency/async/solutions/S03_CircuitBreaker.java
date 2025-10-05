package com.concurrency.async.solutions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 练习3参考答案: 熔断器模式
 *
 * 实现要点：
 * 1. 维护三个状态：CLOSED、OPEN、HALF_OPEN
 * 2. 使用滑动窗口统计失败率
 * 3. 失败率超过阈值时打开熔断器
 * 4. 打开后等待一定时间尝试恢复
 * 5. 半开状态下测试服务是否恢复
 */
public class S03_CircuitBreaker {

    public static void main(String[] args) throws Exception {
        S03_CircuitBreaker solution = new S03_CircuitBreaker();
        SimpleCircuitBreaker breaker = solution.new SimpleCircuitBreaker();

        System.out.println("=== 练习3参考答案: 熔断器模式 ===\n");

        // 测试场景1: 服务正常
        System.out.println("场景1: 服务正常");
        for (int i = 0; i < 5; i++) {
            CompletableFuture<String> result = breaker.call(() -> solution.stableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get());
        }

        // 测试场景2: 服务故障，触发熔断
        System.out.println("\n场景2: 服务故障");
        for (int i = 0; i < 10; i++) {
            CompletableFuture<String> result = breaker.call(() -> solution.unstableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get() + " (状态: " + breaker.getState() + ")");
        }

        // 测试场景3: 熔断器打开，请求被拒绝
        System.out.println("\n场景3: 熔断器打开");
        for (int i = 0; i < 3; i++) {
            CompletableFuture<String> result = breaker.call(() -> solution.stableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get());
        }

        // 测试场景4: 等待恢复
        System.out.println("\n场景4: 等待5秒后尝试恢复...");
        Thread.sleep(5000);

        for (int i = 0; i < 5; i++) {
            CompletableFuture<String> result = breaker.call(() -> solution.stableService());
            System.out.println("  请求" + (i + 1) + ": " + result.get() + " (状态: " + breaker.getState() + ")");
        }

        System.out.println("\n最终状态: " + breaker.getState());
    }

    /**
     * 简单熔断器实现
     */
    class SimpleCircuitBreaker {
        private static final int WINDOW_SIZE = 10;
        private static final double FAILURE_THRESHOLD = 0.5;
        private static final long WAIT_DURATION_MS = 5000;

        private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
        private final AtomicInteger windowIndex = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);

        // 滑动窗口：true=成功，false=失败
        private final boolean[] window = new boolean[WINDOW_SIZE];
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);

        public CompletableFuture<String> call(java.util.function.Supplier<String> supplier) {
            // 检查是否可以从OPEN转到HALF_OPEN
            if (state.get() == CircuitState.OPEN) {
                long now = System.currentTimeMillis();
                if (now - lastFailureTime.get() > WAIT_DURATION_MS) {
                    transitionTo(CircuitState.HALF_OPEN);
                } else {
                    // 仍在等待期，直接返回降级响应
                    return CompletableFuture.completedFuture("服务降级: 熔断器打开");
                }
            }

            // 调用实际服务
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String result = supplier.get();
                    recordSuccess();
                    return result;
                } catch (Exception e) {
                    recordFailure();
                    return "服务调用失败: " + e.getMessage();
                }
            });
        }

        private synchronized void recordSuccess() {
            int index = windowIndex.getAndUpdate(i -> (i + 1) % WINDOW_SIZE);

            // 更新窗口
            if (!window[index]) {
                failureCount.decrementAndGet();
            }
            window[index] = true;
            successCount.incrementAndGet();

            // 如果是半开状态且成功，关闭熔断器
            if (state.get() == CircuitState.HALF_OPEN) {
                transitionTo(CircuitState.CLOSED);
                reset();
            }
        }

        private synchronized void recordFailure() {
            int index = windowIndex.getAndUpdate(i -> (i + 1) % WINDOW_SIZE);

            // 更新窗口
            if (window[index]) {
                successCount.decrementAndGet();
            }
            window[index] = false;
            failureCount.incrementAndGet();

            lastFailureTime.set(System.currentTimeMillis());

            // 如果是半开状态且失败，重新打开熔断器
            if (state.get() == CircuitState.HALF_OPEN) {
                transitionTo(CircuitState.OPEN);
                return;
            }

            // 检查是否需要打开熔断器
            double failureRate = getFailureRate();
            if (failureRate >= FAILURE_THRESHOLD && getTotalCount() >= WINDOW_SIZE) {
                transitionTo(CircuitState.OPEN);
            }
        }

        private double getFailureRate() {
            int total = getTotalCount();
            if (total == 0) return 0.0;
            return (double) failureCount.get() / total;
        }

        private int getTotalCount() {
            return successCount.get() + failureCount.get();
        }

        private void reset() {
            successCount.set(0);
            failureCount.set(0);
            windowIndex.set(0);
        }

        private void transitionTo(CircuitState newState) {
            CircuitState oldState = state.get();
            if (oldState != newState) {
                state.set(newState);
                System.out.println("  熔断器状态: " + oldState + " -> " + newState +
                        " (失败率: " + String.format("%.1f%%", getFailureRate() * 100) + ")");
            }
        }

        public CircuitState getState() {
            return state.get();
        }
    }

    /**
     * 进阶版：使用更精确的滑动窗口
     */
    class AdvancedCircuitBreaker {
        private static final int WINDOW_SIZE = 10;
        private static final double FAILURE_THRESHOLD = 0.5;
        private static final long WAIT_DURATION_MS = 5000;

        private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
        private final AtomicLong lastFailureTime = new AtomicLong(0);

        // 使用循环数组作为滑动窗口
        private final AtomicInteger currentIndex = new AtomicInteger(0);
        private final Boolean[] results = new Boolean[WINDOW_SIZE];

        public CompletableFuture<String> call(java.util.function.Supplier<String> supplier) {
            if (state.get() == CircuitState.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime.get() > WAIT_DURATION_MS) {
                    transitionTo(CircuitState.HALF_OPEN);
                } else {
                    return CompletableFuture.completedFuture("服务降级: 熔断器打开");
                }
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String result = supplier.get();
                    onSuccess();
                    return result;
                } catch (Exception e) {
                    onFailure();
                    throw e;
                }
            }).exceptionally(ex -> "服务调用失败: " + ex.getCause().getMessage());
        }

        private synchronized void onSuccess() {
            recordResult(true);

            if (state.get() == CircuitState.HALF_OPEN) {
                transitionTo(CircuitState.CLOSED);
            }
        }

        private synchronized void onFailure() {
            recordResult(false);
            lastFailureTime.set(System.currentTimeMillis());

            if (state.get() == CircuitState.HALF_OPEN) {
                transitionTo(CircuitState.OPEN);
                return;
            }

            if (shouldOpenCircuit()) {
                transitionTo(CircuitState.OPEN);
            }
        }

        private void recordResult(boolean success) {
            int index = currentIndex.getAndIncrement() % WINDOW_SIZE;
            results[index] = success;
        }

        private boolean shouldOpenCircuit() {
            int total = 0;
            int failures = 0;

            for (Boolean result : results) {
                if (result != null) {
                    total++;
                    if (!result) failures++;
                }
            }

            if (total < WINDOW_SIZE) return false;

            double failureRate = (double) failures / total;
            return failureRate >= FAILURE_THRESHOLD;
        }

        private void transitionTo(CircuitState newState) {
            CircuitState oldState = state.get();
            if (oldState != newState) {
                state.set(newState);
                System.out.println("  熔断器状态: " + oldState + " -> " + newState);
            }
        }
    }

    // ========== 熔断器状态 ==========
    enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    // ========== 模拟服务 ==========

    private String stableService() {
        sleep(50);
        return "成功";
    }

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
