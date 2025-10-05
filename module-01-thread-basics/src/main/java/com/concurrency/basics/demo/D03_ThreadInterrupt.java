package com.concurrency.basics.demo;

/**
 * Demo 03: 线程中断机制详解
 *
 * 中断是Java提供的一种协作机制，用于优雅地停止线程。
 *
 * 核心方法：
 * - interrupt()       : 请求中断线程
 * - isInterrupted()   : 检查是否被中断（不清除标志）
 * - interrupted()     : 检查并清除中断标志（静态方法）
 */
public class D03_ThreadInterrupt {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 线程中断机制演示 ===\n");

        // 演示1: 基本中断使用
        demo1_BasicInterrupt();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示2: 响应中断的正确方式
        demo2_RespondToInterrupt();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示3: 阻塞方法的中断
        demo3_InterruptBlockingMethod();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示4: 中断标志的恢复
        demo4_RestoreInterruptStatus();

        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 演示5: 错误的中断处理
        demo5_WrongInterruptHandling();

        System.out.println("\n主线程结束");
    }

    /**
     * 演示1: 基本中断使用
     */
    private static void demo1_BasicInterrupt() throws InterruptedException {
        System.out.println("--- 演示1: 基本中断使用 ---");

        Thread worker = new Thread(() -> {
            System.out.println("工作线程开始");
            int count = 0;

            // 检查中断标志
            while (!Thread.currentThread().isInterrupted()) {
                count++;
                if (count % 100_000_000 == 0) {
                    System.out.println("计数: " + count);
                }
            }

            System.out.println("工作线程检测到中断，准备退出");
        }, "Worker");

        worker.start();
        Thread.sleep(100); // 让工作线程运行一会儿

        System.out.println("主线程请求中断");
        worker.interrupt(); // 请求中断

        worker.join();
        System.out.println("工作线程已退出");
    }

    /**
     * 演示2: 响应中断的正确方式
     */
    private static void demo2_RespondToInterrupt() throws InterruptedException {
        System.out.println("--- 演示2: 响应中断的正确方式 ---");

        Thread task = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("执行任务...");
                    Thread.sleep(500); // 阻塞方法会抛出InterruptedException
                }
            } catch (InterruptedException e) {
                System.out.println("捕获到中断异常: " + e.getMessage());
                // 恢复中断状态（重要！）
                Thread.currentThread().interrupt();
            } finally {
                // 清理资源
                System.out.println("清理资源");
            }
        }, "Task");

        task.start();
        Thread.sleep(1200); // 让任务执行2-3次

        System.out.println("主线程请求中断");
        task.interrupt();

        task.join();
        System.out.println("中断标志: " + task.isInterrupted());
    }

    /**
     * 演示3: 阻塞方法的中断
     * sleep/wait/join等方法会响应中断
     */
    private static void demo3_InterruptBlockingMethod() throws InterruptedException {
        System.out.println("--- 演示3: 阻塞方法的中断 ---");

        // 场景1: 中断sleep
        Thread sleepingThread = new Thread(() -> {
            try {
                System.out.println("线程准备休眠5秒");
                Thread.sleep(5000);
                System.out.println("休眠完成"); // 不会执行
            } catch (InterruptedException e) {
                System.out.println("休眠被中断: " + e.getMessage());
                // 注意: 捕获异常后，中断标志被清除
                System.out.println("捕获异常后中断标志: " + Thread.currentThread().isInterrupted()); // false
            }
        }, "Sleeping-Thread");

        sleepingThread.start();
        Thread.sleep(500); // 等待线程进入休眠

        System.out.println("主线程中断休眠线程");
        sleepingThread.interrupt();

        sleepingThread.join();

        // 场景2: 中断wait
        Object lock = new Object();
        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("线程进入等待");
                    lock.wait(); // 无限等待
                    System.out.println("等待结束"); // 不会执行
                } catch (InterruptedException e) {
                    System.out.println("等待被中断");
                }
            }
        }, "Waiting-Thread");

        waitingThread.start();
        Thread.sleep(500);

        System.out.println("主线程中断等待线程");
        waitingThread.interrupt();

        waitingThread.join();
    }

    /**
     * 演示4: 中断标志的恢复
     * 当捕获InterruptedException后，中断标志会被清除，
     * 如果需要向上传递中断状态，应该恢复中断标志
     */
    private static void demo4_RestoreInterruptStatus() throws InterruptedException {
        System.out.println("--- 演示4: 中断标志的恢复 ---");

        Thread task = new Thread(() -> {
            try {
                doWork();
            } catch (InterruptedException e) {
                System.out.println("任务被中断");
            }
        }, "Task");

        task.start();
        Thread.sleep(200);

        System.out.println("主线程发送中断");
        task.interrupt();

        task.join();
    }

    /**
     * 模拟工作方法 - 正确恢复中断状态
     */
    private static void doWork() throws InterruptedException {
        try {
            System.out.println("开始工作");
            Thread.sleep(1000);
            System.out.println("工作完成");
        } catch (InterruptedException e) {
            System.out.println("工作中被中断");
            // 恢复中断状态，让调用者知道发生了中断
            Thread.currentThread().interrupt();
            throw e; // 向上传递异常
        }
    }

    /**
     * 演示5: 错误的中断处理方式
     */
    private static void demo5_WrongInterruptHandling() throws InterruptedException {
        System.out.println("--- 演示5: 错误的中断处理 ---");

        // 错误1: 吞掉InterruptedException
        Thread badThread1 = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ✗ 什么都不做，吞掉异常
                // 这会导致中断信号丢失！
            }
            System.out.println("错误示例1: 中断被吞掉");
        }, "Bad-Thread-1");

        badThread1.start();
        Thread.sleep(100);
        badThread1.interrupt();
        badThread1.join();

        // 错误2: 使用已废弃的stop()方法
        Thread badThread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                System.out.println("计数: " + i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Bad-Thread-2");

        badThread2.start();
        Thread.sleep(100);

        // ✗ 不推荐使用stop()，已被废弃
        // badThread2.stop(); // 会立即终止线程，可能导致数据不一致

        // ✓ 正确做法：使用interrupt()
        badThread2.interrupt();
        badThread2.join();

        System.out.println("\n正确做法：");
        System.out.println("1. 捕获InterruptedException后，重新设置中断标志");
        System.out.println("2. 或者向上抛出异常，让调用者处理");
        System.out.println("3. 不要使用stop()、suspend()、resume()等已废弃方法");
    }

    /**
     * 工具方法: 打印中断状态
     */
    private static void printInterruptStatus() {
        Thread current = Thread.currentThread();
        System.out.println("线程: " + current.getName());
        System.out.println("中断标志: " + current.isInterrupted());
        System.out.println();
    }
}
