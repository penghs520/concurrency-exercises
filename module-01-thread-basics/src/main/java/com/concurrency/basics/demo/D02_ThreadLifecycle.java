package com.concurrency.basics.demo;

/**
 * Demo 02: 线程生命周期与状态转换
 * <p>
 * 线程的6种状态：
 * 1. NEW          - 新建
 * 2. RUNNABLE     - 可运行（就绪/运行）
 * 3. BLOCKED      - 阻塞（等待监视器锁）
 * 4. WAITING      - 等待（无限期）
 * 5. TIMED_WAITING - 限时等待
 * 6. TERMINATED   - 终止
 */
public class D02_ThreadLifecycle {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 线程生命周期演示 ===\n");

        // 演示1: NEW -> RUNNABLE -> TERMINATED
        demo1_BasicLifecycle();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示2: BLOCKED状态
        demo2_BlockedState();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示3: WAITING状态
        demo3_WaitingState();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示4: TIMED_WAITING状态
        demo4_TimedWaitingState();

        Thread.sleep(2000);
        System.out.println("\n主线程结束");
    }

    /**
     * 演示1: 基本生命周期 NEW -> RUNNABLE -> TERMINATED
     */
    private static void demo1_BasicLifecycle() throws InterruptedException {
        System.out.println("--- 演示1: 基本生命周期 ---");

        Thread thread = new Thread(() -> {
            System.out.println("线程开始执行");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程执行结束");
        }, "Lifecycle-Thread");

        // 1. NEW状态
        System.out.println("创建后状态: " + thread.getState()); // NEW

        // 2. RUNNABLE状态
        thread.start();
        System.out.println("启动后状态: " + thread.getState()); // RUNNABLE

        // 等待线程执行
        Thread.sleep(100);
        System.out.println("执行中状态: " + thread.getState()); // RUNNABLE 或 TIMED_WAITING

        // 等待线程结束
        thread.join();

        // 3. TERMINATED状态
        System.out.println("结束后状态: " + thread.getState()); // TERMINATED
    }

    /**
     * 演示2: BLOCKED状态（等待获取锁）
     */
    private static void demo2_BlockedState() throws InterruptedException {
        System.out.println("--- 演示2: BLOCKED状态 ---");

        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("T1获取到锁，持有3秒");
                try {
                    Thread.sleep(3000);//睡眠，并不释放锁，导致t2想要获取锁只能阻塞
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("T1释放锁");
            }
        }, "T1");

        Thread t2 = new Thread(() -> {
            long start = System.currentTimeMillis();
            synchronized (lock) {
                System.out.println("T2获取到锁，阻塞了：" + (System.currentTimeMillis() - start) + "ms");
            }
        }, "T2");

        t1.start();
        Thread.sleep(100); // 确保t1先获取锁

        t2.start();
        Thread.sleep(100); // 等待t2尝试获取锁

        // T2在等待锁，处于BLOCKED状态
        System.out.println("T1状态: " + t1.getState()); // TIMED_WAITING (sleep中)
        System.out.println("T2状态: " + t2.getState()); // BLOCKED (等待lock)

        t1.join();
        t2.join();
    }

    /**
     * 演示3: WAITING状态（无限期等待）
     */
    private static void demo3_WaitingState() throws InterruptedException {
        System.out.println("--- 演示3: WAITING状态 ---");

        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("线程进入等待状态");
                    lock.wait(); // 进入WAITING状态
                    System.out.println("线程被唤醒");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Waiting-Thread");

        waitingThread.start();
        Thread.sleep(500); // 等待线程进入wait

        // 查看WAITING状态
        System.out.println("等待线程状态: " + waitingThread.getState()); // WAITING

        // 唤醒线程
        //lock.notify();  必须先获得锁，才有资格通知，否则抛出异常：java.lang.IllegalMonitorStateException: current thread is not owner
        synchronized (lock) {
            System.out.println("主线程唤醒等待线程");
            lock.notify();
        }

        waitingThread.join();
    }

    /**
     * 演示4: TIMED_WAITING状态（限时等待）
     */
    private static void demo4_TimedWaitingState() throws InterruptedException {
        System.out.println("--- 演示4: TIMED_WAITING状态 ---");

        // 方式1: Thread.sleep()
        Thread sleepThread = new Thread(() -> {
            try {
                System.out.println("Sleep线程: 休眠2秒");
                Thread.sleep(2000);
                System.out.println("Sleep线程: 醒来");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Sleep-Thread");

        sleepThread.start();
        Thread.sleep(100);
        System.out.println("Sleep线程状态: " + sleepThread.getState()); // TIMED_WAITING

        // 方式2: Object.wait(timeout)
        Thread waitTimeoutThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("WaitTimeout线程: 等待1秒");
                    lock.wait(1000);
                    System.out.println("WaitTimeout线程: 超时返回");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "WaitTimeout-Thread");

        waitTimeoutThread.start();
        Thread.sleep(100);
        System.out.println("WaitTimeout线程状态: " + waitTimeoutThread.getState()); // TIMED_WAITING

        // 方式3: Thread.join(timeout)
        Thread joinThread = new Thread(() -> {
            try {
                System.out.println("Join线程: 等待sleep线程最多1秒");
                sleepThread.join(1000);
                System.out.println("Join线程: 等待结束");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Join-Thread");

        joinThread.start();
        Thread.sleep(100);
        System.out.println("Join线程状态: " + joinThread.getState()); // TIMED_WAITING

        // 等待所有线程结束
        sleepThread.join();
        waitTimeoutThread.join();
        joinThread.join();
    }

    /**
     * 打印线程详细信息
     */
    private static void printThreadInfo(Thread thread) {
        System.out.println("线程名称: " + thread.getName());
        System.out.println("线程ID: " + thread.getId());
        System.out.println("线程状态: " + thread.getState());
        System.out.println("是否存活: " + thread.isAlive());
        System.out.println("优先级: " + thread.getPriority());
        System.out.println("是否守护线程: " + thread.isDaemon());
        System.out.println();
    }
}
