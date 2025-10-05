package com.concurrency.pool.exercises;

/**
 * 练习2: 自定义任务调度器
 *
 * 难度: 🟡 中等
 *
 * 需求:
 * 实现一个支持优先级的任务调度器，具备以下功能：
 * 1. 支持任务优先级（HIGH, MEDIUM, LOW）
 * 2. 高优先级任务先执行
 * 3. 相同优先级按提交顺序执行
 * 4. 支持延迟任务（在指定时间后执行）
 * 5. 支持取消任务
 * 6. 提供任务状态查询（等待、执行中、已完成、已取消）
 *
 * 要求:
 * - 使用PriorityBlockingQueue实现优先级队列
 * - 任务需要实现Comparable接口以支持排序
 * - 延迟任务到期后加入优先级队列
 * - 支持优雅关闭，等待所有任务完成
 *
 * 测试:
 * 在main方法中：
 * 1. 提交不同优先级的任务（高、中、低各若干）
 * 2. 提交延迟任务（2秒后执行）
 * 3. 观察执行顺序是否符合优先级
 * 4. 测试任务取消功能
 * 5. 查询任务状态
 */
public class E02_TaskScheduler {

    /**
     * 任务优先级枚举
     */
    enum Priority {
        HIGH(1),
        MEDIUM(2),
        LOW(3);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 任务状态枚举
     */
    enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        CANCELLED   // 已取消
    }

    /**
     * 可调度的任务
     */
    static class ScheduledTask implements Comparable<ScheduledTask> {
        private final String taskId;
        private final Runnable task;
        private final Priority priority;
        private final long submitTime;      // 提交时间
        private final long executeTime;     // 执行时间（用于延迟任务）
        private volatile TaskStatus status;

        // TODO: 实现构造方法

        /**
         * 立即执行的任务
         */
        public ScheduledTask(String taskId, Runnable task, Priority priority) {
            // TODO: 实现
            this.taskId = taskId;
            this.task = task;
            this.priority = priority;
            this.submitTime = System.currentTimeMillis();
            this.executeTime = submitTime;
            this.status = TaskStatus.PENDING;
        }

        /**
         * 延迟执行的任务
         */
        public ScheduledTask(String taskId, Runnable task, Priority priority, long delayMillis) {
            // TODO: 实现
            this.taskId = taskId;
            this.task = task;
            this.priority = priority;
            this.submitTime = System.currentTimeMillis();
            this.executeTime = submitTime + delayMillis;
            this.status = TaskStatus.PENDING;
        }

        /**
         * 是否到达执行时间
         */
        public boolean isReadyToExecute() {
            // TODO: 实现
            return false;
        }

        /**
         * 比较方法：优先级高的排前面，相同优先级按提交时间排序
         */
        @Override
        public int compareTo(ScheduledTask other) {
            // TODO: 实现优先级比较
            // 提示: 先比较优先级，再比较提交时间
            return 0;
        }

        // TODO: 添加getter/setter方法
    }

    /**
     * 优先级任务调度器
     */
    static class PriorityTaskScheduler {
        // TODO: 定义字段
        // private ThreadPoolExecutor executor;
        // private PriorityBlockingQueue<ScheduledTask> taskQueue;
        // private ScheduledExecutorService delayChecker; // 检查延迟任务
        // private Map<String, ScheduledTask> taskMap;    // 任务映射

        /**
         * 构造方法
         */
        public PriorityTaskScheduler(int threadCount) {
            // TODO: 初始化线程池和队列
            // 提示: 使用PriorityBlockingQueue作为工作队列
        }

        /**
         * 提交立即执行的任务
         */
        public String submit(Runnable task, Priority priority) {
            // TODO: 实现任务提交
            // 生成任务ID，创建ScheduledTask，加入队列
            return null;
        }

        /**
         * 提交延迟任务
         */
        public String submitDelayed(Runnable task, Priority priority, long delayMillis) {
            // TODO: 实现延迟任务提交
            // 提示: 使用单独的定时器检查延迟任务是否到期
            return null;
        }

        /**
         * 取消任务
         */
        public boolean cancelTask(String taskId) {
            // TODO: 实现任务取消
            // 提示: 只能取消未执行的任务
            return false;
        }

        /**
         * 查询任务状态
         */
        public TaskStatus getTaskStatus(String taskId) {
            // TODO: 实现状态查询
            return null;
        }

        /**
         * 获取等待任务数
         */
        public int getPendingTaskCount() {
            // TODO: 实现
            return 0;
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
        System.out.println("=== 优先级任务调度器测试 ===\n");

        // TODO: 1. 创建调度器
        // PriorityTaskScheduler scheduler = new PriorityTaskScheduler(3);

        // TODO: 2. 提交不同优先级的任务
        // System.out.println("提交任务:");
        // String task1 = scheduler.submit(() -> {
        //     System.out.println("低优先级任务1执行");
        //     Thread.sleep(1000);
        // }, Priority.LOW);
        //
        // String task2 = scheduler.submit(() -> {
        //     System.out.println("高优先级任务1执行");
        //     Thread.sleep(1000);
        // }, Priority.HIGH);
        //
        // String task3 = scheduler.submit(() -> {
        //     System.out.println("中优先级任务1执行");
        //     Thread.sleep(1000);
        // }, Priority.MEDIUM);

        // TODO: 3. 提交延迟任务
        // String delayedTask = scheduler.submitDelayed(() -> {
        //     System.out.println("延迟任务执行（2秒后）");
        // }, Priority.HIGH, 2000);

        // TODO: 4. 查询任务状态
        // Thread.sleep(500);
        // System.out.println("task1 状态: " + scheduler.getTaskStatus(task1));

        // TODO: 5. 测试取消任务
        // boolean cancelled = scheduler.cancelTask(task3);
        // System.out.println("取消task3: " + cancelled);

        // TODO: 6. 等待所有任务完成
        // scheduler.shutdown();
        // scheduler.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n任务: 实现支持优先级的任务调度器");
        System.out.println("提示: 查看solutions/E02_TaskScheduler.java 了解参考实现");
    }
}
