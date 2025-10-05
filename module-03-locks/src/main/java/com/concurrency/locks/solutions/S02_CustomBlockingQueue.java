package com.concurrency.locks.solutions;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 练习02参考答案: 自定义阻塞队列
 *
 * 实现要点:
 * 1. 使用数组作为底层存储（循环数组）
 * 2. 使用两个Condition实现精确通知
 *   - notFull: 队列非满条件（生产者等待）
 *   - notEmpty: 队列非空条件（消费者等待）
 * 3. 使用while循环检查条件（防止虚假唤醒）
 * 4. put时通知notEmpty，take时通知notFull
 */
public class S02_CustomBlockingQueue<T> {

    private final T[] items;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();   // 非满条件
    private final Condition notEmpty = lock.newCondition();  // 非空条件

    private int putIndex = 0;   // 下一个put的位置
    private int takeIndex = 0;  // 下一个take的位置
    private int count = 0;      // 当前元素数量

    @SuppressWarnings("unchecked")
    public S02_CustomBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("容量必须大于0");
        }
        this.items = (T[]) new Object[capacity];
    }

    /**
     * 添加元素（阻塞）
     * 如果队列满，则等待直到有空间
     */
    public void put(T item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException("元素不能为null");
        }

        lock.lock();
        try {
            // 关键点1: 使用while而不是if（防止虚假唤醒）
            while (count == items.length) {
                notFull.await();  // 等待队列非满
            }

            // 添加元素到循环数组
            items[putIndex] = item;
            putIndex = (putIndex + 1) % items.length;  // 循环索引
            count++;

            // 关键点2: 通知notEmpty条件（唤醒等待的消费者）
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取出元素（阻塞）
     * 如果队列空，则等待直到有元素
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            // 使用while循环检查条件
            while (count == 0) {
                notEmpty.await();  // 等待队列非空
            }

            // 取出元素
            T item = items[takeIndex];
            items[takeIndex] = null;  // 帮助GC
            takeIndex = (takeIndex + 1) % items.length;  // 循环索引
            count--;

            // 通知notFull条件（唤醒等待的生产者）
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加元素（非阻塞）
     * 如果队列满，立即返回false
     */
    public boolean offer(T item) {
        if (item == null) {
            throw new NullPointerException("元素不能为null");
        }

        lock.lock();
        try {
            if (count == items.length) {
                return false;  // 队列满，返回false
            }

            items[putIndex] = item;
            putIndex = (putIndex + 1) % items.length;
            count++;
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取出元素（非阻塞）
     * 如果队列空，返回null
     */
    public T poll() {
        lock.lock();
        try {
            if (count == 0) {
                return null;  // 队列空，返回null
            }

            T item = items[takeIndex];
            items[takeIndex] = null;
            takeIndex = (takeIndex + 1) % items.length;
            count--;
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回当前队列大小
     */
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回队列容量
     */
    public int capacity() {
        return items.length;
    }

    /**
     * 判断队列是否为空
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return count == 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 判断队列是否已满
     */
    public boolean isFull() {
        lock.lock();
        try {
            return count == items.length;
        } finally {
            lock.unlock();
        }
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testBlockingQueue();
    }

    private static void testBlockingQueue() throws InterruptedException {
        System.out.println("=== 阻塞队列测试 ===\n");

        S02_CustomBlockingQueue<Integer> queue = new S02_CustomBlockingQueue<>(5);

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
