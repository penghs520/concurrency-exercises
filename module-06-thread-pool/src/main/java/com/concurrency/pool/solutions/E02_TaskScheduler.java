package com.concurrency.pool.solutions;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 练习2解答: 自定义任务调度器
 *
 * 实现要点:
 * 1. 使用PriorityBlockingQueue实现优先级队列
 * 2. 任务实现Comparable接口支持排序
 * 3. 使用ScheduledExecutorService处理延迟任务
 * 4. 使用ConcurrentHashMap管理任务状态
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
    static class ScheduledTask implements Comparable<ScheduledTask>, Runnable {
        private final String taskId;
        private final Runnable task;
        private final Priority priority;
        private final long submitTime;
        private final long executeTime;
        private volatile TaskStatus status;

        /**
         * 立即执行的任务
         */
        public ScheduledTask(String taskId, Runnable task, Priority priority) {
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
            return System.currentTimeMillis() >= executeTime;
        }

        @Override
        public void run() {
            if (status == TaskStatus.CANCELLED) {
                return;
            }
            status = TaskStatus.RUNNING;
            try {
                task.run();
                status = TaskStatus.COMPLETED;
            } catch (Exception e) {
                status = TaskStatus.COMPLETED;
                throw e;
            }
        }

        /**
         * 比较方法：优先级高的排前面，相同优先级按提交时间排序
         */
        @Override
        public int compareTo(ScheduledTask other) {
            // 先比较优先级（值小的优先）
            int priorityCompare = Integer.compare(this.priority.getValue(),
                    other.priority.getValue());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 优先级相同，比较提交时间（早提交的优先）
            return Long.compare(this.submitTime, other.submitTime);
        }

        public String getTaskId() {
            return taskId;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }

        public long getExecuteTime() {
            return executeTime;
        }
    }

    /**
     * 优先级任务调度器
     */
    static class PriorityTaskScheduler {
        private final ThreadPoolExecutor executor;
        private final PriorityBlockingQueue<ScheduledTask> delayQueue;
        private final ScheduledExecutorService delayChecker;
        private final ConcurrentHashMap<String, ScheduledTask> taskMap;
        private final AtomicLong taskIdGenerator;
        private volatile boolean shutdown;

        public PriorityTaskScheduler(int threadCount) {
            // 使用PriorityBlockingQueue作为工作队列
            PriorityBlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<>();

            this.executor = new ThreadPoolExecutor(
                    threadCount,
                    threadCount,
                    0L,
                    TimeUnit.MILLISECONDS,
                    workQueue,
                    new NamedThreadFactory("priority-scheduler")
            );

            this.delayQueue = new PriorityBlockingQueue<>();
            this.taskMap = new ConcurrentHashMap<>();
            this.taskIdGenerator = new AtomicLong(1);
            this.shutdown = false;

            // 启动延迟任务检查器
            this.delayChecker = Executors.newSingleThreadScheduledExecutor(
                    new NamedThreadFactory("delay-checker"));
            startDelayChecker();
        }

        /**
         * 提交立即执行的任务
         */
        public String submit(Runnable task, Priority priority) {
            if (shutdown) {
                throw new RejectedExecutionException("调度器已关闭");
            }

            String taskId = "task-" + taskIdGenerator.getAndIncrement();
            ScheduledTask scheduledTask = new ScheduledTask(taskId, task, priority);

            taskMap.put(taskId, scheduledTask);
            executor.execute(scheduledTask);

            System.out.println("[提交] " + taskId + " (优先级: " + priority + ")");
            return taskId;
        }

        /**
         * 提交延迟任务
         */
        public String submitDelayed(Runnable task, Priority priority, long delayMillis) {
            if (shutdown) {
                throw new RejectedExecutionException("调度器已关闭");
            }

            String taskId = "task-" + taskIdGenerator.getAndIncrement();
            ScheduledTask scheduledTask = new ScheduledTask(taskId, task, priority, delayMillis);

            taskMap.put(taskId, scheduledTask);
            delayQueue.offer(scheduledTask);

            System.out.println("[提交] " + taskId + " (优先级: " + priority +
                    ", 延迟: " + delayMillis + "ms)");
            return taskId;
        }

        /**
         * 启动延迟任务检查器
         */
        private void startDelayChecker() {
            delayChecker.scheduleAtFixedRate(() -> {
                try {
                    long now = System.currentTimeMillis();
                    while (!delayQueue.isEmpty()) {
                        ScheduledTask task = delayQueue.peek();
                        if (task != null && task.getExecuteTime() <= now) {
                            delayQueue.poll();
                            if (task.getStatus() != TaskStatus.CANCELLED) {
                                executor.execute(task);
                                System.out.println("[延迟任务就绪] " + task.getTaskId());
                            }
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }

        /**
         * 取消任务
         */
        public boolean cancelTask(String taskId) {
            ScheduledTask task = taskMap.get(taskId);
            if (task == null) {
                return false;
            }

            TaskStatus status = task.getStatus();
            if (status == TaskStatus.PENDING) {
                task.setStatus(TaskStatus.CANCELLED);
                System.out.println("[取消] " + taskId + " (成功)");
                return true;
            } else {
                System.out.println("[取消] " + taskId + " (失败: 任务已" +
                        (status == TaskStatus.RUNNING ? "执行中" : "完成") + ")");
                return false;
            }
        }

        /**
         * 查询任务状态
         */
        public TaskStatus getTaskStatus(String taskId) {
            ScheduledTask task = taskMap.get(taskId);
            return task != null ? task.getStatus() : null;
        }

        /**
         * 获取等待任务数
         */
        public int getPendingTaskCount() {
            return executor.getQueue().size() + delayQueue.size();
        }

        /**
         * 优雅关闭
         */
        public void shutdown() {
            shutdown = true;
            delayChecker.shutdown();
            executor.shutdown();
            System.out.println("\n[关闭] 调度器开始关闭...");
        }

        /**
         * 等待任务完成
         */
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long startTime = System.currentTimeMillis();
            long timeoutMillis = unit.toMillis(timeout);

            // 等待延迟检查器关闭
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = timeoutMillis - elapsed;
            if (remaining > 0) {
                delayChecker.awaitTermination(remaining, TimeUnit.MILLISECONDS);
            }

            // 等待执行器关闭
            elapsed = System.currentTimeMillis() - startTime;
            remaining = timeoutMillis - elapsed;
            if (remaining > 0) {
                boolean terminated = executor.awaitTermination(remaining, TimeUnit.MILLISECONDS);
                if (terminated) {
                    System.out.println("[关闭] 所有任务已完成");
                }
                return terminated;
            }

            return false;
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
            return new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== 优先级任务调度器测试 ===\n");

        // 1. 创建调度器
        PriorityTaskScheduler scheduler = new PriorityTaskScheduler(3);

        // 2. 提交不同优先级的任务
        System.out.println("提交不同优先级的任务:\n");

        String task1 = scheduler.submit(() -> {
            System.out.println("  [执行] 低优先级任务1");
            sleep(1000);
        }, Priority.LOW);

        String task2 = scheduler.submit(() -> {
            System.out.println("  [执行] 高优先级任务1");
            sleep(1000);
        }, Priority.HIGH);

        String task3 = scheduler.submit(() -> {
            System.out.println("  [执行] 中优先级任务1");
            sleep(1000);
        }, Priority.MEDIUM);

        String task4 = scheduler.submit(() -> {
            System.out.println("  [执行] 高优先级任务2");
            sleep(1000);
        }, Priority.HIGH);

        String task5 = scheduler.submit(() -> {
            System.out.println("  [执行] 低优先级任务2");
            sleep(1000);
        }, Priority.LOW);

        // 3. 提交延迟任务
        System.out.println();
        String delayedTask = scheduler.submitDelayed(() -> {
            System.out.println("  [执行] 延迟任务（2秒后，高优先级）");
        }, Priority.HIGH, 2000);

        // 4. 查询任务状态
        Thread.sleep(500);
        System.out.println("\n任务状态查询:");
        System.out.println(task1 + " 状态: " + scheduler.getTaskStatus(task1));
        System.out.println(task2 + " 状态: " + scheduler.getTaskStatus(task2));

        // 5. 测试取消任务
        System.out.println();
        scheduler.cancelTask(task5);

        // 6. 等待所有任务完成
        Thread.sleep(5000);

        // 7. 打印最终状态
        System.out.println("\n最终状态:");
        System.out.println(task1 + " 状态: " + scheduler.getTaskStatus(task1));
        System.out.println(task5 + " 状态: " + scheduler.getTaskStatus(task5));
        System.out.println(delayedTask + " 状态: " + scheduler.getTaskStatus(delayedTask));

        // 8. 关闭调度器
        scheduler.shutdown();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n测试完成");
        System.out.println("\n说明: 任务按优先级执行，高优先级任务优先");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
