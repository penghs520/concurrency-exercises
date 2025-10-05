package com.concurrency.sync.demo;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Demo 02: wait/notify机制
 *
 * 演示内容：
 * 1. wait/notify基本用法
 * 2. 生产者-消费者模式
 * 3. 为什么用while不是if
 * 4. notify vs notifyAll
 */
public class D02_WaitNotify {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Demo 1: wait/notify基本用法 ===");
        demoBasicWaitNotify();

        System.out.println("\n=== Demo 2: 生产者-消费者模式 ===");
        demoProducerConsumer();

        System.out.println("\n=== Demo 3: notify vs notifyAll ===");
        demoNotifyVsNotifyAll();
    }

    // ==================== Demo 1: wait/notify基本用法 ====================

    private static void demoBasicWaitNotify() throws InterruptedException {
        final Object lock = new Object();

        // 等待线程
        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("等待线程: 开始等待...");
                    lock.wait();  // 释放锁，进入等待状态
                    System.out.println("等待线程: 被唤醒！");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Waiter");

        // 通知线程
        Thread notifier = new Thread(() -> {
            try {
                Thread.sleep(2000);  // 等待2秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (lock) {
                System.out.println("通知线程: 发送通知...");
                lock.notify();  // 唤醒等待线程
            }
        }, "Notifier");

        waiter.start();
        Thread.sleep(100);  // 确保waiter先启动
        notifier.start();

        waiter.join();
        notifier.join();
    }

    // ==================== Demo 2: 生产者-消费者模式 ====================

    static class BoundedQueue<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;

        public BoundedQueue(int capacity) {
            this.capacity = capacity;
        }

        // 生产者：向队列添加元素
        public synchronized void put(T item) throws InterruptedException {
            // 注意：使用while而不是if！
            while (queue.size() == capacity) {
                System.out.println(Thread.currentThread().getName() + ": 队列满，等待...");
                wait();  // 队列满，等待消费者取走元素
            }

            queue.add(item);
            System.out.println(Thread.currentThread().getName() + ": 生产 " + item +
                    " (队列大小: " + queue.size() + ")");
            notifyAll();  // 唤醒所有等待的消费者
        }

        // 消费者：从队列取出元素
        public synchronized T take() throws InterruptedException {
            while (queue.isEmpty()) {
                System.out.println(Thread.currentThread().getName() + ": 队列空，等待...");
                wait();  // 队列空，等待生产者添加元素
            }

            T item = queue.remove();
            System.out.println(Thread.currentThread().getName() + ": 消费 " + item +
                    " (队列大小: " + queue.size() + ")");
            notifyAll();  // 唤醒所有等待的生产者
            return item;
        }
    }

    private static void demoProducerConsumer() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(3);

        // 创建生产者线程
        Thread producer1 = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    queue.put(i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "生产者1");

        Thread producer2 = new Thread(() -> {
            try {
                for (int i = 10; i <= 14; i++) {
                    queue.put(i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "生产者2");

        // 创建消费者线程
        Thread consumer1 = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    queue.take();
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "消费者1");

        Thread consumer2 = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    queue.take();
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "消费者2");

        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();

        producer1.join();
        producer2.join();
        consumer1.join();
        consumer2.join();

        System.out.println("生产者-消费者演示完成");
    }

    // ==================== Demo 3: notify vs notifyAll ====================

    private static void demoNotifyVsNotifyAll() throws InterruptedException {
        final Object lock = new Object();

        // 创建3个等待线程
        for (int i = 1; i <= 3; i++) {
            final int threadNum = i;
            new Thread(() -> {
                synchronized (lock) {
                    try {
                        System.out.println("线程" + threadNum + ": 开始等待");
                        lock.wait();
                        System.out.println("线程" + threadNum + ": 被唤醒！");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        Thread.sleep(1000);  // 确保所有线程都在等待

        System.out.println("\n--- 使用notify()只唤醒一个线程 ---");
        synchronized (lock) {
            lock.notify();
        }

        Thread.sleep(1000);

        System.out.println("\n--- 使用notifyAll()唤醒所有线程 ---");
        synchronized (lock) {
            lock.notifyAll();
        }

        Thread.sleep(1000);
        System.out.println("\n结论: notify()只唤醒一个线程，notifyAll()唤醒所有线程");
        System.out.println("最佳实践: 优先使用notifyAll()，避免信号丢失");
    }
}
