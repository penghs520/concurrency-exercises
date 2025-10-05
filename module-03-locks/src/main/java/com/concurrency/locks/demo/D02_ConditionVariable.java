package com.concurrency.locks.demo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo 02: Condition条件变量详解
 *
 * 本示例演示：
 * 1. Condition基本用法（await/signal）
 * 2. 多个Condition实现生产者-消费者
 * 3. Condition vs wait/notify的区别
 * 4. signalAll vs signal的使用
 */
public class D02_ConditionVariable {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Condition条件变量演示 ===\n");

        // 演示1: 基本用法
        demo1_BasicUsage();
        Thread.sleep(2000);

        // 演示2: 生产者-消费者模式
        demo2_ProducerConsumer();
        Thread.sleep(3000);

        // 演示3: 多个Condition的优势
        demo3_MultipleConditions();

        System.out.println("\n所有演示完成！");
    }

    /**
     * 演示1: Condition的基本用法
     * await等待，signal唤醒
     */
    private static void demo1_BasicUsage() {
        System.out.println("--- 演示1: Condition基本用法 ---");

        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        // 线程1: 等待条件
        Thread waiter = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("线程1: 开始等待条件...");
                condition.await();  // 释放锁并等待
                System.out.println("线程1: 条件满足，继续执行");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "Waiter");

        // 线程2: 发送信号
        Thread signaler = new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待1秒
                lock.lock();
                try {
                    System.out.println("线程2: 发送信号");
                    condition.signal();  // 唤醒等待的线程
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Signaler");

        waiter.start();
        signaler.start();

        try {
            waiter.join();
            signaler.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示2: 生产者-消费者模式
     * 使用单个Condition实现
     */
    private static void demo2_ProducerConsumer() {
        System.out.println("--- 演示2: 生产者-消费者模式 ---");

        SimpleBuffer<Integer> buffer = new SimpleBuffer<>(3);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.put(i);
                    System.out.println("生产: " + i + " (缓冲区大小: " + buffer.size() + ")");
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "生产者");

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    Integer item = buffer.take();
                    System.out.println("消费: " + item + " (缓冲区大小: " + buffer.size() + ")");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "消费者");

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示3: 多个Condition的优势
     * 使用两个Condition实现精确通知
     */
    private static void demo3_MultipleConditions() throws InterruptedException {
        System.out.println("--- 演示3: 多个Condition的优势 ---");

        BoundedBuffer<String> buffer = new BoundedBuffer<>(2);

        // 多个生产者
        for (int i = 1; i <= 2; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    for (int j = 1; j <= 3; j++) {
                        String item = "P" + id + "-" + j;
                        buffer.put(item);
                        System.out.println("生产者" + id + " 生产: " + item);
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "生产者-" + i).start();
        }

        // 多个消费者
        for (int i = 1; i <= 2; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    for (int j = 1; j <= 3; j++) {
                        String item = buffer.take();
                        System.out.println("消费者" + id + " 消费: " + item);
                        Thread.sleep(300);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "消费者-" + i).start();
        }

        Thread.sleep(5000);
        System.out.println();
    }

    /**
     * 简单缓冲区 - 使用单个Condition
     * 演示基本的生产者-消费者模式
     */
    static class SimpleBuffer<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();

        public SimpleBuffer(int capacity) {
            this.capacity = capacity;
        }

        /**
         * 添加元素
         * 如果缓冲区满，等待
         */
        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                // 关键: 使用while而不是if（防止虚假唤醒）
                while (queue.size() == capacity) {
                    condition.await();  // 等待空间
                }

                queue.add(item);
                condition.signalAll();  // 通知所有等待的线程
            } finally {
                lock.unlock();
            }
        }

        /**
         * 取出元素
         * 如果缓冲区空，等待
         */
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    condition.await();  // 等待元素
                }

                T item = queue.remove();
                condition.signalAll();  // 通知所有等待的线程
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 有界缓冲区 - 使用两个Condition
     * 演示多个Condition的优势：精确通知
     */
    static class BoundedBuffer<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;
        private final Lock lock = new ReentrantLock();

        // 两个条件变量：实现精确通知
        private final Condition notFull = lock.newCondition();   // 非满条件
        private final Condition notEmpty = lock.newCondition();  // 非空条件

        public BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        /**
         * 生产者调用：添加元素
         * 等待"非满"条件，通知"非空"条件
         */
        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                // 等待队列非满
                while (queue.size() == capacity) {
                    System.out.println("  [" + Thread.currentThread().getName() + " 等待空间...]");
                    notFull.await();  // 等待非满条件
                }

                queue.add(item);

                // 通知消费者：队列非空
                notEmpty.signal();  // 只唤醒一个消费者
            } finally {
                lock.unlock();
            }
        }

        /**
         * 消费者调用：取出元素
         * 等待"非空"条件，通知"非满"条件
         */
        public T take() throws InterruptedException {
            lock.lock();
            try {
                // 等待队列非空
                while (queue.isEmpty()) {
                    System.out.println("  [" + Thread.currentThread().getName() + " 等待元素...]");
                    notEmpty.await();  // 等待非空条件
                }

                T item = queue.remove();

                // 通知生产者：队列非满
                notFull.signal();  // 只唤醒一个生产者

                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Condition vs wait/notify 对比说明
     *
     * 【wait/notify - 单个监视器】
     * synchronized (lock) {
     *     while (!condition) {
     *         lock.wait();  // 所有线程等待同一个监视器
     *     }
     *     lock.notifyAll();  // 唤醒所有线程（包括不需要的）
     * }
     *
     * 【Condition - 多个条件变量】
     * lock.lock();
     * try {
     *     while (!condition) {
     *         notFull.await();  // 生产者等待notFull
     *     }
     *     notEmpty.signal();  // 只唤醒消费者
     * } finally {
     *     lock.unlock();
     * }
     *
     * 优势:
     * 1. 精确通知：生产者只唤醒消费者，消费者只唤醒生产者
     * 2. 避免惊群：不会唤醒不需要的线程
     * 3. 更灵活：可以创建多个条件变量
     */
}
