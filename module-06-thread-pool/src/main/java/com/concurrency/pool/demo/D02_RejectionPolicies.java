package com.concurrency.pool.demo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo 02: 拒绝策略详解
 *
 * 本示例演示：
 * 1. 4种内置拒绝策略的行为
 * 2. 自定义拒绝策略
 * 3. 拒绝策略的选择场景
 */
public class D02_RejectionPolicies {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 拒绝策略演示 ===\n");

        // Demo 1: AbortPolicy - 抛异常（默认）
        demo1_AbortPolicy();
        Thread.sleep(2000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 2: CallerRunsPolicy - 调用者执行
        demo2_CallerRunsPolicy();
        Thread.sleep(2000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 3: DiscardPolicy - 丢弃任务
        demo3_DiscardPolicy();
        Thread.sleep(2000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 4: DiscardOldestPolicy - 丢弃最老任务
        demo4_DiscardOldestPolicy();
        Thread.sleep(2000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 5: 自定义拒绝策略
        demo5_CustomPolicy();

        System.out.println("\n主线程执行完毕");
    }

    /**
     * Demo 1: AbortPolicy - 抛出异常（默认策略）
     * 适用场景: 希望明确感知任务被拒绝
     */
    private static void demo1_AbortPolicy() {
        System.out.println("--- Demo 1: AbortPolicy（抛异常） ---");
        System.out.println("适用场景: 希望明确感知任务被拒绝\n");

        // 配置: 核心1，最大2，队列1 -> 最多处理3个任务
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new NamedThreadFactory("abort-pool"),
                new ThreadPoolExecutor.AbortPolicy()  // 默认策略
        );

        // 提交5个任务，后2个会被拒绝
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            try {
                executor.execute(() -> {
                    System.out.println("[执行] 任务" + taskId + " - " +
                            Thread.currentThread().getName());
                    sleep(2000);
                });
                System.out.println("[提交] 任务" + taskId + " 成功");
            } catch (RejectedExecutionException e) {
                System.err.println("[拒绝] 任务" + taskId + " - " + e.getClass().getSimpleName());
            }
        }

        executor.shutdown();
    }

    /**
     * Demo 2: CallerRunsPolicy - 调用者执行
     * 适用场景: 任务不能丢失，提供降级策略
     */
    private static void demo2_CallerRunsPolicy() {
        System.out.println("--- Demo 2: CallerRunsPolicy（调用者执行） ---");
        System.out.println("适用场景: 任务不能丢失，提供降级策略\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new NamedThreadFactory("caller-pool"),
                new ThreadPoolExecutor.CallerRunsPolicy()  // 调用者执行策略
        );

        System.out.println("主线程: " + Thread.currentThread().getName() + "\n");

        // 提交5个任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("[执行] 任务" + taskId + " - " +
                        Thread.currentThread().getName());
                sleep(2000);
            });
            System.out.println("[提交] 任务" + taskId);
        }

        System.out.println("\n注意: 被拒绝的任务由主线程执行，可能阻塞主线程！");
        executor.shutdown();
    }

    /**
     * Demo 3: DiscardPolicy - 静默丢弃
     * 适用场景: 任务允许丢失，如日志收集
     */
    private static void demo3_DiscardPolicy() {
        System.out.println("--- Demo 3: DiscardPolicy（静默丢弃） ---");
        System.out.println("适用场景: 任务允许丢失，如日志收集\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new NamedThreadFactory("discard-pool"),
                new ThreadPoolExecutor.DiscardPolicy()  // 丢弃策略
        );

        // 提交5个任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("[执行] 任务" + taskId + " - " +
                        Thread.currentThread().getName());
                sleep(2000);
            });
            System.out.println("[提交] 任务" + taskId);
        }

        System.out.println("\n注意: 被拒绝的任务被静默丢弃，不抛异常！");
        executor.shutdown();
    }

    /**
     * Demo 4: DiscardOldestPolicy - 丢弃最老任务
     * 适用场景: 后来的任务优先级更高
     */
    private static void demo4_DiscardOldestPolicy() {
        System.out.println("--- Demo 4: DiscardOldestPolicy（丢弃最老任务） ---");
        System.out.println("适用场景: 后来的任务优先级更高\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),  // 队列容量2
                new NamedThreadFactory("discard-oldest-pool"),
                new ThreadPoolExecutor.DiscardOldestPolicy()  // 丢弃最老任务
        );

        // 提交5个任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("[执行] 任务" + taskId + " - " +
                        Thread.currentThread().getName());
                sleep(2000);
            });
            System.out.println("[提交] 任务" + taskId + " - 队列大小: " +
                    executor.getQueue().size());
            sleep(300);  // 避免提交过快
        }

        System.out.println("\n说明: 任务1在执行，任务2-3在队列，提交任务4时丢弃任务2");
        executor.shutdown();
    }

    /**
     * Demo 5: 自定义拒绝策略
     */
    private static void demo5_CustomPolicy() throws InterruptedException {
        System.out.println("--- Demo 5: 自定义拒绝策略 ---");
        System.out.println("场景: 记录日志、发送告警、存入数据库等\n");

        // 自定义拒绝策略
        RejectedExecutionHandler customHandler = new CustomRejectionHandler();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new NamedThreadFactory("custom-pool"),
                customHandler  // 使用自定义策略
        );

        // 提交5个任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("[执行] 任务" + taskId + " - " +
                        Thread.currentThread().getName());
                sleep(2000);
            });
            System.out.println("[提交] 任务" + taskId);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * 自定义拒绝策略: 记录日志并计数
     */
    static class CustomRejectionHandler implements RejectedExecutionHandler {
        private final AtomicInteger rejectedCount = new AtomicInteger(0);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int count = rejectedCount.incrementAndGet();

            // 1. 记录日志
            System.err.println("\n[自定义拒绝] 任务被拒绝");
            System.err.println("  拒绝原因: 线程池已满");
            System.err.println("  当前线程数: " + executor.getPoolSize());
            System.err.println("  队列大小: " + executor.getQueue().size());
            System.err.println("  拒绝计数: " + count);

            // 2. 可以执行其他操作
            // - 发送告警
            // - 存入数据库
            // - 写入日志文件
            // - 发送到消息队列
            // - 降级处理

            System.err.println("  处理方式: 记录日志并丢弃\n");
        }

        public int getRejectedCount() {
            return rejectedCount.get();
        }
    }

    /**
     * 拒绝策略选择指南
     */
    public static void printPolicyGuide() {
        System.out.println("\n=== 拒绝策略选择指南 ===\n");
        System.out.println("1. AbortPolicy（默认）");
        System.out.println("   - 行为: 抛出RejectedExecutionException");
        System.out.println("   - 适用: 需要明确感知任务被拒绝");
        System.out.println("   - 示例: 关键业务任务\n");

        System.out.println("2. CallerRunsPolicy");
        System.out.println("   - 行为: 由调用线程执行任务");
        System.out.println("   - 适用: 任务不能丢失，提供降级");
        System.out.println("   - 注意: 可能阻塞调用线程\n");

        System.out.println("3. DiscardPolicy");
        System.out.println("   - 行为: 静默丢弃任务");
        System.out.println("   - 适用: 任务允许丢失");
        System.out.println("   - 示例: 日志收集、监控上报\n");

        System.out.println("4. DiscardOldestPolicy");
        System.out.println("   - 行为: 丢弃队列最老的任务");
        System.out.println("   - 适用: 后来的任务更重要");
        System.out.println("   - 示例: 实时数据处理\n");

        System.out.println("5. 自定义策略");
        System.out.println("   - 行为: 自定义逻辑");
        System.out.println("   - 适用: 复杂场景");
        System.out.println("   - 示例: 记录日志、发送告警、存入DB等");
    }

    /**
     * 自定义线程工厂
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
        }
    }

    /**
     * 辅助方法: 休眠
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
