package com.concurrency.sync.solutions;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 练习01参考答案: 有界缓冲区实现
 *
 * 实现要点:
 * 1. 使用LinkedList作为底层存储
 * 2. put/take方法都需要synchronized保护
 * 3. 使用while循环检查条件（防止虚假唤醒）
 * 4. 使用notifyAll()避免信号丢失
 */
public class S01_BoundedBuffer<T> {

    private final Queue<T> queue;
    private final int capacity;

    public S01_BoundedBuffer(int capacity) {
        this.queue = new LinkedList<>();
        this.capacity = capacity;
    }

    /**
     * 向缓冲区添加元素
     * 如果缓冲区满，则等待直到有空间
     */
    public synchronized void put(T item) throws InterruptedException {
        // 关键点1: 使用while而不是if（防止虚假唤醒）
        while (queue.size() == capacity) {
            wait();  // 队列满，等待消费者取走元素
        }

        queue.add(item);

        // 关键点2: 使用notifyAll而不是notify（避免信号丢失）
        notifyAll();  // 唤醒所有等待的消费者
    }

    /**
     * 从缓冲区取出元素
     * 如果缓冲区空，则等待直到有元素
     */
    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();  // 队列空，等待生产者添加元素
        }

        T item = queue.remove();
        notifyAll();  // 唤醒所有等待的生产者
        return item;
    }

    /**
     * 返回当前缓冲区元素数量
     */
    public synchronized int size() {
        return queue.size();
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testBoundedBuffer();
    }

    private static void testBoundedBuffer() throws InterruptedException {
        S01_BoundedBuffer<Integer> buffer = new S01_BoundedBuffer<>(5);

        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.put(i);
                    System.out.println("生产: " + i + " (缓冲区大小: " + buffer.size() + ")");
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "生产者");

        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    Integer item = buffer.take();
                    System.out.println("消费: " + item + " (缓冲区大小: " + buffer.size() + ")");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "消费者");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("测试完成！");
    }
}
