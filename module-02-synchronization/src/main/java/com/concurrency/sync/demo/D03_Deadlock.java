package com.concurrency.sync.demo;

/**
 * Demo 03: 死锁演示与避免
 *
 * 演示内容：
 * 1. 制造死锁
 * 2. 使用jstack检测死锁
 * 3. 通过锁排序避免死锁
 */
public class D03_Deadlock {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Demo 1: 制造死锁 ===");
        System.out.println("提示: 程序会卡住，使用 Ctrl+C 终止，或使用jstack检测");
        System.out.println("命令: jps 找到进程ID，然后 jstack <pid>");
        System.out.println();

        // 取消注释以下代码演示死锁（会卡住）
        // demoDeadlock();

        System.out.println("=== Demo 2: 避免死锁（锁排序） ===");
        demoAvoidDeadlock();
    }

    // ==================== Demo 1: 制造死锁 ====================

    static class DeadlockDemo {
        private final Object lock1 = new Object();
        private final Object lock2 = new Object();

        public void method1() {
            synchronized (lock1) {
                System.out.println(Thread.currentThread().getName() + ": 获得lock1");

                try {
                    Thread.sleep(100);  // 增加死锁概率
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println(Thread.currentThread().getName() + ": 等待lock2...");
                synchronized (lock2) {
                    System.out.println(Thread.currentThread().getName() + ": 获得lock2");
                }
            }
        }

        public void method2() {
            synchronized (lock2) {
                System.out.println(Thread.currentThread().getName() + ": 获得lock2");

                try {
                    Thread.sleep(100);  // 增加死锁概率
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println(Thread.currentThread().getName() + ": 等待lock1...");
                synchronized (lock1) {
                    System.out.println(Thread.currentThread().getName() + ": 获得lock1");
                }
            }
        }
    }

    private static void demoDeadlock() throws InterruptedException {
        DeadlockDemo demo = new DeadlockDemo();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                demo.method1();
            }
        }, "线程A");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                demo.method2();
            }
        }, "线程B");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    // ==================== Demo 2: 通过锁排序避免死锁 ====================

    static class Account {
        private final int id;
        private int balance;

        public Account(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }

        public int getId() {
            return id;
        }

        public int getBalance() {
            return balance;
        }

        public void debit(int amount) {
            balance -= amount;
        }

        public void credit(int amount) {
            balance += amount;
        }
    }

    // ❌ 错误的转账方法（可能死锁）
    static class UnsafeTransfer {
        public void transfer(Account from, Account to, int amount) {
            synchronized (from) {
                System.out.println(Thread.currentThread().getName() +
                        ": 锁定账户 " + from.getId());

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (to) {
                    System.out.println(Thread.currentThread().getName() +
                            ": 锁定账户 " + to.getId());
                    from.debit(amount);
                    to.credit(amount);
                    System.out.println(Thread.currentThread().getName() +
                            ": 转账完成 " + from.getId() + " -> " + to.getId());
                }
            }
        }
    }

    // ✅ 正确的转账方法（锁排序避免死锁）
    static class SafeTransfer {
        public void transfer(Account from, Account to, int amount) {
            // 关键：始终按照账户ID的顺序获取锁
            Account first = from.getId() < to.getId() ? from : to;
            Account second = first == from ? to : from;

            synchronized (first) {
                System.out.println(Thread.currentThread().getName() +
                        ": 锁定账户 " + first.getId());

                synchronized (second) {
                    System.out.println(Thread.currentThread().getName() +
                            ": 锁定账户 " + second.getId());

                    from.debit(amount);
                    to.credit(amount);

                    System.out.println(Thread.currentThread().getName() +
                            ": 转账完成 " + from.getId() + " -> " + to.getId() +
                            " (from余额:" + from.getBalance() +
                            ", to余额:" + to.getBalance() + ")");
                }
            }
        }
    }

    private static void demoAvoidDeadlock() throws InterruptedException {
        Account account1 = new Account(1, 1000);
        Account account2 = new Account(2, 1000);

        SafeTransfer safeTransfer = new SafeTransfer();

        // 线程A: 从账户1转到账户2
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                safeTransfer.transfer(account1, account2, 100);
            }
        }, "线程A");

        // 线程B: 从账户2转到账户1
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                safeTransfer.transfer(account2, account1, 50);
            }
        }, "线程B");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("\n最终余额:");
        System.out.println("账户1: " + account1.getBalance());
        System.out.println("账户2: " + account2.getBalance());
        System.out.println("总额: " + (account1.getBalance() + account2.getBalance()));
        System.out.println("\n结论: 通过锁排序（Lock Ordering）成功避免死锁");
    }

    // ==================== 检测死锁的方法 ====================

    /**
     * 使用jstack检测死锁的步骤：
     *
     * 1. 找到Java进程ID:
     *    jps
     *
     * 2. 生成线程dump:
     *    jstack <pid> > thread_dump.txt
     *
     * 3. 查找死锁信息:
     *    grep -A 20 "Found one Java-level deadlock" thread_dump.txt
     *
     * 4. jstack输出示例:
     *    Found one Java-level deadlock:
     *    =============================
     *    "线程B":
     *      waiting to lock monitor 0x00007f8c1c004e00 (object 0x000000076ab3e6d0, a java.lang.Object),
     *      which is held by "线程A"
     *    "线程A":
     *      waiting to lock monitor 0x00007f8c1c007350 (object 0x000000076ab3e6e0, a java.lang.Object),
     *      which is held by "线程B"
     *
     * 5. 使用JConsole或VisualVM图形化检测:
     *    - 启动JConsole: jconsole
     *    - 连接到Java进程
     *    - 查看"线程"选项卡
     *    - 点击"检测死锁"按钮
     */
}
