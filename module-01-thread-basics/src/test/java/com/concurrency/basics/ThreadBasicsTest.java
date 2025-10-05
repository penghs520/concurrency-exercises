package com.concurrency.basics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module 01 单元测试
 *
 * 测试线程基础知识点
 */
@DisplayName("线程基础测试")
public class ThreadBasicsTest {

    @Test
    @DisplayName("测试线程创建")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testThreadCreation() {
        // 测试Runnable方式
        Thread thread = new Thread(() -> {
            System.out.println("测试线程执行");
        }, "TestThread");

        assertNotNull(thread);
        assertEquals("TestThread", thread.getName());
        assertEquals(Thread.State.NEW, thread.getState());

        thread.start();
        assertEquals(Thread.State.RUNNABLE, thread.getState());
    }

    @Test
    @DisplayName("测试线程join")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testThreadJoin() throws InterruptedException {
        final boolean[] flag = {false};

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(500);
                flag[0] = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        worker.join(); // 等待worker完成

        assertTrue(flag[0], "工作线程应该已完成");
        assertEquals(Thread.State.TERMINATED, worker.getState());
    }

    @Test
    @DisplayName("测试线程中断")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testThreadInterrupt() throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                // 工作中
            }
        });

        worker.start();
        Thread.sleep(100);

        assertFalse(worker.isInterrupted(), "启动时不应该被中断");

        worker.interrupt();
        Thread.sleep(100);

        assertTrue(worker.isInterrupted(), "应该被中断");
    }

    @Test
    @DisplayName("测试InterruptedException")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testInterruptedException() {
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(5000); // 休眠5秒
                fail("不应该完成休眠");
            } catch (InterruptedException e) {
                // 预期会捕获到中断异常
                Thread.currentThread().interrupt(); // 恢复中断状态
            }
        });

        sleeper.start();

        try {
            Thread.sleep(100);
            sleeper.interrupt(); // 中断休眠
            sleeper.join();
            assertTrue(sleeper.isInterrupted(), "中断标志应该被恢复");
        } catch (InterruptedException e) {
            fail("主线程不应该被中断");
        }
    }

    @Test
    @DisplayName("测试守护线程")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDaemonThread() {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        daemon.setDaemon(true);
        assertTrue(daemon.isDaemon(), "应该是守护线程");

        daemon.start();
        // 主线程结束后，守护线程会自动终止
    }

    @Test
    @DisplayName("测试线程优先级")
    void testThreadPriority() {
        Thread thread = new Thread(() -> {});

        // 默认优先级
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());

        // 设置最高优先级
        thread.setPriority(Thread.MAX_PRIORITY);
        assertEquals(Thread.MAX_PRIORITY, thread.getPriority());

        // 设置最低优先级
        thread.setPriority(Thread.MIN_PRIORITY);
        assertEquals(Thread.MIN_PRIORITY, thread.getPriority());
    }

    @Test
    @DisplayName("测试线程状态转换")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testThreadStates() throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // NEW
        assertEquals(Thread.State.NEW, thread.getState());

        // RUNNABLE
        thread.start();
        Thread.sleep(50);
        assertTrue(thread.getState() == Thread.State.RUNNABLE ||
                   thread.getState() == Thread.State.TIMED_WAITING);

        // TERMINATED
        thread.join();
        assertEquals(Thread.State.TERMINATED, thread.getState());
    }

    @Test
    @DisplayName("测试多线程数据共享")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testThreadDataSharing() throws InterruptedException {
        class Counter {
            int count = 0;
            synchronized void increment() {
                count++;
            }
        }

        Counter counter = new Counter();
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1000, counter.count, "计数应该准确");
    }

    @Test
    @DisplayName("测试线程顺序执行")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testSequentialExecution() throws InterruptedException {
        StringBuilder result = new StringBuilder();

        Thread t1 = new Thread(() -> result.append("1"));
        Thread t2 = new Thread(() -> result.append("2"));
        Thread t3 = new Thread(() -> result.append("3"));

        t1.start();
        t1.join();

        t2.start();
        t2.join();

        t3.start();
        t3.join();

        assertEquals("123", result.toString(), "执行顺序应该是1->2->3");
    }
}
