package com.concurrency.collections.solutions;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 练习2参考答案: 工作队列系统
 *
 * 核心知识点:
 * 1. PriorityBlockingQueue的使用
 * 2. 生产者-消费者模式
 * 3. 优雅关闭机制
 * 4. 任务优先级处理
 */
public class S02_WorkQueue {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 工作队列系统（参考答案） ===\n");

        // 测试工作队列
        testWorkQueue();
    }

    private static void testWorkQueue() throws Exception {
        // 创建工作队列（4个工作线程）
        WorkQueueSystem workQueue = new WorkQueueSystem(4);

        System.out.println("--- 提交任务 ---");

        // 提交不同优先级的任务
        for (int i = 0; i < 10; i++) {
            Priority priority;
            if (i % 3 == 0) {
                priority = Priority.HIGH;
            } else if (i % 3 == 1) {
                priority = Priority.NORMAL;
            } else {
                priority = Priority.LOW;
            }

            int taskId = i;
            workQueue.submit(new Task(taskId, "Task-" + taskId, priority, () -> {
                // 模拟任务执行
                Thread.sleep(100 + ThreadLocalRandom.current().nextInt(200));
                System.out.println("  [" + Thread.currentThread().getName() +
                        "] 完成任务: Task-" + taskId + " (优先级: " + priority + ")");
                return "Result-" + taskId;
            }));
        }

        System.out.println("已提交10个任务\n");

        // 等待一段时间
        Thread.sleep(1000);

        System.out.println("\n--- 当前状态 ---");
        System.out.println(workQueue.getStats());

        // 优雅关闭
        System.out.println("\n--- 关闭队列 ---");
        workQueue.shutdown();
        workQueue.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n--- 最终统计 ---");
        System.out.println(workQueue.getStats());
    }

    /**
     * 任务优先级
     */
    enum Priority {
        HIGH(1),
        NORMAL(5),
        LOW(10);

        final int value;

        Priority(int value) {
            this.value = value;
        }
    }

    /**
     * 任务接口
     */
    @FunctionalInterface
    interface TaskExecutor {
        Object execute() throws Exception;
    }

    /**
     * 任务封装
     */
    static class Task implements Comparable<Task> {
        final int id;
        final String name;
        final Priority priority;
        final TaskExecutor executor;
        final long submitTime;

        public Task(int id, String name, Priority priority, TaskExecutor executor) {
            this.id = id;
            this.name = name;
            this.priority = priority;
            this.executor = executor;
            this.submitTime = System.currentTimeMillis();
        }

        @Override
        public int compareTo(Task other) {
            // 优先级高的排在前面
            int result = Integer.compare(this.priority.value, other.priority.value);
            if (result == 0) {
                // 同优先级，先提交的先执行
                result = Long.compare(this.submitTime, other.submitTime);
            }
            return result;
        }

        @Override
        public String toString() {
            return name + "[" + priority + "]";
        }
    }

    /**
     * 工作队列系统
     */
    static class WorkQueueSystem {
        private final PriorityBlockingQueue<Task> taskQueue;
        private final Thread[] workers;
        private volatile boolean shutdown = false;

        private final AtomicInteger submittedCount = new AtomicInteger(0);
        private final AtomicInteger completedCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);

        public WorkQueueSystem(int workerCount) {
            // 使用PriorityBlockingQueue支持优先级
            this.taskQueue = new PriorityBlockingQueue<>();
            this.workers = new Thread[workerCount];

            // 创建并启动工作线程
            for (int i = 0; i < workerCount; i++) {
                workers[i] = new Thread(new Worker(), "Worker-" + i);
                workers[i].start();
            }
        }

        /**
         * 提交任务
         */
        public void submit(Task task) throws InterruptedException {
            if (shutdown) {
                throw new RejectedExecutionException("WorkQueue已关闭");
            }

            taskQueue.put(task);
            submittedCount.incrementAndGet();
        }

        /**
         * 优雅关闭
         */
        public void shutdown() {
            shutdown = true;

            // 中断所有工作线程
            for (Thread worker : workers) {
                worker.interrupt();
            }
        }

        /**
         * 等待终止
         */
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);

            for (Thread worker : workers) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }

                worker.join(TimeUnit.NANOSECONDS.toMillis(remaining));

                if (worker.isAlive()) {
                    return false;
                }
            }

            return true;
        }

        /**
         * 获取统计信息
         */
        public String getStats() {
            return String.format(
                    "队列状态: 待处理=%d, 已提交=%d, 已完成=%d, 失败=%d",
                    taskQueue.size(),
                    submittedCount.get(),
                    completedCount.get(),
                    failedCount.get()
            );
        }

        /**
         * 工作线程
         */
        class Worker implements Runnable {
            @Override
            public void run() {
                while (!shutdown || !taskQueue.isEmpty()) {
                    try {
                        // 使用poll避免永久阻塞，便于响应shutdown
                        Task task = taskQueue.poll(100, TimeUnit.MILLISECONDS);

                        if (task != null) {
                            try {
                                // 执行任务
                                task.executor.execute();
                                completedCount.incrementAndGet();
                            } catch (Exception e) {
                                // 任务执行失败
                                System.err.println("[" + Thread.currentThread().getName() +
                                        "] 任务执行失败: " + task.name + ", 错误: " + e.getMessage());
                                failedCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        // 被中断，检查shutdown标志
                        if (shutdown) {
                            break;
                        }
                        Thread.currentThread().interrupt();
                    }
                }

                System.out.println("[" + Thread.currentThread().getName() + "] 工作线程退出");
            }
        }
    }
}

/**
 * 【知识点总结】
 *
 * 1. PriorityBlockingQueue的使用:
 *    - 元素需实现Comparable接口
 *    - 自动按优先级排序
 *    - 无界队列，注意内存使用
 *
 * 2. 工作线程模式:
 *    - 循环从队列取任务
 *    - 使用poll(timeout)避免永久阻塞
 *    - 检查shutdown标志决定是否退出
 *
 * 3. 优雅关闭机制:
 *    - 设置shutdown标志（volatile保证可见性）
 *    - 中断所有工作线程
 *    - 等待线程结束（join）
 *    - 处理剩余任务（检查队列是否为空）
 *
 * 4. 异常处理:
 *    - 捕获任务执行异常，不影响工作线程
 *    - 记录失败统计
 *    - 区分中断异常和任务异常
 *
 * 【扩展优化】
 *
 * 1. 任务超时控制:
 *    class TaskWrapper implements Callable<Object> {
 *        public Object call() throws Exception {
 *            Future<?> future = executor.submit(task);
 *            try {
 *                return future.get(timeout, TimeUnit.SECONDS);
 *            } catch (TimeoutException e) {
 *                future.cancel(true);
 *                throw e;
 *            }
 *        }
 *    }
 *
 * 2. 任务重试机制:
 *    - 失败任务重新入队
 *    - 记录重试次数
 *    - 指数退避延迟
 *
 * 3. 动态线程池:
 *    - 根据队列长度动态增减工作线程
 *    - 核心线程 + 最大线程
 *    - 线程空闲超时回收
 *
 * 4. 监控与告警:
 *    - 任务执行时间统计
 *    - 队列长度监控
 *    - 失败率告警
 */
