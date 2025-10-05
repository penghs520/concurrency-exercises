package com.concurrency.pool.exercises;

/**
 * 练习3: 带监控的线程池
 *
 * 难度: 🔴 困难 ⭐
 *
 * 需求:
 * 实现一个具备完善监控能力的线程池，支持：
 * 1. 实时监控：线程数、队列大小、任务执行情况
 * 2. 任务执行统计：成功数、失败数、平均执行时间、最慢任务
 * 3. 告警机制：队列积压告警、拒绝任务告警、执行异常告警
 * 4. 性能分析：记录每个任务的执行时间，输出TOP 10最慢任务
 * 5. 优雅关闭：生成运行报告
 *
 * 要求:
 * - 继承ThreadPoolExecutor，重写beforeExecute、afterExecute、terminated方法
 * - 使用ConcurrentHashMap记录任务执行信息
 * - 实现自定义ThreadFactory，给线程命名并设置异常处理器
 * - 告警阈值可配置（队列使用率、拒绝任务数）
 * - 生成详细的运行报告（总任务数、成功率、平均耗时等）
 *
 * 扩展（可选）:
 * - 支持导出监控数据到文件
 * - 支持JMX监控
 * - 集成日志框架
 *
 * 测试:
 * 在main方法中：
 * 1. 创建监控线程池，设置告警阈值
 * 2. 提交100个任务，部分任务会失败，部分耗时较长
 * 3. 观察实时监控输出
 * 4. 触发告警（提交超过容量的任务）
 * 5. 优雅关闭，输出运行报告
 */
public class E03_MonitoredThreadPool {

    /**
     * 任务执行记录
     */
    static class TaskRecord {
        private final String taskId;
        private final long submitTime;    // 提交时间
        private long startTime;           // 开始时间
        private long endTime;             // 结束时间
        private boolean success;          // 是否成功
        private Throwable exception;      // 异常信息

        public TaskRecord(String taskId) {
            this.taskId = taskId;
            this.submitTime = System.currentTimeMillis();
        }

        public long getWaitTime() {
            // TODO: 计算等待时间（开始时间 - 提交时间）
            return 0;
        }

        public long getExecutionTime() {
            // TODO: 计算执行时间（结束时间 - 开始时间）
            return 0;
        }

        // TODO: 添加getter/setter方法
    }

    /**
     * 监控统计数据
     */
    static class MonitorStats {
        private long totalTasks;          // 总任务数
        private long successTasks;        // 成功任务数
        private long failedTasks;         // 失败任务数
        private long rejectedTasks;       // 拒绝任务数
        private long totalExecutionTime;  // 总执行时间
        private long maxExecutionTime;    // 最长执行时间
        private String slowestTask;       // 最慢的任务

        // TODO: 添加方法
        public void recordSuccess(TaskRecord record) {
            // TODO: 记录成功任务
        }

        public void recordFailure(TaskRecord record) {
            // TODO: 记录失败任务
        }

        public void recordRejection() {
            // TODO: 记录拒绝任务
        }

        public double getSuccessRate() {
            // TODO: 计算成功率
            return 0;
        }

        public double getAverageExecutionTime() {
            // TODO: 计算平均执行时间
            return 0;
        }

        @Override
        public String toString() {
            // TODO: 格式化输出统计信息
            return "";
        }
    }

    /**
     * 告警配置
     */
    static class AlarmConfig {
        private double queueUsageThreshold = 0.8;  // 队列使用率告警阈值（80%）
        private int rejectedCountThreshold = 10;   // 拒绝任务数告警阈值
        private long slowTaskThreshold = 5000;     // 慢任务阈值（5秒）

        // TODO: 添加getter/setter方法
    }

    /**
     * 监控线程池
     */
    static class MonitoredThreadPoolExecutor {
        // TODO: 定义字段
        // private ThreadPoolExecutor executor;
        // private MonitorStats stats;
        // private AlarmConfig alarmConfig;
        // private Map<Runnable, TaskRecord> taskRecords;
        // private ScheduledExecutorService monitor; // 定时监控

        /**
         * 构造方法
         */
        public MonitoredThreadPoolExecutor(int corePoolSize,
                                           int maximumPoolSize,
                                           int queueCapacity,
                                           AlarmConfig alarmConfig) {
            // TODO: 初始化线程池
            // 提示: 继承ThreadPoolExecutor或使用组合模式
        }

        /**
         * 提交任务
         */
        public void execute(Runnable task) {
            // TODO: 实现任务提交
            // 提示: 包装任务，添加监控逻辑
        }

        /**
         * 任务执行前钩子
         */
        protected void beforeExecute(Thread t, Runnable r) {
            // TODO: 记录任务开始时间
        }

        /**
         * 任务执行后钩子
         */
        protected void afterExecute(Runnable r, Throwable t) {
            // TODO: 记录任务完成时间、更新统计、检查告警
        }

        /**
         * 线程池终止钩子
         */
        protected void terminated() {
            // TODO: 生成运行报告
        }

        /**
         * 启动实时监控
         */
        private void startMonitoring() {
            // TODO: 启动定时任务，每隔一定时间打印监控信息
        }

        /**
         * 检查告警
         */
        private void checkAlarms() {
            // TODO: 检查队列使用率、拒绝任务数、慢任务等
        }

        /**
         * 发送告警
         */
        private void sendAlarm(String message) {
            // TODO: 输出告警信息
            System.err.println("[告警] " + message);
        }

        /**
         * 获取监控统计
         */
        public MonitorStats getStats() {
            // TODO: 返回统计数据
            return null;
        }

        /**
         * 生成运行报告
         */
        public String generateReport() {
            // TODO: 生成详细的运行报告
            // 包括: 总任务数、成功率、平均耗时、最慢任务等
            return "";
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
     * 测试方法
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== 监控线程池测试 ===\n");

        // TODO: 1. 创建监控线程池
        // AlarmConfig config = new AlarmConfig();
        // config.setQueueUsageThreshold(0.7);
        // config.setRejectedCountThreshold(5);
        //
        // MonitoredThreadPoolExecutor executor = new MonitoredThreadPoolExecutor(
        //     5, 10, 20, config
        // );

        // TODO: 2. 提交正常任务
        // for (int i = 1; i <= 50; i++) {
        //     final int taskId = i;
        //     executor.execute(() -> {
        //         try {
        //             Thread.sleep(100 + (long)(Math.random() * 200));
        //         } catch (InterruptedException e) {
        //             Thread.currentThread().interrupt();
        //         }
        //     });
        // }

        // TODO: 3. 提交慢任务
        // for (int i = 51; i <= 60; i++) {
        //     executor.execute(() -> {
        //         try {
        //             Thread.sleep(6000); // 超过慢任务阈值
        //         } catch (InterruptedException e) {
        //             Thread.currentThread().interrupt();
        //         }
        //     });
        // }

        // TODO: 4. 提交失败任务
        // for (int i = 61; i <= 70; i++) {
        //     executor.execute(() -> {
        //         throw new RuntimeException("模拟任务失败");
        //     });
        // }

        // TODO: 5. 提交超过容量的任务，触发拒绝告警
        // for (int i = 71; i <= 100; i++) {
        //     try {
        //         executor.execute(() -> {
        //             Thread.sleep(1000);
        //         });
        //     } catch (RejectedExecutionException e) {
        //         // 拒绝任务
        //     }
        // }

        // TODO: 6. 等待任务完成
        // Thread.sleep(10000);

        // TODO: 7. 打印统计信息
        // System.out.println("\n统计信息:");
        // System.out.println(executor.getStats());

        // TODO: 8. 优雅关闭，生成报告
        // executor.shutdown();
        // executor.awaitTermination(30, TimeUnit.SECONDS);
        //
        // System.out.println("\n运行报告:");
        // System.out.println(executor.generateReport());

        System.out.println("\n任务: 实现带监控和告警的线程池");
        System.out.println("提示: 查看solutions/E03_MonitoredThreadPool.java 了解参考实现");
    }
}
