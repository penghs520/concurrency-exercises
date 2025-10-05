package com.concurrency.collections.demo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo 02: BlockingQueue - 阻塞队列
 *
 * 本示例演示:
 * 1. BlockingQueue的基本操作（put/take）
 * 2. 生产者-消费者模式
 * 3. 不同BlockingQueue实现的特点
 * 4. 批量操作（drainTo）
 */
public class D02_BlockingQueue {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BlockingQueue演示 ===\n");

        // 1. 基本操作
        demo1_BasicOperations();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 生产者-消费者模式
        demo2_ProducerConsumer();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 3. 不同实现对比
        demo3_QueueComparison();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 4. 批量操作
        demo4_BatchOperations();
    }

    /**
     * Demo 1: 基本操作
     */
    private static void demo1_BasicOperations() throws Exception {
        System.out.println("--- 1. BlockingQueue基本操作 ---");

        BlockingQueue<String> queue = new ArrayBlockingQueue<>(3); // 容量为3

        System.out.println("队列容量: 3\n");

        // 1. add() - 满则抛异常
        System.out.println("使用add():");
        queue.add("A");
        queue.add("B");
        queue.add("C");
        System.out.println("  添加3个元素: " + queue);

        try {
            queue.add("D"); // 队列已满
        } catch (IllegalStateException e) {
            System.out.println("  ✗ add()失败: " + e.getClass().getSimpleName());
        }

        // 2. offer() - 满则返回false
        System.out.println("\n使用offer():");
        queue.clear();
        queue.offer("A");
        queue.offer("B");
        queue.offer("C");
        boolean success = queue.offer("D");
        System.out.println("  offer('D')结果: " + success);

        // 3. put() - 满则阻塞
        System.out.println("\n使用put():");
        queue.clear();

        // 在另一个线程中put（会阻塞）
        new Thread(() -> {
            try {
                System.out.println("  [生产者] 准备put 4个元素...");
                queue.put("1");
                queue.put("2");
                queue.put("3");
                System.out.println("  [生产者] 已put 3个，队列满，准备put第4个（会阻塞）...");
                queue.put("4"); // 阻塞
                System.out.println("  [生产者] 成功put第4个元素");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        Thread.sleep(1000); // 等待生产者阻塞

        System.out.println("  [主线程] 当前队列: " + queue);
        System.out.println("  [主线程] 取出一个元素: " + queue.take());
        Thread.sleep(100); // 生产者解除阻塞

        System.out.println("  [主线程] 最终队列: " + queue);

        // 4. take() - 空则阻塞
        System.out.println("\n使用take():");
        queue.clear();

        new Thread(() -> {
            try {
                System.out.println("  [消费者] 从空队列take（会阻塞）...");
                String item = queue.take();
                System.out.println("  [消费者] 成功take: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        Thread.sleep(1000);
        System.out.println("  [主线程] 放入元素'X'");
        queue.put("X"); // 消费者解除阻塞

        Thread.sleep(100);
    }

    /**
     * Demo 2: 生产者-消费者模式
     */
    private static void demo2_ProducerConsumer() throws Exception {
        System.out.println("--- 2. 生产者-消费者模式 ---");

        BlockingQueue<Task> queue = new LinkedBlockingQueue<>(10);
        AtomicInteger taskIdGenerator = new AtomicInteger(0);
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);

        // 生产者
        Runnable producer = () -> {
            try {
                for (int i = 0; i < 20; i++) {
                    Task task = new Task(taskIdGenerator.incrementAndGet());
                    queue.put(task);
                    producedCount.incrementAndGet();
                    System.out.println("[生产者] 生产: " + task + " (队列大小: " + queue.size() + ")");
                    Thread.sleep(50); // 模拟生产耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // 消费者
        Runnable consumer = () -> {
            try {
                while (!Thread.interrupted()) {
                    Task task = queue.take(); // 阻塞等待
                    consumedCount.incrementAndGet();
                    System.out.println("  [消费者] 消费: " + task);
                    Thread.sleep(100); // 模拟处理耗时（比生产慢）
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // 启动线程
        Thread producerThread = new Thread(producer, "Producer");
        Thread consumerThread = new Thread(consumer, "Consumer");

        producerThread.start();
        consumerThread.start();

        // 等待生产者完成
        producerThread.join();
        Thread.sleep(500); // 让消费者多消费一些

        // 停止消费者
        consumerThread.interrupt();
        consumerThread.join(1000);

        System.out.println("\n结果统计:");
        System.out.println("  生产数量: " + producedCount.get());
        System.out.println("  消费数量: " + consumedCount.get());
        System.out.println("  队列剩余: " + queue.size());
    }

    /**
     * Demo 3: 不同实现对比
     */
    private static void demo3_QueueComparison() throws Exception {
        System.out.println("--- 3. 不同BlockingQueue实现对比 ---");

        // 1. ArrayBlockingQueue - 有界，数组实现
        System.out.println("1. ArrayBlockingQueue (有界，数组):");
        BlockingQueue<Integer> arrayQueue = new ArrayBlockingQueue<>(5);
        arrayQueue.add(1);
        arrayQueue.add(2);
        System.out.println("   " + arrayQueue);

        // 2. LinkedBlockingQueue - 可选有界，链表实现
        System.out.println("\n2. LinkedBlockingQueue (可选有界，链表):");
        BlockingQueue<Integer> linkedQueue = new LinkedBlockingQueue<>(5);
        linkedQueue.add(1);
        linkedQueue.add(2);
        System.out.println("   " + linkedQueue);

        // 3. PriorityBlockingQueue - 无界，优先级队列
        System.out.println("\n3. PriorityBlockingQueue (无界，优先级):");
        BlockingQueue<Task> priorityQueue = new PriorityBlockingQueue<>();
        priorityQueue.add(new Task(3));
        priorityQueue.add(new Task(1));
        priorityQueue.add(new Task(2));
        System.out.println("   放入顺序: 3, 1, 2");
        System.out.println("   取出顺序: " + priorityQueue.take().id + ", "
                + priorityQueue.take().id + ", " + priorityQueue.take().id);

        // 4. DelayQueue - 延迟队列
        System.out.println("\n4. DelayQueue (延迟队列):");
        DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();
        long now = System.currentTimeMillis();

        delayQueue.put(new DelayedTask("Task-3s", 3000));
        delayQueue.put(new DelayedTask("Task-1s", 1000));
        delayQueue.put(new DelayedTask("Task-2s", 2000));

        System.out.println("   添加了3个延迟任务");
        System.out.println("   等待并取出...");

        for (int i = 0; i < 3; i++) {
            DelayedTask task = delayQueue.take(); // 阻塞直到任务到期
            long elapsed = System.currentTimeMillis() - now;
            System.out.println("   " + task.name + " (等待了 " + elapsed + "ms)");
        }

        // 5. SynchronousQueue - 零容量队列
        System.out.println("\n5. SynchronousQueue (零容量，直接交换):");
        SynchronousQueue<String> syncQueue = new SynchronousQueue<>();

        new Thread(() -> {
            try {
                System.out.println("   [线程A] 准备put...");
                syncQueue.put("数据"); // 阻塞，直到有消费者take
                System.out.println("   [线程A] put成功（已被取走）");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        Thread.sleep(1000);
        System.out.println("   [主线程] 准备take...");
        String data = syncQueue.take();
        System.out.println("   [主线程] take到: " + data);
    }

    /**
     * Demo 4: 批量操作
     */
    private static void demo4_BatchOperations() throws Exception {
        System.out.println("--- 4. 批量操作 (drainTo) ---");

        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

        // 填充队列
        for (int i = 1; i <= 10; i++) {
            queue.put(i);
        }

        System.out.println("队列初始大小: " + queue.size());

        // drainTo - 批量取出（减少锁竞争）
        java.util.List<Integer> batch = new java.util.ArrayList<>();
        int count = queue.drainTo(batch, 5); // 最多取5个

        System.out.println("批量取出数量: " + count);
        System.out.println("取出的元素: " + batch);
        System.out.println("队列剩余大小: " + queue.size());

        // 再次批量取出全部
        batch.clear();
        queue.drainTo(batch); // 取出所有
        System.out.println("\n全部取出: " + batch);
        System.out.println("队列剩余大小: " + queue.size());
    }

    /**
     * 任务类
     */
    static class Task implements Comparable<Task> {
        final int id;

        Task(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.id, other.id); // 按ID排序
        }

        @Override
        public String toString() {
            return "Task-" + id;
        }
    }

    /**
     * 延迟任务类
     */
    static class DelayedTask implements Delayed {
        final String name;
        final long delayTime; // 延迟时间（毫秒）
        final long expire;    // 过期时间戳

        DelayedTask(String name, long delayTime) {
            this.name = name;
            this.delayTime = delayTime;
            this.expire = System.currentTimeMillis() + delayTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = expire - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.expire, ((DelayedTask) o).expire);
        }
    }
}

/**
 * 【知识点总结】
 *
 * 1. BlockingQueue操作:
 *    插入: add(抛异常) / offer(返回false) / put(阻塞)
 *    移除: remove(抛异常) / poll(返回null) / take(阻塞)
 *    检查: element(抛异常) / peek(返回null)
 *
 * 2. 实现类特点:
 *    - ArrayBlockingQueue: 有界，数组，单锁
 *    - LinkedBlockingQueue: 可选有界，链表，双锁（高吞吐）
 *    - PriorityBlockingQueue: 无界，堆，优先级
 *    - DelayQueue: 无界，延迟任务
 *    - SynchronousQueue: 零容量，直接交换
 *
 * 3. 典型应用:
 *    - 生产者-消费者模式
 *    - 任务队列
 *    - 线程池工作队列
 *    - 定时任务调度
 *
 * 4. 最佳实践:
 *    - 优先使用有界队列（防止OOM）
 *    - 正确处理InterruptedException
 *    - 批量操作用drainTo（减少锁竞争）
 *    - 根据场景选择合适的实现
 */
