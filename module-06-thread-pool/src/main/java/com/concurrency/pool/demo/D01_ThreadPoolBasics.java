package com.concurrency.pool.demo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo 01: 线程池基础
 *
 * 本示例演示：
 * 1. 手动创建ThreadPoolExecutor
 * 2. 核心参数的作用
 * 3. 任务执行流程
 * 4. 线程池监控
 */
public class D01_ThreadPoolBasics {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 线程池基础演示 ===\n");

        // Demo 1: 基本创建与使用
        demo1_BasicUsage();
        Thread.sleep(3000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 2: 理解核心参数
        demo2_CoreParameters();
        Thread.sleep(5000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 3: 线程池监控
        demo3_Monitoring();

        System.out.println("\n主线程执行完毕");
    }

    /**
     * Demo 1: 基本创建与使用
     */
    private static void demo1_BasicUsage() {
        System.out.println("--- Demo 1: 基本创建与使用 ---\n");

        // 手动创建线程池（推荐方式）
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                          // corePoolSize: 核心线程数
                4,                          // maximumPoolSize: 最大线程数
                60L,                        // keepAliveTime: 空闲线程存活时间
                TimeUnit.SECONDS,           // unit: 时间单位
                new ArrayBlockingQueue<>(2), // workQueue: 工作队列（容量2）
                new NamedThreadFactory("demo1-pool"), // threadFactory: 线程工厂
                new ThreadPoolExecutor.AbortPolicy()  // handler: 拒绝策略
        );

        // 提交5个任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("任务" + taskId + " 开始执行，线程: " +
                        Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("任务" + taskId + " 执行完成");
            });
            System.out.println("提交任务" + taskId + " - 当前线程数: " +
                    executor.getPoolSize() + ", 队列大小: " + executor.getQueue().size());
        }

        // 优雅关闭
        executor.shutdown();
    }

    /**
     * Demo 2: 理解核心参数
     * 演示任务提交时线程池的决策流程
     */
    private static void demo2_CoreParameters() throws InterruptedException {
        System.out.println("--- Demo 2: 理解核心参数 ---");
        System.out.println("配置: 核心线程2, 最大线程5, 队列容量3\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,  // 核心线程数
                5,  // 最大线程数
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3),  // 队列容量3
                new NamedThreadFactory("demo2-pool")
        );

        // 提交10个任务，观察执行流程
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;

            try {
                executor.execute(() -> {
                    System.out.println("[执行] 任务" + taskId + " - " +
                            Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000);  // 模拟耗时操作
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                // 打印当前状态
                System.out.printf("提交任务%-2d -> 线程数: %d/%d, 活动: %d, 队列: %d%n",
                        taskId,
                        executor.getPoolSize(),
                        executor.getMaximumPoolSize(),
                        executor.getActiveCount(),
                        executor.getQueue().size());

                Thread.sleep(200);  // 避免提交过快

            } catch (RejectedExecutionException e) {
                System.err.println("[拒绝] 任务" + taskId + " 被拒绝执行！");
            }
        }

        System.out.println("\n执行流程说明:");
        System.out.println("任务1-2: 创建2个核心线程执行");
        System.out.println("任务3-5: 核心线程忙，放入队列（队列容量3）");
        System.out.println("任务6-8: 队列满，创建3个非核心线程执行（总线程数达到5）");
        System.out.println("任务9-10: 线程数已达上限，执行拒绝策略");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Demo 3: 线程池监控
     */
    private static void demo3_Monitoring() throws InterruptedException {
        System.out.println("--- Demo 3: 线程池监控 ---\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                3, 10, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new NamedThreadFactory("monitor-pool")
        );

        // 提交20个任务
        for (int i = 1; i <= 20; i++) {
            final int taskId = i;
            executor.execute(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 监控线程池状态
        System.out.println("线程池监控指标:");
        System.out.println("-".repeat(60));

        for (int i = 0; i < 5; i++) {
            printPoolStatus(executor);
            Thread.sleep(500);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n最终统计:");
        printPoolStatus(executor);
    }

    /**
     * 打印线程池状态
     */
    private static void printPoolStatus(ThreadPoolExecutor executor) {
        System.out.printf("核心线程数: %d | 最大线程数: %d | 当前线程数: %d | 活动线程数: %d%n",
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getPoolSize(),
                executor.getActiveCount());

        System.out.printf("队列容量: %d | 队列大小: %d | 总任务数: %d | 已完成: %d%n",
                ((LinkedBlockingQueue<?>) executor.getQueue()).remainingCapacity() +
                        executor.getQueue().size(),
                executor.getQueue().size(),
                executor.getTaskCount(),
                executor.getCompletedTaskCount());

        // 计算指标
        double threadUtilization = executor.getPoolSize() > 0 ?
                (double) executor.getActiveCount() / executor.getPoolSize() * 100 : 0;
        System.out.printf("线程利用率: %.2f%%%n", threadUtilization);
        System.out.println("-".repeat(60));
    }

    /**
     * 自定义线程工厂 - 给线程命名
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(namePrefix + "-" + threadNumber.getAndIncrement());
            // 设置为非守护线程
            t.setDaemon(false);
            // 设置优先级
            t.setPriority(Thread.NORM_PRIORITY);

            // 设置未捕获异常处理器
            t.setUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("线程 " + thread.getName() + " 发生异常:");
                throwable.printStackTrace();
            });

            return t;
        }
    }
}
