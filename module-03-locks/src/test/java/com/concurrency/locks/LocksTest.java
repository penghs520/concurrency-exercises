package com.concurrency.locks;

import com.concurrency.locks.solutions.S01_BankTransfer;
import com.concurrency.locks.solutions.S02_CustomBlockingQueue;
import com.concurrency.locks.solutions.S03_CacheWithReadWriteLock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lock框架单元测试
 */
public class LocksTest {

    /**
     * 测试银行转账
     */
    @Test
    public void testBankTransfer() throws InterruptedException {
        // 创建账户
        S01_BankTransfer.BankAccount account1 = new S01_BankTransfer.BankAccount("A001", 1000);
        S01_BankTransfer.BankAccount account2 = new S01_BankTransfer.BankAccount("A002", 1000);
        S01_BankTransfer.BankAccount account3 = new S01_BankTransfer.BankAccount("A003", 1000);

        // 创建多个转账线程
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                account1.transfer(account2, 50);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                account2.transfer(account3, 50);
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                account3.transfer(account1, 50);
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // 验证总余额不变
        int totalBalance = account1.getBalance() + account2.getBalance() + account3.getBalance();
        assertEquals(3000, totalBalance, "总余额应保持不变");
    }

    /**
     * 测试银行转账 - 余额不足
     */
    @Test
    public void testBankTransfer_InsufficientBalance() {
        S01_BankTransfer.BankAccount account1 = new S01_BankTransfer.BankAccount("A001", 100);
        S01_BankTransfer.BankAccount account2 = new S01_BankTransfer.BankAccount("A002", 0);

        // 转账金额大于余额
        boolean success = account1.transfer(account2, 200);
        assertFalse(success, "余额不足时转账应失败");
        assertEquals(100, account1.getBalance(), "转出账户余额不变");
        assertEquals(0, account2.getBalance(), "转入账户余额不变");
    }

    /**
     * 测试阻塞队列 - 基本操作
     */
    @Test
    public void testBlockingQueue_BasicOperations() throws InterruptedException {
        S02_CustomBlockingQueue<Integer> queue = new S02_CustomBlockingQueue<>(5);

        // 测试put和take
        queue.put(1);
        queue.put(2);
        queue.put(3);

        assertEquals(3, queue.size());

        assertEquals(1, queue.take());
        assertEquals(2, queue.take());
        assertEquals(1, queue.size());
    }

    /**
     * 测试阻塞队列 - 生产者消费者
     */
    @Test
    public void testBlockingQueue_ProducerConsumer() throws InterruptedException {
        S02_CustomBlockingQueue<Integer> queue = new S02_CustomBlockingQueue<>(10);
        int itemCount = 100;
        CountDownLatch latch = new CountDownLatch(2);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    queue.put(i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 消费者
        AtomicInteger sum = new AtomicInteger(0);
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    sum.addAndGet(queue.take());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        producer.start();
        consumer.start();

        latch.await();

        // 验证所有元素都被正确处理
        int expectedSum = itemCount * (itemCount + 1) / 2;
        assertEquals(expectedSum, sum.get(), "所有元素应被正确消费");
        assertEquals(0, queue.size(), "队列应为空");
    }

    /**
     * 测试阻塞队列 - offer和poll
     */
    @Test
    public void testBlockingQueue_OfferAndPoll() {
        S02_CustomBlockingQueue<Integer> queue = new S02_CustomBlockingQueue<>(3);

        // 测试offer
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertTrue(queue.offer(3));
        assertFalse(queue.offer(4), "队列满时offer应返回false");

        // 测试poll
        assertEquals(1, queue.poll());
        assertEquals(2, queue.poll());
        assertEquals(3, queue.poll());
        assertNull(queue.poll(), "队列空时poll应返回null");
    }

    /**
     * 测试缓存 - 基本操作
     */
    @Test
    public void testCache_BasicOperations() {
        AtomicInteger loadCount = new AtomicInteger(0);

        S03_CacheWithReadWriteLock<String, String> cache =
                new S03_CacheWithReadWriteLock<>(key -> {
                    loadCount.incrementAndGet();
                    return "Data-" + key;
                });

        // 第一次读取，应该触发加载
        String value1 = cache.get("key1");
        assertEquals("Data-key1", value1);
        assertEquals(1, loadCount.get(), "应该加载一次");

        // 第二次读取，应该从缓存获取
        String value2 = cache.get("key1");
        assertEquals("Data-key1", value2);
        assertEquals(1, loadCount.get(), "不应该再次加载");

        // 测试put
        cache.put("key2", "Custom-Data");
        assertEquals("Custom-Data", cache.get("key2"));
        assertEquals(1, loadCount.get(), "不应该触发加载");
    }

    /**
     * 测试缓存 - 并发读取
     */
    @Test
    public void testCache_ConcurrentReads() throws InterruptedException {
        AtomicInteger loadCount = new AtomicInteger(0);

        S03_CacheWithReadWriteLock<String, String> cache =
                new S03_CacheWithReadWriteLock<>(key -> {
                    loadCount.incrementAndGet();
                    try {
                        Thread.sleep(100); // 模拟加载耗时
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return "Data-" + key;
                });

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<String> results = new ArrayList<>();

        // 创建多个线程并发读取同一个key
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                String value = cache.get("shared-key");
                synchronized (results) {
                    results.add(value);
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // 验证所有线程都读取到相同的值
        assertEquals(threadCount, results.size());
        results.forEach(value -> assertEquals("Data-shared-key", value));

        // 验证只加载了一次（双重检查生效）
        assertTrue(loadCount.get() <= 2, "应该只加载1-2次（双重检查）");
    }

    /**
     * 测试缓存 - 更新和删除
     */
    @Test
    public void testCache_UpdateAndRemove() {
        S03_CacheWithReadWriteLock<String, String> cache =
                new S03_CacheWithReadWriteLock<>(key -> "Original-" + key);

        // 初始加载
        assertEquals("Original-key1", cache.get("key1"));

        // 更新缓存
        cache.put("key1", "Updated-Data");
        assertEquals("Updated-Data", cache.get("key1"));

        // 删除缓存
        cache.remove("key1");
        assertEquals("Original-key1", cache.get("key1"), "删除后应重新加载");

        // 清空缓存
        cache.put("key2", "Data2");
        cache.clear();
        assertEquals(0, cache.size());
    }

    /**
     * 测试缓存 - 读写并发
     */
    @Test
    public void testCache_ConcurrentReadWrite() throws InterruptedException {
        S03_CacheWithReadWriteLock<Integer, String> cache =
                new S03_CacheWithReadWriteLock<>(key -> "Data-" + key);

        int operationCount = 100;
        CountDownLatch latch = new CountDownLatch(2);

        // 写线程
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < operationCount; i++) {
                    cache.put(i, "Value-" + i);
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 读线程
        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < operationCount; i++) {
                    cache.get(i % 10);
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        writer.start();
        reader.start();

        latch.await();

        // 验证缓存正常工作
        assertTrue(cache.size() > 0, "缓存应该有数据");
    }

    /**
     * 测试ReentrantLock的可重入性
     */
    @Test
    public void testReentrantLock_Reentrant() {
        S01_BankTransfer.BankAccount account = new S01_BankTransfer.BankAccount("A001", 1000);

        // 测试可重入：同一线程多次调用需要锁的方法
        account.deposit(100);
        account.withdraw(50);
        int balance = account.getBalance();

        assertEquals(1050, balance);
    }

    /**
     * 测试阻塞队列的容量限制
     */
    @Test
    public void testBlockingQueue_Capacity() {
        S02_CustomBlockingQueue<Integer> queue = new S02_CustomBlockingQueue<>(3);

        assertEquals(3, queue.capacity());
        assertTrue(queue.isEmpty());
        assertFalse(queue.isFull());

        queue.offer(1);
        queue.offer(2);
        queue.offer(3);

        assertTrue(queue.isFull());
        assertFalse(queue.isEmpty());
    }
}
