package com.concurrency.atomic.demo;

import java.util.concurrent.atomic.*;

/**
 * Demo 01: 原子类基础操作
 *
 * 本示例演示：
 * 1. AtomicInteger基本用法
 * 2. AtomicLong的使用
 * 3. AtomicBoolean的典型场景
 * 4. AtomicReference引用类型
 */
public class D01_AtomicBasics {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 原子类基础操作演示 ===\n");

        demo1_AtomicInteger();
        Thread.sleep(100);

        demo2_AtomicBoolean();
        Thread.sleep(100);

        demo3_AtomicReference();
        Thread.sleep(100);

        demo4_CompareWithSynchronized();
    }

    /**
     * Demo 1: AtomicInteger基础操作
     */
    private static void demo1_AtomicInteger() {
        System.out.println("--- Demo 1: AtomicInteger基础操作 ---");

        AtomicInteger count = new AtomicInteger(0);

        // 基本操作
        System.out.println("初始值: " + count.get());                    // 0
        count.set(10);
        System.out.println("设置后: " + count.get());                    // 10

        // 自增自减
        System.out.println("incrementAndGet: " + count.incrementAndGet()); // 11 (++i)
        System.out.println("getAndIncrement: " + count.getAndIncrement()); // 11 (i++)
        System.out.println("当前值: " + count.get());                     // 12

        System.out.println("decrementAndGet: " + count.decrementAndGet()); // 11 (--i)
        System.out.println("getAndDecrement: " + count.getAndDecrement()); // 11 (i--)
        System.out.println("当前值: " + count.get());                     // 10

        // 加减操作
        System.out.println("addAndGet(5): " + count.addAndGet(5));        // 15
        System.out.println("getAndAdd(-3): " + count.getAndAdd(-3));      // 15
        System.out.println("当前值: " + count.get());                     // 12

        // CAS操作
        boolean success = count.compareAndSet(12, 100);
        System.out.println("CAS(12->100): " + success);                   // true
        System.out.println("当前值: " + count.get());                     // 100

        success = count.compareAndSet(12, 200);
        System.out.println("CAS(12->200): " + success);                   // false（期望值不匹配）

        // 函数式更新（Java 8+）
        count.set(10);
        int result = count.updateAndGet(x -> x * 2);
        System.out.println("updateAndGet(x*2): " + result);               // 20

        result = count.accumulateAndGet(5, (x, y) -> x + y);
        System.out.println("accumulateAndGet(+5): " + result);            // 25

        System.out.println();
    }

    /**
     * Demo 2: AtomicBoolean典型场景
     */
    private static void demo2_AtomicBoolean() {
        System.out.println("--- Demo 2: AtomicBoolean单次初始化 ---");

        AtomicBoolean initialized = new AtomicBoolean(false);

        // 模拟多线程环境
        Runnable initTask = () -> {
            if (initialized.compareAndSet(false, true)) {
                System.out.println(Thread.currentThread().getName() + " 执行初始化");
                try {
                    Thread.sleep(50); // 模拟初始化耗时操作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 初始化完成");
            } else {
                System.out.println(Thread.currentThread().getName() + " 跳过初始化（已完成）");
            }
        };

        // 启动3个线程，只有一个会执行初始化
        Thread t1 = new Thread(initTask, "Thread-1");
        Thread t2 = new Thread(initTask, "Thread-2");
        Thread t3 = new Thread(initTask, "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * Demo 3: AtomicReference引用类型
     */
    private static void demo3_AtomicReference() {
        System.out.println("--- Demo 3: AtomicReference引用类型 ---");

        AtomicReference<User> userRef = new AtomicReference<>(new User("Alice", 25));

        System.out.println("初始用户: " + userRef.get());

        // CAS更新整个对象
        User oldUser = userRef.get();
        User newUser = new User("Bob", 30);
        boolean success = userRef.compareAndSet(oldUser, newUser);
        System.out.println("CAS更新用户: " + success);
        System.out.println("当前用户: " + userRef.get());

        // 函数式更新（创建新的不可变对象）
        userRef.updateAndGet(user -> new User(user.name, user.age + 1));
        System.out.println("年龄+1后: " + userRef.get());

        // getAndSet
        User previous = userRef.getAndSet(new User("Charlie", 35));
        System.out.println("getAndSet返回: " + previous);
        System.out.println("当前用户: " + userRef.get());

        System.out.println();
    }

    /**
     * Demo 4: 对比synchronized性能
     */
    private static void demo4_CompareWithSynchronized() throws InterruptedException {
        System.out.println("--- Demo 4: AtomicInteger vs synchronized 性能对比 ---");

        int threadCount = 10;
        int iterations = 100_000;

        // 测试AtomicInteger
        AtomicInteger atomicCounter = new AtomicInteger(0);
        long start1 = System.currentTimeMillis();

        Thread[] threads1 = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads1[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    atomicCounter.incrementAndGet();
                }
            });
            threads1[i].start();
        }

        for (Thread t : threads1) {
            t.join();
        }

        long time1 = System.currentTimeMillis() - start1;
        System.out.println("AtomicInteger: 结果=" + atomicCounter.get() + ", 耗时=" + time1 + "ms");

        // 测试synchronized
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        long start2 = System.currentTimeMillis();

        Thread[] threads2 = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads2[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    syncCounter.increment();
                }
            });
            threads2[i].start();
        }

        for (Thread t : threads2) {
            t.join();
        }

        long time2 = System.currentTimeMillis() - start2;
        System.out.println("synchronized: 结果=" + syncCounter.getCount() + ", 耗时=" + time2 + "ms");

        System.out.println("性能提升: " + String.format("%.2f", (double) time2 / time1) + "x");
    }

    /**
     * 用户类（不可变对象）
     */
    static class User {
        final String name;
        final int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + "}";
        }
    }

    /**
     * synchronized版本的计数器
     */
    static class SynchronizedCounter {
        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }
}
