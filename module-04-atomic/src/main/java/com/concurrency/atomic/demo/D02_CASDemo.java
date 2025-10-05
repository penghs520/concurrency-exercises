package com.concurrency.atomic.demo;

import java.util.concurrent.atomic.*;

/**
 * Demo 02: CAS（Compare-And-Swap）机制深入演示
 *
 * 本示例演示：
 * 1. CAS的基本原理
 * 2. CAS的自旋重试机制
 * 3. ABA问题的演示
 * 4. AtomicStampedReference解决ABA问题
 * 5. AtomicMarkableReference的使用
 */
public class D02_CASDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CAS机制深入演示 ===\n");

        demo1_CASBasics();
        Thread.sleep(100);

        demo2_CASRetry();
        Thread.sleep(100);

        demo3_ABAProblem();
        Thread.sleep(100);

        demo4_AtomicStampedReference();
        Thread.sleep(100);

        demo5_AtomicMarkableReference();
    }

    /**
     * Demo 1: CAS基础操作
     */
    private static void demo1_CASBasics() {
        System.out.println("--- Demo 1: CAS基础操作 ---");

        AtomicInteger atomicInt = new AtomicInteger(100);

        // CAS操作：期望值匹配才更新
        System.out.println("初始值: " + atomicInt.get());

        // 期望是100，更新为200
        boolean result1 = atomicInt.compareAndSet(100, 200);
        System.out.println("CAS(100->200): " + result1 + ", 当前值: " + atomicInt.get());

        // 期望是100（但实际是200），更新失败
        boolean result2 = atomicInt.compareAndSet(100, 300);
        System.out.println("CAS(100->300): " + result2 + ", 当前值: " + atomicInt.get());

        // 期望是200，更新为300
        boolean result3 = atomicInt.compareAndSet(200, 300);
        System.out.println("CAS(200->300): " + result3 + ", 当前值: " + atomicInt.get());

        System.out.println();
    }

    /**
     * Demo 2: CAS的自旋重试机制
     */
    private static void demo2_CASRetry() throws InterruptedException {
        System.out.println("--- Demo 2: CAS自旋重试 ---");

        AtomicInteger counter = new AtomicInteger(0);

        // 自定义CAS操作：计数器+10
        Runnable task = () -> {
            int retryCount = 0;
            while (true) {
                int current = counter.get();
                int next = current + 10;

                if (counter.compareAndSet(current, next)) {
                    System.out.println(Thread.currentThread().getName()
                            + " CAS成功: " + current + " -> " + next
                            + " (重试次数: " + retryCount + ")");
                    break;
                } else {
                    retryCount++;
                    // CAS失败，继续重试
                }
            }
        };

        // 启动10个线程并发执行
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task, "Thread-" + (i + 1));
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("最终结果: " + counter.get() + " (期望: 100)");
        System.out.println();
    }

    /**
     * Demo 3: 演示ABA问题
     */
    private static void demo3_ABAProblem() throws InterruptedException {
        System.out.println("--- Demo 3: ABA问题演示 ---");

        AtomicInteger balance = new AtomicInteger(100);

        // 线程1：读取余额，准备取款50
        Thread t1 = new Thread(() -> {
            int money = balance.get();
            System.out.println("线程1读取余额: " + money);

            try {
                Thread.sleep(100); // 模拟业务处理
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 线程1苏醒，执行CAS
            boolean success = balance.compareAndSet(money, money - 50);
            System.out.println("线程1取款CAS: " + success + ", 当前余额: " + balance.get());
        }, "线程1");

        // 线程2：取出所有钱，又存回去（制造ABA）
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50); // 让线程1先读取
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("线程2取出所有钱: 100 -> 0");
            balance.compareAndSet(100, 0);

            System.out.println("线程2存入100: 0 -> 100");
            balance.compareAndSet(0, 100);
        }, "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("问题：线程1的CAS成功了，但中间状态已改变！");
        System.out.println();
    }

    /**
     * Demo 4: 使用AtomicStampedReference解决ABA问题
     */
    private static void demo4_AtomicStampedReference() throws InterruptedException {
        System.out.println("--- Demo 4: AtomicStampedReference解决ABA ---");

        // 初始值100，版本号0
        AtomicStampedReference<Integer> balanceRef =
                new AtomicStampedReference<>(100, 0);

        // 线程1：读取余额和版本号
        Thread t1 = new Thread(() -> {
            int[] stampHolder = new int[1];
            Integer money = balanceRef.get(stampHolder);
            int stamp = stampHolder[0];

            System.out.println("线程1读取: 余额=" + money + ", 版本=" + stamp);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 尝试CAS（版本号不匹配会失败）
            boolean success = balanceRef.compareAndSet(
                    money,          // 期望引用
                    money - 50,     // 新引用
                    stamp,          // 期望版本号
                    stamp + 1       // 新版本号
            );
            System.out.println("线程1取款CAS: " + success);

            int[] newStamp = new int[1];
            Integer current = balanceRef.get(newStamp);
            System.out.println("线程1看到: 余额=" + current + ", 版本=" + newStamp[0]);
        }, "线程1");

        // 线程2：制造ABA，但版本号会变化
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int[] stampHolder = new int[1];
            Integer money = balanceRef.get(stampHolder);
            int stamp = stampHolder[0];

            // 取出所有钱
            System.out.println("线程2取出: 100 -> 0 (版本" + stamp + "->" + (stamp + 1) + ")");
            balanceRef.compareAndSet(money, 0, stamp, stamp + 1);

            // 存回100
            stampHolder = new int[1];
            money = balanceRef.get(stampHolder);
            stamp = stampHolder[0];
            System.out.println("线程2存入: 0 -> 100 (版本" + stamp + "->" + (stamp + 1) + ")");
            balanceRef.compareAndSet(money, 100, stamp, stamp + 1);
        }, "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("结果：版本号机制检测到了中间修改！");
        System.out.println();
    }

    /**
     * Demo 5: AtomicMarkableReference的使用
     */
    private static void demo5_AtomicMarkableReference() {
        System.out.println("--- Demo 5: AtomicMarkableReference ---");

        // 初始值：User对象，标记false（未删除）
        User user = new User("Alice", 25);
        AtomicMarkableReference<User> userRef =
                new AtomicMarkableReference<>(user, false);

        System.out.println("初始: " + userRef.getReference() + ", 已删除=" + userRef.isMarked());

        // 标记为已删除
        boolean[] markHolder = new boolean[1];
        User currentUser = userRef.get(markHolder);
        boolean oldMark = markHolder[0];

        boolean success = userRef.compareAndSet(
                currentUser,  // 期望引用
                currentUser,  // 新引用（不变）
                oldMark,      // 期望标记：false
                true          // 新标记：true（已删除）
        );

        System.out.println("标记为已删除: " + success);
        System.out.println("当前: " + userRef.getReference() + ", 已删除=" + userRef.isMarked());

        // 尝试修改已删除的用户（应该失败）
        User newUser = new User("Bob", 30);
        success = userRef.compareAndSet(
                currentUser,
                newUser,
                false,        // 期望未删除
                false
        );

        System.out.println("尝试修改已删除用户: " + success + " (期望失败)");

        System.out.println();
    }

    /**
     * 用户类
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
}
