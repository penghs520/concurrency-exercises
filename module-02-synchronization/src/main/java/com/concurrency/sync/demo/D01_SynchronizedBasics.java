package com.concurrency.sync.demo;

/**
 * Demo 01: synchronized基础用法
 *
 * 演示内容：
 * 1. 不加锁的线程安全问题
 * 2. 同步方法
 * 3. 同步代码块
 * 4. 类锁 vs 对象锁
 * 5. 可重入性
 */
public class D01_SynchronizedBasics {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Demo 1: 线程安全问题 ===");
        demoThreadSafetyIssue();

        System.out.println("\n=== Demo 2: 同步方法 ===");
        demoSynchronizedMethod();

        System.out.println("\n=== Demo 3: 同步代码块 ===");
        demoSynchronizedBlock();

        System.out.println("\n=== Demo 4: 类锁 vs 对象锁 ===");
        demoClassLockVsObjectLock();

        System.out.println("\n=== Demo 5: 可重入性 ===");
        demoReentrant();
    }

    // ==================== Demo 1: 线程安全问题 ====================

    static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++;  // 非原子操作！
        }

        public int getCount() {
            return count;
        }
    }

    private static void demoThreadSafetyIssue() throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();

        // 创建10个线程，每个线程执行1000次increment
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("期望结果: 10000");
        System.out.println("实际结果: " + counter.getCount());
        System.out.println("结论: 没有同步保护，结果不正确！");
    }

    // ==================== Demo 2: 同步方法 ====================

    static class SafeCounter {
        private int count = 0;

        // 同步实例方法：锁的是this对象
        public synchronized void increment() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }

    private static void demoSynchronizedMethod() throws InterruptedException {
        SafeCounter counter = new SafeCounter();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("期望结果: 10000");
        System.out.println("实际结果: " + counter.getCount());
        System.out.println("结论: 使用synchronized后，结果正确！");
    }

    // ==================== Demo 3: 同步代码块 ====================

    static class BlockCounter {
        private int count = 0;
        private final Object lock = new Object();  // 私有锁对象

        public void increment() {
            // 只对临界区代码加锁，缩小锁范围
            synchronized (lock) {
                count++;
            }
        }

        public int getCount() {
            synchronized (lock) {
                return count;
            }
        }
    }

    private static void demoSynchronizedBlock() throws InterruptedException {
        BlockCounter counter = new BlockCounter();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("使用私有锁对象的结果: " + counter.getCount());
        System.out.println("结论: 同步代码块提供更细粒度的控制");
    }

    // ==================== Demo 4: 类锁 vs 对象锁 ====================

    static class LockDemo {
        // 同步静态方法：锁的是LockDemo.class对象（类锁）
        public static synchronized void staticMethod(String threadName) {
            System.out.println(threadName + " 进入静态同步方法");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(threadName + " 离开静态同步方法");
        }

        // 同步实例方法：锁的是this对象（对象锁）
        public synchronized void instanceMethod(String threadName) {
            System.out.println(threadName + " 进入实例同步方法");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(threadName + " 离开实例同步方法");
        }
    }

    private static void demoClassLockVsObjectLock() throws InterruptedException {
        LockDemo obj1 = new LockDemo();
        LockDemo obj2 = new LockDemo();

        // 测试类锁：两个线程调用静态方法，互斥执行
        Thread t1 = new Thread(() -> LockDemo.staticMethod("线程1"));
        Thread t2 = new Thread(() -> LockDemo.staticMethod("线程2"));

        System.out.println("测试类锁（静态方法）：");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n测试对象锁（同一对象）：");
        // 同一对象的两个线程，互斥执行
        Thread t3 = new Thread(() -> obj1.instanceMethod("线程3"));
        Thread t4 = new Thread(() -> obj1.instanceMethod("线程4"));
        t3.start();
        t4.start();
        t3.join();
        t4.join();

        System.out.println("\n测试对象锁（不同对象）：");
        // 不同对象的两个线程，并发执行
        Thread t5 = new Thread(() -> obj1.instanceMethod("线程5"));
        Thread t6 = new Thread(() -> obj2.instanceMethod("线程6"));
        t5.start();
        t6.start();
        t5.join();
        t6.join();

        System.out.println("结论: 类锁全局唯一，对象锁每个实例一个");
    }

    // ==================== Demo 5: 可重入性 ====================

    static class ReentrantDemo {
        private int count = 0;

        public synchronized void outer() {
            System.out.println("进入outer方法，count = " + count);
            count++;
            inner();  // 同一线程再次获取this锁（可重入）
            System.out.println("离开outer方法，count = " + count);
        }

        public synchronized void inner() {
            System.out.println("  进入inner方法，count = " + count);
            count++;
            System.out.println("  离开inner方法，count = " + count);
        }
    }

    private static void demoReentrant() {
        ReentrantDemo demo = new ReentrantDemo();
        demo.outer();
        System.out.println("结论: synchronized是可重入锁");
    }
}
