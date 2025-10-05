package com.concurrency.locks.exercises;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 练习02: 自定义阻塞队列 🟡
 *
 * 难度: 中等
 * 预计时间: 40分钟
 *
 * 任务描述:
 * 使用Lock和Condition实现一个有界阻塞队列，类似于ArrayBlockingQueue。
 *
 * 要求:
 * 1. 使用ReentrantLock和Condition实现
 * 2. 支持put和take操作（阻塞式）
 * 3. 支持offer和poll操作（非阻塞式）
 * 4. 队列满时put阻塞，队列空时take阻塞
 * 5. 使用两个Condition实现精确通知
 *
 * 提示:
 * - 使用数组作为底层存储
 * - 使用两个Condition: notFull和notEmpty
 * - put时等待notFull，通知notEmpty
 * - take时等待notEmpty，通知notFull
 * - 使用while循环检查条件（防止虚假唤醒）
 */
public class E02_CustomBlockingQueue<T> {

    // TODO: 添加必要的字段
    // private final T[] items;
    // private final Lock lock = new ReentrantLock();
    // private final Condition notFull = lock.newCondition();
    // private final Condition notEmpty = lock.newCondition();
    // private int putIndex, takeIndex, count;

    public E02_CustomBlockingQueue(int capacity) {
        // TODO: 初始化
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 添加元素（阻塞）
     * 如果队列满，则等待直到有空间
     */
    public void put(T item) throws InterruptedException {
        // TODO: 实现
        // 1. 获取锁
        // 2. 使用while循环等待notFull条件
        // 3. 添加元素
        // 4. 更新putIndex和count
        // 5. 通知notEmpty
        // 6. 释放锁

        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 取出元素（阻塞）
     * 如果队列空，则等待直到有元素
     */
    public T take() throws InterruptedException {
        // TODO: 实现
        // 1. 获取锁
        // 2. 使用while循环等待notEmpty条件
        // 3. 取出元素
        // 4. 更新takeIndex和count
        // 5. 通知notFull
        // 6. 释放锁

        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 添加元素（非阻塞）
     * 如果队列满，立即返回false
     */
    public boolean offer(T item) {
        // TODO: 实现（选做）
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 取出元素（非阻塞）
     * 如果队列空，返回null
     */
    public T poll() {
        // TODO: 实现（选做）
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 返回当前队列大小
     */
    public int size() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 返回队列容量
     */
    public int capacity() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 判断队列是否为空
     */
    public boolean isEmpty() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 判断队列是否已满
     */
    public boolean isFull() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testBlockingQueue();
    }

    private static void testBlockingQueue() throws InterruptedException {
        System.out.println("=== 阻塞队列测试 ===\n");

        E02_CustomBlockingQueue<Integer> queue = new E02_CustomBlockingQueue<>(5);

        // 生产者线程
        Thread producer1 = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    queue.put(i);
                    System.out.println("生产者1 生产: " + i + " (队列大小: " + queue.size() + ")");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "生产者1");

        Thread producer2 = new Thread(() -> {
            try {
                for (int i = 101; i <= 105; i++) {
                    queue.put(i);
                    System.out.println("生产者2 生产: " + i + " (队列大小: " + queue.size() + ")");
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "生产者2");

        // 消费者线程
        Thread consumer1 = new Thread(() -> {
            try {
                for (int i = 1; i <= 7; i++) {
                    Integer item = queue.take();
                    System.out.println("消费者1 消费: " + item + " (队列大小: " + queue.size() + ")");
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "消费者1");

        Thread consumer2 = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    Integer item = queue.take();
                    System.out.println("消费者2 消费: " + item + " (队列大小: " + queue.size() + ")");
                    Thread.sleep(350);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "消费者2");

        // 启动线程
        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();

        // 等待完成
        producer1.join();
        producer2.join();
        consumer1.join();
        consumer2.join();

        System.out.println("\n最终队列大小: " + queue.size());
        System.out.println("✓ 测试完成！");
    }
}
