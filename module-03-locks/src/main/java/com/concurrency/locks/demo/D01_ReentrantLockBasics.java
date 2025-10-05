package com.concurrency.locks.demo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo 01: ReentrantLock基础用法
 *
 * 本示例演示：
 * 1. Lock的基本用法（lock/unlock）
 * 2. tryLock非阻塞尝试获取锁
 * 3. tryLock超时获取锁
 * 4. lockInterruptibly可中断获取锁
 * 5. 公平锁 vs 非公平锁
 * 6. 可重入性演示
 */
public class D01_ReentrantLockBasics {

    public static void main(String[] args) throws Exception {
        System.out.println("=== ReentrantLock基础用法演示 ===\n");

        // 演示1: 基本用法
        demo1_BasicUsage();
        Thread.sleep(500);

        // 演示2: tryLock非阻塞尝试
        demo2_TryLock();
        Thread.sleep(500);

        // 演示3: tryLock超时获取
        demo3_TryLockWithTimeout();
        Thread.sleep(500);

        // 演示4: lockInterruptibly可中断
        demo4_LockInterruptibly();
        Thread.sleep(500);

        // 演示5: 公平锁vs非公平锁
        demo5_FairVsUnfair();
        Thread.sleep(500);

        // 演示6: 可重入性
        demo6_Reentrant();

        System.out.println("\n所有演示完成！");
    }

    /**
     * 演示1: Lock的基本用法
     * lock/unlock的标准模式
     */
    private static void demo1_BasicUsage() {
        System.out.println("--- 演示1: 基本用法 ---");

        Counter counter = new Counter();

        // 创建多个线程同时增加计数
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    counter.increment();
                }
            }, "Thread-" + i);
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("最终计数: " + counter.getCount() + " (预期: 500)");
        System.out.println();
    }

    /**
     * 演示2: tryLock - 非阻塞尝试获取锁
     * 如果无法立即获取锁，返回false而不是阻塞
     */
    private static void demo2_TryLock() {
        System.out.println("--- 演示2: tryLock非阻塞尝试 ---");

        Lock lock = new ReentrantLock();

        // 线程1: 持有锁5秒
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("线程1: 获取锁，持有5秒");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                System.out.println("线程1: 释放锁");
            }
        }, "Holder");

        // 线程2: 尝试获取锁，不阻塞
        Thread trier = new Thread(() -> {
            try {
                Thread.sleep(100); // 确保线程1先获取锁
                System.out.println("线程2: 尝试获取锁...");

                if (lock.tryLock()) {
                    try {
                        System.out.println("线程2: 成功获取锁");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("线程2: 无法获取锁，执行替代方案");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Trier");

        holder.start();
        trier.start();

        try {
            holder.join();
            trier.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示3: tryLock(timeout) - 限时等待获取锁
     * 最多等待指定时间，超时返回false
     */
    private static void demo3_TryLockWithTimeout() {
        System.out.println("--- 演示3: tryLock超时获取 ---");

        Lock lock = new ReentrantLock();

        // 线程1: 持有锁2秒
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("线程1: 获取锁，持有2秒");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                System.out.println("线程1: 释放锁");
            }
        }, "Holder");

        // 线程2: 最多等待1秒
        Thread shortWait = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("线程2: 尝试获取锁，最多等待1秒...");

                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("线程2: 成功获取锁");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("线程2: 超时，未获取到锁");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "ShortWait");

        // 线程3: 最多等待3秒
        Thread longWait = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("线程3: 尝试获取锁，最多等待3秒...");

                if (lock.tryLock(3, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("线程3: 成功获取锁");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("线程3: 超时，未获取到锁");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "LongWait");

        holder.start();
        shortWait.start();
        longWait.start();

        try {
            holder.join();
            shortWait.join();
            longWait.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示4: lockInterruptibly - 可中断的锁获取
     * 等待锁时可以响应中断
     */
    private static void demo4_LockInterruptibly() {
        System.out.println("--- 演示4: lockInterruptibly可中断 ---");

        Lock lock = new ReentrantLock();

        // 线程1: 持有锁10秒
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("线程1: 获取锁，持有10秒");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("线程1: 被中断");
            } finally {
                lock.unlock();
            }
        }, "Holder");

        // 线程2: 可中断地等待锁
        Thread waiter = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("线程2: 可中断地等待锁...");
                lock.lockInterruptibly(); // 可被中断
                try {
                    System.out.println("线程2: 获取到锁");
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("线程2: 等待锁时被中断");
            }
        }, "Waiter");

        holder.start();
        waiter.start();

        try {
            Thread.sleep(500);
            System.out.println("主线程: 中断线程2");
            waiter.interrupt(); // 中断等待的线程

            holder.interrupt(); // 中断持有锁的线程
            holder.join();
            waiter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示5: 公平锁 vs 非公平锁
     * 公平锁按照请求顺序获取锁，非公平锁允许插队
     */
    private static void demo5_FairVsUnfair() {
        System.out.println("--- 演示5: 公平锁 vs 非公平锁 ---");

        System.out.println("\n【非公平锁】（默认，允许插队）");
        testLockFairness(new ReentrantLock(false));

        System.out.println("\n【公平锁】（按顺序获取锁）");
        testLockFairness(new ReentrantLock(true));

        System.out.println();
    }

    private static void testLockFairness(Lock lock) {
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < 2; j++) {
                    lock.lock();
                    try {
                        System.out.println("  线程" + threadId + " 获取锁");
                        Thread.sleep(10); // 模拟处理
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }, "Thread-" + i).start();
        }

        try {
            Thread.sleep(500); // 等待所有线程完成
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 演示6: 可重入性
     * 同一线程可以多次获取同一个锁
     */
    private static void demo6_Reentrant() {
        System.out.println("--- 演示6: 可重入性 ---");

        ReentrantLock lock = new ReentrantLock();

        System.out.println("主线程: 第一次获取锁");
        lock.lock();
        try {
            System.out.println("  持有计数: " + lock.getHoldCount());

            System.out.println("主线程: 第二次获取锁（重入）");
            lock.lock();
            try {
                System.out.println("  持有计数: " + lock.getHoldCount());

                System.out.println("主线程: 第三次获取锁（重入）");
                lock.lock();
                try {
                    System.out.println("  持有计数: " + lock.getHoldCount());
                } finally {
                    lock.unlock();
                    System.out.println("主线程: 第三次释放锁，持有计数: " + lock.getHoldCount());
                }
            } finally {
                lock.unlock();
                System.out.println("主线程: 第二次释放锁，持有计数: " + lock.getHoldCount());
            }
        } finally {
            lock.unlock();
            System.out.println("主线程: 第一次释放锁，持有计数: " + lock.getHoldCount());
        }

        System.out.println();
    }

    /**
     * 线程安全的计数器
     * 使用ReentrantLock保护共享变量
     */
    static class Counter {
        private int count = 0;
        private final Lock lock = new ReentrantLock();

        public void increment() {
            lock.lock();  // 获取锁
            try {
                count++;  // 临界区代码
            } finally {
                lock.unlock();  // 必须在finally中释放锁
            }
        }

        public int getCount() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }
}
