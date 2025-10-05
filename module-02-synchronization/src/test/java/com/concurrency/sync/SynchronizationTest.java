package com.concurrency.sync;

import com.concurrency.sync.solutions.S01_BoundedBuffer;
import com.concurrency.sync.solutions.S02_ReadWriteLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module 02 同步机制测试
 */
public class SynchronizationTest {

    @Test
    @Timeout(10)
    public void testBoundedBuffer() throws InterruptedException {
        S01_BoundedBuffer<Integer> buffer = new S01_BoundedBuffer<>(3);

        // 测试基本put/take
        buffer.put(1);
        buffer.put(2);
        assertEquals(2, buffer.size());

        assertEquals(1, buffer.take());
        assertEquals(2, buffer.take());
        assertEquals(0, buffer.size());
    }

    @Test
    @Timeout(10)
    public void testBoundedBufferConcurrent() throws InterruptedException {
        S01_BoundedBuffer<Integer> buffer = new S01_BoundedBuffer<>(10);
        CountDownLatch latch = new CountDownLatch(2);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    buffer.put(i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    buffer.take();
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
        assertEquals(0, buffer.size());
    }

    @Test
    @Timeout(10)
    public void testReadWriteLock() throws InterruptedException {
        S02_ReadWriteLock rwLock = new S02_ReadWriteLock();
        AtomicInteger sharedData = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(4);

        // 读线程
        Runnable reader = () -> {
            try {
                for (int i = 0; i < 10; i++) {
                    rwLock.readLock();
                    try {
                        sharedData.get();
                    } finally {
                        rwLock.readUnlock();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        // 写线程
        Runnable writer = () -> {
            try {
                for (int i = 0; i < 10; i++) {
                    rwLock.writeLock();
                    try {
                        sharedData.incrementAndGet();
                    } finally {
                        rwLock.writeUnlock();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        new Thread(reader).start();
        new Thread(reader).start();
        new Thread(writer).start();
        new Thread(writer).start();

        latch.await();
        assertEquals(20, sharedData.get());
    }
}
