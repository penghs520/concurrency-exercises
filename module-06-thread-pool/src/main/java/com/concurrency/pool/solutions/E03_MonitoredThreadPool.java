package com.concurrency.pool.solutions;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 练习3解答: 带监控的线程池
 *
 * 实现要点:
 * 1. 继承ThreadPoolExecutor，重写钩子方法
 * 2. 使用ConcurrentHashMap记录任务执行信息
 * 3. 实现告警机制
 * 4. 生成详细的运行报告
 */
public class E03_MonitoredThreadPool {

    /**
     * 任务执行记录
     */
    static class TaskRecord {
        private final String taskId;
        private final long submitTime;
        private long startTime;
        private long endTime;
        private boolean success;
        private Throwable exception;
        private String threadName;

        public TaskRecord(String taskId) {
            this.taskId = taskId;
            this.submitTime = System.currentTimeMillis();
        }

        public long getWaitTime() {
            return startTime > 0 ? startTime - submitTime : 0;
        }

        public long getExecutionTime() {
            return endTime > 0 ? endTime - startTime : 0;
        }

        // Getters and setters
        public String getTaskId() { return taskId; }
        public long getSubmitTime() { return submitTime; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public Throwable getException() { return exception; }
        public void setException(Throwable exception) { this.exception = exception; }
        public String getThreadName() { return threadName; }
        public void setThreadName(String threadName) { this.threadName = threadName; }
    }

    /**
     * 监控统计数据
     */
    static class MonitorStats {
        private final AtomicLong totalTasks = new AtomicLong(0);
        private final AtomicLong successTasks = new AtomicLong(0);
        private final AtomicLong failedTasks = new AtomicLong(0);
        private final AtomicLong rejectedTasks = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private volatile String slowestTask;

        public void recordSuccess(TaskRecord record) {
            totalTasks.incrementAndGet();
            successTasks.incrementAndGet();
            long execTime = record.getExecutionTime();
            totalExecutionTime.addAndGet(execTime);

            // 更新最慢任务
            if (execTime > maxExecutionTime.get()) {
                maxExecutionTime.set(execTime);
                slowestTask = record.getTaskId();
            }
        }

        public void recordFailure(TaskRecord record) {
            totalTasks.incrementAndGet();
            failedTasks.incrementAndGet();
            long execTime = record.getExecutionTime();
            totalExecutionTime.addAndGet(execTime);
        }

        public void recordRejection() {
            rejectedTasks.incrementAndGet();
        }

        public double getSuccessRate() {
            long total = totalTasks.get();
            return total > 0 ? (double) successTasks.get() / total * 100 : 0;
        }

        public double getAverageExecutionTime() {
            long total = totalTasks.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0;
        }

        @Override
        public String toString() {
            return String.format(
                    "总任务: %d | 成功: %d | 失败: %d | 拒绝: %d | 成功率: %.2f%% | 平均耗时: %.2fms",
                    totalTasks.get(), successTasks.get(), failedTasks.get(),
                    rejectedTasks.get(), getSuccessRate(), getAverageExecutionTime());
        }

        // Getters
        public long getTotalTasks() { return totalTasks.get(); }
        public long getSuccessTasks() { return successTasks.get(); }
        public long getFailedTasks() { return failedTasks.get(); }
        public long getRejectedTasks() { return rejectedTasks.get(); }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public String getSlowestTask() { return slowestTask; }
    }

    /**
     * 告警配置
     */
    static class AlarmConfig {
        private double queueUsageThreshold = 0.8;
        private int rejectedCountThreshold = 10;
        private long slowTaskThreshold = 5000;

        public double getQueueUsageThreshold() { return queueUsageThreshold; }
        public void setQueueUsageThreshold(double queueUsageThreshold) {
            this.queueUsageThreshold = queueUsageThreshold;
        }
        public int getRejectedCountThreshold() { return rejectedCountThreshold; }
        public void setRejectedCountThreshold(int rejectedCountThreshold) {
            this.rejectedCountThreshold = rejectedCountThreshold;
        }
        public long getSlowTaskThreshold() { return slowTaskThreshold; }
        public void setSlowTaskThreshold(long slowTaskThreshold) {
            this.slowTaskThreshold = slowTaskThreshold;
        }
    }

    /**
     * 监控线程池
     */
    static class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {
        private final MonitorStats stats;
        private final AlarmConfig alarmConfig;
        private final ConcurrentHashMap<Runnable, TaskRecord> taskRecords;
        private final ScheduledExecutorService monitor;
        private final AtomicLong taskIdGenerator;
        private final int queueCapacity;

        public MonitoredThreadPoolExecutor(int corePoolSize,
                                           int maximumPoolSize,
                                           int queueCapacity,
                                           AlarmConfig alarmConfig) {
            super(corePoolSize, maximumPoolSize, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    new MonitoredThreadFactory("monitored-pool"),
                    new MonitoredRejectionHandler());

            this.queueCapacity = queueCapacity;
            this.stats = new MonitorStats();
            this.alarmConfig = alarmConfig;
            this.taskRecords = new ConcurrentHashMap<>();
            this.taskIdGenerator = new AtomicLong(1);

            // 设置拒绝处理器
            ((MonitoredRejectionHandler) getRejectedExecutionHandler()).setStats(stats);

            // 启动实时监控
            this.monitor = Executors.newSingleThreadScheduledExecutor(
                    new MonitoredThreadFactory("monitor"));
            startMonitoring();
        }

        @Override
        public void execute(Runnable command) {
            // 包装任务以添加监控
            MonitoredTask task = new MonitoredTask(
                    "task-" + taskIdGenerator.getAndIncrement(), command);
            TaskRecord record = new TaskRecord(task.getTaskId());
            taskRecords.put(task, record);

            super.execute(task);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);

            if (r instanceof MonitoredTask) {
                MonitoredTask task = (MonitoredTask) r;
                TaskRecord record = taskRecords.get(task);
                if (record != null) {
                    record.setStartTime(System.currentTimeMillis());
                    record.setThreadName(t.getName());
                }
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            if (r instanceof MonitoredTask) {
                MonitoredTask task = (MonitoredTask) r;
                TaskRecord record = taskRecords.get(task);
                if (record != null) {
                    record.setEndTime(System.currentTimeMillis());

                    if (t == null) {
                        record.setSuccess(true);
                        stats.recordSuccess(record);
                    } else {
                        record.setSuccess(false);
                        record.setException(t);
                        stats.recordFailure(record);
                        sendAlarm("任务执行异常: " + task.getTaskId() + " - " + t.getMessage());
                    }

                    // 检查慢任务
                    if (record.getExecutionTime() > alarmConfig.getSlowTaskThreshold()) {
                        sendAlarm("慢任务告警: " + task.getTaskId() +
                                " 耗时 " + record.getExecutionTime() + "ms");
                    }

                    // 检查其他告警
                    checkAlarms();
                }
            }
        }

        @Override
        protected void terminated() {
            super.terminated();
            monitor.shutdown();
            System.out.println("\n[线程池终止]");
            System.out.println(generateReport());
        }

        /**
         * 启动实时监控
         */
        private void startMonitoring() {
            monitor.scheduleAtFixedRate(() -> {
                try {
                    System.out.printf("[监控] 线程: %d/%d | 活动: %d | 队列: %d | %s%n",
                            getPoolSize(), getMaximumPoolSize(),
                            getActiveCount(), getQueue().size(),
                            stats.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 2, 2, TimeUnit.SECONDS);
        }

        /**
         * 检查告警
         */
        private void checkAlarms() {
            // 检查队列使用率
            double queueUsage = (double) getQueue().size() / queueCapacity;
            if (queueUsage >= alarmConfig.getQueueUsageThreshold()) {
                sendAlarm(String.format("队列积压告警: 使用率 %.2f%%", queueUsage * 100));
            }

            // 检查拒绝任务数
            if (stats.getRejectedTasks() >= alarmConfig.getRejectedCountThreshold()) {
                sendAlarm("拒绝任务数过多: " + stats.getRejectedTasks());
            }
        }

        /**
         * 发送告警
         */
        private void sendAlarm(String message) {
            System.err.println("[告警] " + message);
        }

        /**
         * 获取监控统计
         */
        public MonitorStats getStats() {
            return stats;
        }

        /**
         * 生成运行报告
         */
        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("\n").append("=".repeat(70)).append("\n");
            report.append("                        线程池运行报告\n");
            report.append("=".repeat(70)).append("\n");

            // 基本信息
            report.append("配置信息:\n");
            report.append(String.format("  核心线程数: %d | 最大线程数: %d | 队列容量: %d%n",
                    getCorePoolSize(), getMaximumPoolSize(), queueCapacity));

            // 统计信息
            report.append("\n统计信息:\n");
            report.append(String.format("  总任务数: %d%n", stats.getTotalTasks()));
            report.append(String.format("  成功任务: %d (%.2f%%)%n",
                    stats.getSuccessTasks(), stats.getSuccessRate()));
            report.append(String.format("  失败任务: %d%n", stats.getFailedTasks()));
            report.append(String.format("  拒绝任务: %d%n", stats.getRejectedTasks()));
            report.append(String.format("  已完成任务: %d%n", getCompletedTaskCount()));

            // 性能信息
            report.append("\n性能信息:\n");
            report.append(String.format("  平均执行时间: %.2f ms%n", stats.getAverageExecutionTime()));
            report.append(String.format("  最长执行时间: %d ms%n", stats.getMaxExecutionTime()));
            report.append(String.format("  最慢任务: %s%n",
                    stats.getSlowestTask() != null ? stats.getSlowestTask() : "N/A"));

            // TOP 10 最慢任务
            report.append("\nTOP 10 最慢任务:\n");
            taskRecords.values().stream()
                    .filter(r -> r.getEndTime() > 0)
                    .sorted((r1, r2) -> Long.compare(r2.getExecutionTime(), r1.getExecutionTime()))
                    .limit(10)
                    .forEach(r -> report.append(String.format("  %s: %d ms (%s)%n",
                            r.getTaskId(), r.getExecutionTime(),
                            r.isSuccess() ? "成功" : "失败")));

            report.append("=".repeat(70)).append("\n");
            return report.toString();
        }

        /**
         * 监控任务包装类
         */
        private static class MonitoredTask implements Runnable {
            private final String taskId;
            private final Runnable task;

            public MonitoredTask(String taskId, Runnable task) {
                this.taskId = taskId;
                this.task = task;
            }

            @Override
            public void run() {
                task.run();
            }

            public String getTaskId() {
                return taskId;
            }
        }

        /**
         * 监控拒绝处理器
         */
        private static class MonitoredRejectionHandler implements RejectedExecutionHandler {
            private MonitorStats stats;

            public void setStats(MonitorStats stats) {
                this.stats = stats;
            }

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (stats != null) {
                    stats.recordRejection();
                }
                // 使用CallerRunsPolicy策略
                if (!executor.isShutdown()) {
                    r.run();
                }
            }
        }
    }

    /**
     * 监控线程工厂
     */
    static class MonitoredThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public MonitoredThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("[异常] 线程 " + thread.getName() + " 发生未捕获异常:");
                throwable.printStackTrace();
            });
            return t;
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== 监控线程池测试 ===\n");

        // 1. 创建监控线程池
        AlarmConfig config = new AlarmConfig();
        config.setQueueUsageThreshold(0.7);
        config.setRejectedCountThreshold(5);
        config.setSlowTaskThreshold(3000);

        MonitoredThreadPoolExecutor executor = new MonitoredThreadPoolExecutor(
                5, 10, 20, config);

        System.out.println("线程池配置: 核心5, 最大10, 队列20\n");

        // 2. 提交正常任务
        System.out.println("提交正常任务...\n");
        for (int i = 1; i <= 30; i++) {
            final int taskId = i;
            executor.execute(() -> {
                try {
                    Thread.sleep(100 + (long) (Math.random() * 200));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(3000);

        // 3. 提交慢任务
        System.out.println("\n提交慢任务...\n");
        for (int i = 31; i <= 35; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(3000);

        // 4. 提交失败任务
        System.out.println("\n提交失败任务...\n");
        for (int i = 36; i <= 40; i++) {
            final int taskId = i;
            executor.execute(() -> {
                throw new RuntimeException("模拟任务" + taskId + "失败");
            });
        }

        Thread.sleep(3000);

        // 5. 提交大量任务触发告警
        System.out.println("\n提交大量任务...\n");
        for (int i = 41; i <= 60; i++) {
            try {
                executor.execute(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                // 忽略拒绝异常
            }
        }

        // 6. 等待任务完成
        Thread.sleep(10000);

        // 7. 打印统计信息
        System.out.println("\n当前统计:");
        System.out.println(executor.getStats());

        // 8. 优雅关闭
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("\n测试完成");
    }
}
