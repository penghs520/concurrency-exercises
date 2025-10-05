package com.concurrency.basics.exercises;

/**
 * 练习2: 线程协调与顺序执行 🟡
 *
 * 【题目描述】
 * 有三个线程T1、T2、T3，要求：
 * 1. T1打印 "First"
 * 2. T2在T1执行完后打印 "Second"
 * 3. T3在T2执行完后打印 "Third"
 * 要求使用线程协调机制确保按顺序输出。
 *
 * 【要求】
 * 1. 使用join()方法实现（方式1）
 * 2. 使用interrupt()机制实现（方式2 - 挑战）
 * 3. 最终输出必须是: First -> Second -> Third
 *
 * 【学习目标】
 * - 掌握join()方法
 * - 理解线程间协作
 * - 掌握线程执行顺序控制
 *
 * 【难度】: 🟡 中级
 */
public class E02_ThreadCoordination {

    public static void main(String[] args) {
        System.out.println("=== 线程协调与顺序执行 ===\n");

        System.out.println("--- 方式1: 使用join()实现 ---");
        method1_UseJoin();

        System.out.println("\n" + "=".repeat(40) + "\n");

        System.out.println("--- 方式2: 使用共享标志实现 ---");
        method2_UseFlag();
    }

    /**
     * 方式1: 使用join()实现线程顺序执行
     * TODO: 完成此方法
     *
     * 提示：
     * 1. 创建三个线程T1、T2、T3
     * 2. T1.start() -> T1.join() -> T2.start() -> T2.join() -> T3.start()
     * 3. 或者在T2中调用T1.join()，在T3中调用T2.join()
     */
    private static void method1_UseJoin() {
        // TODO: 创建PrintTask实例
        // PrintTask task1 = new PrintTask("First", null);
        // PrintTask task2 = new PrintTask("Second", t1);
        // PrintTask task3 = new PrintTask("Third", t2);

        // TODO: 创建并启动线程
        // Thread t1 = new Thread(task1, "T1");
        // Thread t2 = new Thread(task2, "T2");
        // Thread t3 = new Thread(task3, "T3");

        // TODO: 按顺序启动
        // t1.start();
        // t2.start();
        // t3.start();

        // TODO: 等待所有线程完成
        // try {
        //     t3.join(); // 等待t3完成即可（t3会等t2，t2会等t1）
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
    }

    /**
     * 方式2: 使用共享标志位实现
     * TODO: 完成此方法
     *
     * 提示：
     * 1. 使用volatile int类型的阶段标志
     * 2. T1等待stage==0，执行后设置stage=1
     * 3. T2等待stage==1，执行后设置stage=2
     * 4. T3等待stage==2，执行后设置stage=3
     */
    private static void method2_UseFlag() {
        // TODO: 创建共享的阶段控制对象
        // StageController controller = new StageController();

        // TODO: 创建三个任务
        // Thread t1 = new Thread(() -> {
        //     controller.waitForStage(0);
        //     System.out.println("First");
        //     controller.nextStage();
        // }, "T1");

        // TODO: 创建T2和T3类似的逻辑

        // TODO: 启动所有线程（顺序无关）
        // t3.start();
        // t1.start();
        // t2.start();

        // TODO: 等待完成
    }

    /**
     * 打印任务
     * TODO: 完成此类
     */
    static class PrintTask implements Runnable {
        private final String message;
        private final Thread previousThread; // 需要等待的前置线程

        public PrintTask(String message, Thread previousThread) {
            this.message = message;
            this.previousThread = previousThread;
        }

        @Override
        public void run() {
            // TODO: 如果有前置线程，先等待它完成
            // 提示: 使用previousThread.join()

            // TODO: 打印消息
            System.out.println(message);
        }
    }

    /**
     * 阶段控制器
     * TODO: 完成此类（方式2使用）
     */
    static class StageController {
        private volatile int currentStage = 0;

        /**
         * 等待指定阶段
         * TODO: 实现此方法
         *
         * @param stage 要等待的阶段
         */
        public void waitForStage(int stage) {
            // TODO: 自旋等待currentStage == stage
            // 提示: while (currentStage != stage) { Thread.yield(); }
        }

        /**
         * 进入下一个阶段
         * TODO: 实现此方法
         */
        public void nextStage() {
            // TODO: currentStage++
        }
    }
}

/**
 * 【参考输出】
 * === 线程协调与顺序执行 ===
 *
 * --- 方式1: 使用join()实现 ---
 * First
 * Second
 * Third
 *
 * ========================================
 *
 * --- 方式2: 使用共享标志实现 ---
 * First
 * Second
 * Third
 *
 * 【扩展思考】
 * 1. 如果有10个线程需要顺序执行，用哪种方式更好？
 * 2. 能否用wait/notify实现？（下个模块学习）
 * 3. 能否用CountDownLatch实现？（后续模块学习）
 */
