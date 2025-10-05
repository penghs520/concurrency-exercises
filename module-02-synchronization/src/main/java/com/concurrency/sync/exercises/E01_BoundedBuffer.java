package com.concurrency.sync.exercises;

/**
 * 练习01: 有界缓冲区实现 🟢
 *
 * 难度: 基础
 * 预计时间: 20分钟
 *
 * 任务描述:
 * 实现一个线程安全的有界缓冲区（固定大小的队列），支持以下操作：
 * 1. put(T item): 向缓冲区添加元素，如果缓冲区满则等待
 * 2. take(): 从缓冲区取出元素，如果缓冲区空则等待
 * 3. size(): 返回当前缓冲区元素数量
 *
 * 要求:
 * - 使用synchronized + wait/notify实现
 * - 保证线程安全
 * - 正确处理生产者-消费者场景
 *
 * 提示:
 * - 使用while循环检查条件（不是if）
 * - 优先使用notifyAll()而不是notify()
 * - 可以使用LinkedList或数组作为底层存储
 */
public class E01_BoundedBuffer<T> {

    // TODO: 添加必要的字段

    public E01_BoundedBuffer(int capacity) {
        // TODO: 初始化
    }

    /**
     * 向缓冲区添加元素
     * 如果缓冲区满，则等待直到有空间
     */
    public void put(T item) throws InterruptedException {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 从缓冲区取出元素
     * 如果缓冲区空，则等待直到有元素
     */
    public T take() throws InterruptedException {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 返回当前缓冲区元素数量
     */
    public int size() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testBoundedBuffer();
    }

    private static void testBoundedBuffer() throws InterruptedException {
        E01_BoundedBuffer<Integer> buffer = new E01_BoundedBuffer<>(5);

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
