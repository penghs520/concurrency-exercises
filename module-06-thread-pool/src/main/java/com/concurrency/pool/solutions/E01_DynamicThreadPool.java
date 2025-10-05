package com.concurrency.pool.solutions;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 练习1解答: 动态可调整线程池
 *
 * 实现要点:
 * 1. 封装ThreadPoolExecutor，提供动态调整接口
 * 2. 参数调整时进行合法性校验
 * 3. 提供详细的监控指标
 * 4. 实现优雅关闭
 */
public class E01_DynamicThreadPool {

    /**
     * 动态线程池实现
     */
    static class DynamicThreadPoolExecutor {
        private ThreadPoolExecutor executor;
        private final int queueCapacity;

        /**
         * 构造方法
         */
        public DynamicThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int queueCapacity) {
            if (corePoolSize <= 0 || maximumPoolSize <= 0 || queueCapacity <= 0) {
                throw new IllegalArgumentException("参数必须大于0");
            }
            if (maximumPoolSize < corePoolSize) {
                throw new IllegalArgumentException("最大线程数必须 >= 核心线程数");
            }

            this.queueCapacity = queueCapacity;

            // 创建线程池
            this.executor = new ThreadPoolExecutor(
                    corePoolSize,
                    maximumPoolSize,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    new NamedThreadFactory("dynamic-pool"),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }

        /**
         * 提交任务
         */
        public void execute(Runnable task) {
            executor.execute(task);
        }

        /**
         * 动态调整核心线程数
         */
        public void setCorePoolSize(int corePoolSize) {
            if (corePoolSize <= 0) {
                throw new IllegalArgumentException("核心线程数必须 > 0");
            }
            if (corePoolSize > executor.getMaximumPoolSize()) {
                throw new IllegalArgumentException(
                        "核心线程数不能大于最大线程数: " + executor.getMaximumPoolSize());
            }

            int oldValue = executor.getCorePoolSize();
            executor.setCorePoolSize(corePoolSize);

            System.out.println("[配置调整] 核心线程数: " + oldValue + " -> " + corePoolSize);

            // 如果减小核心线程数，空闲线程会被回收
            if (corePoolSize < oldValue) {
                System.out.println("  提示: 空闲线程将在60秒后被回收");
            }
        }

        /**
         * 动态调整最大线程数
         */
        public void setMaximumPoolSize(int maximumPoolSize) {
            if (maximumPoolSize <= 0) {
                throw new IllegalArgumentException("最大线程数必须 > 0");
            }
            if (maximumPoolSize < executor.getCorePoolSize()) {
                throw new IllegalArgumentException(
                        "最大线程数不能小于核心线程数: " + executor.getCorePoolSize());
            }

            int oldValue = executor.getMaximumPoolSize();
            executor.setMaximumPoolSize(maximumPoolSize);

            System.out.println("[配置调整] 最大线程数: " + oldValue + " -> " + maximumPoolSize);
        }

        /**
         * 动态调整空闲线程存活时间
         */
        public void setKeepAliveTime(long time, TimeUnit unit) {
            executor.setKeepAliveTime(time, unit);
            System.out.println("[配置调整] 空闲线程存活时间: " + time + " " + unit);
        }

        /**
         * 获取监控信息
         */
        public PoolMetrics getMetrics() {
            return new PoolMetrics(
                    executor.getCorePoolSize(),
                    executor.getMaximumPoolSize(),
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    queueCapacity,
                    executor.getCompletedTaskCount(),
                    executor.getTaskCount()
            );
        }

        /**
         * 优雅关闭
         */
        public void shutdown() {
            System.out.println("\n[关闭] 线程池开始关闭...");
            executor.shutdown();
        }

        /**
         * 等待任务完成
         */
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            boolean terminated = executor.awaitTermination(timeout, unit);
            if (terminated) {
                System.out.println("[关闭] 所有任务已完成");
            } else {
                System.out.println("[关闭] 等待超时，强制关闭");
                executor.shutdownNow();
            }
            return terminated;
        }
    }

    /**
     * 监控指标类
     */
    static class PoolMetrics {
        private final int corePoolSize;
        private final int maximumPoolSize;
        private final int poolSize;
        private final int activeCount;
        private final int queueSize;
        private final int queueCapacity;
        private final long completedTaskCount;
        private final long taskCount;

        public PoolMetrics(int corePoolSize, int maximumPoolSize, int poolSize,
                           int activeCount, int queueSize, int queueCapacity,
                           long completedTaskCount, long taskCount) {
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.poolSize = poolSize;
            this.activeCount = activeCount;
            this.queueSize = queueSize;
            this.queueCapacity = queueCapacity;
            this.completedTaskCount = completedTaskCount;
            this.taskCount = taskCount;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n线程池监控指标:\n");
            sb.append("=".repeat(60)).append("\n");
            sb.append(String.format("核心线程数: %d | 最大线程数: %d%n", corePoolSize, maximumPoolSize));
            sb.append(String.format("当前线程数: %d | 活动线程数: %d%n", poolSize, activeCount));
            sb.append(String.format("队列容量: %d | 队列大小: %d%n", queueCapacity, queueSize));
            sb.append(String.format("总任务数: %d | 已完成: %d%n", taskCount, completedTaskCount));

            // 计算利用率
            double threadUtilization = poolSize > 0 ? (double) activeCount / poolSize * 100 : 0;
            double queueUtilization = queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;

            sb.append(String.format("线程利用率: %.2f%% | 队列利用率: %.2f%%%n",
                    threadUtilization, queueUtilization));
            sb.append("=".repeat(60));

            return sb.toString();
        }
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
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== 动态线程池测试 ===\n");

        // 1. 创建动态线程池
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(5, 10, 100);
        System.out.println("创建线程池: 核心5, 最大10, 队列100\n");

        // 2. 提交20个任务
        System.out.println("提交20个任务...");
        for (int i = 1; i <= 20; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("  任务" + taskId + " 执行 - " + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 3. 打印初始状态
        Thread.sleep(500);
        System.out.println(executor.getMetrics());

        // 4. 动态调整核心线程数
        Thread.sleep(2000);
        executor.setCorePoolSize(10);
        System.out.println(executor.getMetrics());

        // 5. 再提交20个任务
        System.out.println("\n提交20个任务...");
        for (int i = 21; i <= 40; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("  任务" + taskId + " 执行 - " + Thread.currentThread().getName());
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 6. 观察线程数变化
        Thread.sleep(1000);
        System.out.println(executor.getMetrics());

        // 7. 测试调整最大线程数
        executor.setMaximumPoolSize(15);

        // 8. 等待一段时间
        Thread.sleep(3000);
        System.out.println(executor.getMetrics());

        // 9. 优雅关闭
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println(executor.getMetrics());
        System.out.println("\n测试完成");
    }
}
