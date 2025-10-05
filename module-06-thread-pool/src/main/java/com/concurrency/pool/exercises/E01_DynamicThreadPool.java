package com.concurrency.pool.exercises;

/**
 * 练习1: 动态可调整线程池
 *
 * 难度: 🟢 简单
 *
 * 需求:
 * 实现一个可以动态调整参数的线程池，支持：
 * 1. 运行时动态调整核心线程数
 * 2. 运行时动态调整最大线程数
 * 3. 运行时动态调整队列容量（提示：需要重新创建线程池）
 * 4. 提供线程池监控方法，返回当前状态
 * 5. 支持优雅关闭
 *
 * 要求:
 * - 参数调整需要进行合法性校验
 * - 调整核心线程数时，如果小于当前值，空闲线程会被回收
 * - 调整最大线程数时，确保 >= 核心线程数
 * - 提供监控指标：线程数、队列大小、完成任务数等
 *
 * 测试:
 * 在main方法中：
 * 1. 创建动态线程池，初始配置: 核心5，最大10，队列100
 * 2. 提交20个任务
 * 3. 运行中调整核心线程数为10
 * 4. 再提交20个任务
 * 5. 打印监控指标
 * 6. 优雅关闭
 */
public class E01_DynamicThreadPool {

    // TODO: 实现DynamicThreadPoolExecutor类
    // 提示: 可以继承ThreadPoolExecutor或封装ThreadPoolExecutor

    /**
     * 动态线程池实现
     */
    static class DynamicThreadPoolExecutor {
        // TODO: 定义字段
        // private ThreadPoolExecutor executor;

        /**
         * 构造方法
         * @param corePoolSize 核心线程数
         * @param maximumPoolSize 最大线程数
         * @param queueCapacity 队列容量
         */
        public DynamicThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int queueCapacity) {
            // TODO: 初始化线程池
        }

        /**
         * 提交任务
         */
        public void execute(Runnable task) {
            // TODO: 实现任务提交
        }

        /**
         * 动态调整核心线程数
         */
        public void setCorePoolSize(int corePoolSize) {
            // TODO: 实现核心线程数调整
            // 提示: ThreadPoolExecutor提供了setCorePoolSize方法
        }

        /**
         * 动态调整最大线程数
         */
        public void setMaximumPoolSize(int maximumPoolSize) {
            // TODO: 实现最大线程数调整
            // 提示: 确保 maximumPoolSize >= corePoolSize
        }

        /**
         * 获取监控信息
         */
        public PoolMetrics getMetrics() {
            // TODO: 实现监控信息收集
            return null;
        }

        /**
         * 优雅关闭
         */
        public void shutdown() {
            // TODO: 实现优雅关闭
        }

        /**
         * 等待任务完成
         */
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
            // TODO: 实现等待逻辑
            return false;
        }
    }

    /**
     * 监控指标类
     */
    static class PoolMetrics {
        private int corePoolSize;
        private int maximumPoolSize;
        private int poolSize;           // 当前线程数
        private int activeCount;        // 活动线程数
        private int queueSize;          // 队列中的任务数
        private long completedTaskCount; // 已完成任务数
        private long taskCount;         // 总任务数

        // TODO: 添加构造方法、getter、toString等

        @Override
        public String toString() {
            // TODO: 格式化输出监控信息
            return "";
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== 动态线程池测试 ===\n");

        // TODO: 1. 创建动态线程池
        // DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(5, 10, 100);

        // TODO: 2. 提交20个任务
        // for (int i = 1; i <= 20; i++) {
        //     final int taskId = i;
        //     executor.execute(() -> {
        //         System.out.println("任务" + taskId + " 执行");
        //         Thread.sleep(1000);
        //     });
        // }

        // TODO: 3. 打印初始状态
        // Thread.sleep(500);
        // System.out.println("初始状态: " + executor.getMetrics());

        // TODO: 4. 动态调整核心线程数
        // executor.setCorePoolSize(10);
        // System.out.println("调整核心线程数为10");

        // TODO: 5. 再提交20个任务
        // for (int i = 21; i <= 40; i++) { ... }

        // TODO: 6. 打印最终状态
        // Thread.sleep(2000);
        // System.out.println("最终状态: " + executor.getMetrics());

        // TODO: 7. 优雅关闭
        // executor.shutdown();
        // executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n任务: 实现动态可调整的线程池");
        System.out.println("提示: 查看solutions/E01_DynamicThreadPool.java 了解参考实现");
    }
}
