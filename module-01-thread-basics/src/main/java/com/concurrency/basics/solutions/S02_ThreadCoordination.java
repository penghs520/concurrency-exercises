package com.concurrency.basics.solutions;

/**
 * 练习2参考答案: 线程协调与顺序执行
 *
 * 提供三种实现方式：
 * 1. 使用join()方法（推荐）
 * 2. 使用共享标志位
 * 3. 在线程内部使用join()
 */
public class S02_ThreadCoordination {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 线程协调与顺序执行（参考答案） ===\n");

        System.out.println("--- 方式1: 主线程控制join() ---");
        method1_MainThreadJoin();

        Thread.sleep(500);
        System.out.println("\n" + "=".repeat(40) + "\n");

        System.out.println("--- 方式2: 线程内部join() ---");
        method2_InternalJoin();

        Thread.sleep(500);
        System.out.println("\n" + "=".repeat(40) + "\n");

        System.out.println("--- 方式3: 共享标志位 ---");
        method3_UseFlag();

        Thread.sleep(500);
        System.out.println("\n" + "=".repeat(40) + "\n");

        System.out.println("--- 方式4: 使用Object.wait/notify (预习) ---");
        method4_WaitNotify();
    }

    /**
     * 方式1: 主线程按顺序启动并等待
     * 优点: 简单直观，主线程完全控制执行顺序
     * 缺点: 主线程需要等待，不够灵活
     */
    private static void method1_MainThreadJoin() throws InterruptedException {
        Thread t1 = new Thread(() -> System.out.println("First"), "T1");
        Thread t2 = new Thread(() -> System.out.println("Second"), "T2");
        Thread t3 = new Thread(() -> System.out.println("Third"), "T3");

        // 按顺序启动并等待
        t1.start();
        t1.join(); // 等待t1完成

        t2.start();
        t2.join(); // 等待t2完成

        t3.start();
        t3.join(); // 等待t3完成
    }

    /**
     * 方式2: 线程内部调用前置线程的join()
     * 优点: 各线程自己控制依赖关系，解耦主线程
     * 缺点: 需要传递线程引用
     */
    private static void method2_InternalJoin() throws InterruptedException {
        // 使用PrintTask类
        Thread t1 = new Thread(new PrintTask("First", null), "T1");
        Thread t2 = new Thread(new PrintTask("Second", t1), "T2");
        Thread t3 = new Thread(new PrintTask("Third", t2), "T3");

        // 可以任意顺序启动
        t3.start();
        t1.start();
        t2.start();

        // 只需等待最后一个线程
        t3.join();
    }

    /**
     * 方式3: 使用共享标志位（volatile）
     * 优点: 灵活，可以实现复杂的协调逻辑
     * 缺点: 自旋等待消耗CPU
     */
    private static void method3_UseFlag() throws InterruptedException {
        StageController controller = new StageController();

        Thread t1 = new Thread(() -> {
            controller.waitForStage(0);
            System.out.println("First");
            controller.nextStage();
        }, "T1");

        Thread t2 = new Thread(() -> {
            controller.waitForStage(1);
            System.out.println("Second");
            controller.nextStage();
        }, "T2");

        Thread t3 = new Thread(() -> {
            controller.waitForStage(2);
            System.out.println("Third");
            controller.nextStage();
        }, "T3");

        // 任意顺序启动
        t2.start();
        t3.start();
        t1.start();

        // 等待所有完成
        t1.join();
        t2.join();
        t3.join();
    }

    /**
     * 方式4: 使用wait/notify机制（下个模块学习）
     * 优点: 不消耗CPU，效率高
     * 缺点: 代码复杂
     */
    private static void method4_WaitNotify() throws InterruptedException {
        Object lock1 = new Object();
        Object lock2 = new Object();

        Thread t1 = new Thread(() -> {
            System.out.println("First");
            synchronized (lock1) {
                lock1.notify(); // 唤醒t2
            }
        }, "T1");

        Thread t2 = new Thread(() -> {
            synchronized (lock1) {
                try {
                    lock1.wait(); // 等待t1唤醒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("Second");
            synchronized (lock2) {
                lock2.notify(); // 唤醒t3
            }
        }, "T2");

        Thread t3 = new Thread(() -> {
            synchronized (lock2) {
                try {
                    lock2.wait(); // 等待t2唤醒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("Third");
        }, "T3");

        // 必须按顺序启动，确保wait先于notify
        t3.start();
        Thread.sleep(100);
        t2.start();
        Thread.sleep(100);
        t1.start();

        t1.join();
        t2.join();
        t3.join();
    }

    /**
     * 打印任务（方式2使用）
     */
    static class PrintTask implements Runnable {
        private final String message;
        private final Thread previousThread;

        public PrintTask(String message, Thread previousThread) {
            this.message = message;
            this.previousThread = previousThread;
        }

        @Override
        public void run() {
            // 等待前置线程完成
            if (previousThread != null) {
                try {
                    previousThread.join();
                } catch (InterruptedException e) {
                    System.err.println(Thread.currentThread().getName() + " 被中断");
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // 打印消息
            System.out.println(message);
        }
    }

    /**
     * 阶段控制器（方式3使用）
     */
    static class StageController {
        private volatile int currentStage = 0;

        /**
         * 等待指定阶段
         */
        public void waitForStage(int stage) {
            while (currentStage != stage) {
                Thread.yield(); // 让出CPU，减少自旋消耗
            }
        }

        /**
         * 进入下一个阶段
         */
        public void nextStage() {
            currentStage++;
        }
    }
}

/**
 * 【方案对比】
 *
 * | 方式 | 优点 | 缺点 | 适用场景 |
 * |------|------|------|----------|
 * | 主线程join | 简单直观 | 主线程阻塞 | 简单的顺序执行 |
 * | 内部join | 解耦主线程 | 需要传递引用 | 链式依赖关系 |
 * | 共享标志 | 灵活 | 自旋消耗CPU | 复杂协调逻辑 |
 * | wait/notify | 高效 | 代码复杂 | 高性能要求 |
 *
 * 【推荐】
 * - 简单场景：使用join()（方式1或2）
 * - 复杂场景：使用并发工具类（CountDownLatch、CyclicBarrier等，后续学习）
 *
 * 【扩展思考】
 * 1. 如果要实现T1、T2并行执行，T3等待两者都完成后执行？
 *    答：使用CountDownLatch或两次join()
 *
 * 2. 如果要循环执行T1->T2->T3多次？
 *    答：使用CyclicBarrier（可重用的屏障）
 *
 * 3. 如果有多个线程等待同一个条件？
 *    答：使用Condition或Semaphore
 */
