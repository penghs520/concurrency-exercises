package com.concurrency.collections.exercises;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 练习2: 工作队列系统 🟡
 *
 * 【题目描述】
 * 实现一个支持优先级的工作队列系统，能够处理不同优先级的任务。
 *
 * 【要求】
 * 1. 使用BlockingQueue实现任务队列
 * 2. 支持任务优先级（HIGH, NORMAL, LOW）
 * 3. 多个工作线程并发处理任务
 * 4. 支持优雅关闭（等待所有任务完成）
 * 5. 提供统计信息（已处理、待处理、失败任务数）
 *
 * 【学习目标】
 * - BlockingQueue的实际应用
 * - PriorityBlockingQueue的使用
 * - 生产者-消费者模式
 * - 优雅关闭机制
 *
 * 【难度】: 🟡 中等
 */
public class E02_WorkQueue {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 工作队列系统 ===\n");

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
     * TODO: 完成实现
     */
    static class WorkQueueSystem {
        private final BlockingQueue<Task> taskQueue;
        private final Thread[] workers;
        private volatile boolean shutdown = false;

        private final AtomicInteger submittedCount = new AtomicInteger(0);
        private final AtomicInteger completedCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);

        public WorkQueueSystem(int workerCount) {
            // TODO: 初始化队列和工作线程
            // 提示：
            // 1. 使用PriorityBlockingQueue支持优先级
            // 2. 创建workerCount个工作线程
            // 3. 启动所有工作线程

            this.taskQueue = null; // TODO: 创建PriorityBlockingQueue
            this.workers = new Thread[workerCount];

            // TODO: 创建并启动工作线程
            // for (int i = 0; i < workerCount; i++) {
            //     workers[i] = new Thread(new Worker(), "Worker-" + i);
            //     workers[i].start();
            // }
        }

        /**
         * 提交任务
         * TODO: 实现任务提交
         */
        public void submit(Task task) throws InterruptedException {
            // TODO: 实现
            // 提示：
            // 1. 检查是否已关闭
            // 2. 将任务放入队列
            // 3. 更新提交计数
        }

        /**
         * 优雅关闭
         * TODO: 实现关闭逻辑
         */
        public void shutdown() {
            // TODO: 实现
            // 提示：
            // 1. 设置shutdown标志
            // 2. 中断所有工作线程
        }

        /**
         * 等待终止
         * TODO: 实现等待终止
         */
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            // TODO: 实现
            // 提示：
            // 1. 等待所有工作线程结束
            // 2. 使用Thread.join(timeout)
            return false;
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
         * TODO: 实现工作线程逻辑
         */
        class Worker implements Runnable {
            @Override
            public void run() {
                // TODO: 实现
                // 提示：
                // 1. 循环从队列取任务
                // 2. 执行任务
                // 3. 处理异常
                // 4. 更新统计信息
                // 5. 响应shutdown信号

                // 伪代码：
                // while (!shutdown || !taskQueue.isEmpty()) {
                //     try {
                //         Task task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                //         if (task != null) {
                //             执行task
                //             更新completedCount
                //         }
                //     } catch (Exception e) {
                //         更新failedCount
                //     }
                // }
            }
        }
    }
}

/**
 * 【参考输出】
 * === 工作队列系统 ===
 *
 * --- 提交任务 ---
 * 已提交10个任务
 *
 *   [Worker-0] 完成任务: Task-0 (优先级: HIGH)
 *   [Worker-1] 完成任务: Task-3 (优先级: HIGH)
 *   [Worker-2] 完成任务: Task-6 (优先级: HIGH)
 *   [Worker-3] 完成任务: Task-9 (优先级: HIGH)
 *   [Worker-0] 完成任务: Task-1 (优先级: NORMAL)
 *   [Worker-1] 完成任务: Task-4 (优先级: NORMAL)
 *   [Worker-2] 完成任务: Task-7 (优先级: NORMAL)
 *   [Worker-3] 完成任务: Task-2 (优先级: LOW)
 *   [Worker-0] 完成任务: Task-5 (优先级: LOW)
 *
 * --- 当前状态 ---
 * 队列状态: 待处理=1, 已提交=10, 已完成=9, 失败=0
 *
 * --- 关闭队列 ---
 *   [Worker-1] 完成任务: Task-8 (优先级: LOW)
 *
 * --- 最终统计 ---
 * 队列状态: 待处理=0, 已提交=10, 已完成=10, 失败=0
 */

/**
 * 【实现提示】
 *
 * 1. 队列选择:
 *    - PriorityBlockingQueue: 支持优先级
 *    - 实现Comparable接口定义优先级规则
 *
 * 2. 工作线程模式:
 *    - 循环从队列取任务
 *    - 使用poll(timeout)避免永久阻塞
 *    - 检查shutdown标志
 *
 * 3. 优雅关闭:
 *    - 设置shutdown标志（volatile）
 *    - 等待队列清空
 *    - 中断工作线程
 *    - join等待线程结束
 *
 * 4. 异常处理:
 *    - 捕获任务执行异常
 *    - 记录失败统计
 *    - 不影响其他任务
 *
 * 【扩展功能】
 *
 * 1. 任务超时:
 *    - 使用Future包装任务
 *    - 设置执行超时时间
 *    - 超时后取消任务
 *
 * 2. 任务重试:
 *    - 失败任务重新入队
 *    - 设置最大重试次数
 *    - 指数退避策略
 *
 * 3. 动态工作线程:
 *    - 根据队列长度动态调整
 *    - 核心线程 + 临时线程
 *    - 类似ThreadPoolExecutor
 *
 * 4. 任务监控:
 *    - 任务执行时间统计
 *    - 队列长度监控
 *    - 告警机制
 */
