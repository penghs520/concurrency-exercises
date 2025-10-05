package com.concurrency.advanced.demo;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

/**
 * Demo 03: Phaser演示
 *
 * 本示例演示：
 * 1. 多阶段同步
 * 2. 动态注册/注销参与者
 * 3. 自定义阶段完成行为
 * 4. 实际应用：多阶段数据处理
 */
public class D03_Phaser {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Phaser演示 ===\n");

        // Demo 1: 基本用法
        demo1_BasicUsage();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 2: 动态参与者
        demo2_DynamicParties();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 3: 自定义阶段行为
        demo3_CustomPhaseAdvance();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 4: 实际应用 - 多阶段数据处理
        demo4_MultiPhaseProcessing();
    }

    /**
     * Demo 1: Phaser基本用法
     */
    private static void demo1_BasicUsage() throws InterruptedException {
        System.out.println("--- Demo 1: Phaser基本用法 ---\n");

        int numThreads = 3;
        Phaser phaser = new Phaser(numThreads);

        for (int i = 0; i < numThreads; i++) {
            int workerId = i;
            new Thread(() -> {
                // 阶段1
                System.out.println("Worker-" + workerId + " 完成阶段1");
                phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段1

                // 阶段2
                System.out.println("Worker-" + workerId + " 完成阶段2");
                phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段2

                // 阶段3
                System.out.println("Worker-" + workerId + " 完成阶段3");
                phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段3

                System.out.println("Worker-" + workerId + " 全部完成");
            }, "Worker-" + i).start();
        }

        Thread.sleep(2000);
    }

    /**
     * Demo 2: 动态注册/注销参与者
     */
    private static void demo2_DynamicParties() throws InterruptedException {
        System.out.println("--- Demo 2: 动态参与者 ---\n");

        Phaser phaser = new Phaser(1); // 主线程

        System.out.println("初始参与者数: " + phaser.getRegisteredParties());

        // 主控制线程
        new Thread(() -> {
            for (int phase = 0; phase < 3; phase++) {
                System.out.println("\n--- 阶段 " + phase + " 开始 ---");

                // 动态添加工作线程
                int workers = (phase + 1) * 2;
                System.out.println("启动 " + workers + " 个工作线程");

                for (int i = 0; i < workers; i++) {
                    phaser.register(); // 动态注册
                    int workerId = i;
                    new Thread(() -> {
                        System.out.println("  Worker-" + workerId + " 执行任务");
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        phaser.arriveAndDeregister(); // 完成后注销
                        System.out.println("  Worker-" + workerId + " 完成并注销");
                    }).start();
                }

                phaser.arriveAndAwaitAdvance(); // 等待当前阶段完成
                System.out.println("阶段 " + phase + " 完成！");
            }

            phaser.arriveAndDeregister(); // 主线程注销
            System.out.println("\n所有阶段完成");
        }, "Controller").start();

        Thread.sleep(3000);
    }

    /**
     * Demo 3: 自定义阶段完成行为
     */
    private static void demo3_CustomPhaseAdvance() throws InterruptedException {
        System.out.println("--- Demo 3: 自定义阶段行为 ---\n");

        Phaser phaser = new Phaser(3) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.println(">>> 阶段 " + phase + " 完成！参与者: " + registeredParties);

                // 阶段2完成后终止
                if (phase >= 2) {
                    System.out.println(">>> Phaser终止");
                    return true; // 返回true表示终止
                }
                return false;
            }
        };

        for (int i = 0; i < 3; i++) {
            int workerId = i;
            new Thread(() -> {
                for (int phase = 0; phase <= 3; phase++) {
                    if (phaser.isTerminated()) {
                        System.out.println("Worker-" + workerId + " 检测到Phaser已终止");
                        break;
                    }
                    System.out.println("Worker-" + workerId + " 阶段" + phase);
                    phaser.arriveAndAwaitAdvance();
                }
            }, "Worker-" + i).start();
        }

        Thread.sleep(2000);
    }

    /**
     * Demo 4: 实际应用 - 多阶段数据处理
     */
    private static void demo4_MultiPhaseProcessing() throws InterruptedException {
        System.out.println("--- Demo 4: 多阶段数据处理 ---\n");

        DataProcessor processor = new DataProcessor();
        processor.process();

        Thread.sleep(3000);
    }

    /**
     * 多阶段数据处理器
     */
    static class DataProcessor {
        private final int numWorkers = 4;
        private final Phaser phaser;

        public DataProcessor() {
            phaser = new Phaser(numWorkers) {
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    String[] phases = {"加载", "清洗", "分析", "输出"};
                    if (phase < phases.length) {
                        System.out.println("\n*** 阶段[" + phases[phase] + "]完成 ***\n");
                    }
                    return phase >= 3; // 4个阶段后终止
                }
            };
        }

        public void process() {
            for (int i = 0; i < numWorkers; i++) {
                new Thread(new DataWorker(i)).start();
            }
        }

        class DataWorker implements Runnable {
            private final int id;

            DataWorker(int id) {
                this.id = id;
            }

            @Override
            public void run() {
                try {
                    // 阶段0: 加载数据
                    System.out.println("Worker-" + id + ": 加载数据...");
                    Thread.sleep(100 + (int)(Math.random() * 100));
                    phaser.arriveAndAwaitAdvance();

                    // 阶段1: 数据清洗
                    System.out.println("Worker-" + id + ": 清洗数据...");
                    Thread.sleep(100 + (int)(Math.random() * 100));
                    phaser.arriveAndAwaitAdvance();

                    // 阶段2: 数据分析
                    System.out.println("Worker-" + id + ": 分析数据...");
                    Thread.sleep(100 + (int)(Math.random() * 100));
                    phaser.arriveAndAwaitAdvance();

                    // 阶段3: 输出结果
                    System.out.println("Worker-" + id + ": 输出结果...");
                    Thread.sleep(100 + (int)(Math.random() * 100));
                    phaser.arriveAndAwaitAdvance();

                    System.out.println("Worker-" + id + ": 全部完成！");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 补充Demo: Exchanger演示
     */
    static class ExchangerDemo {
        public static void demo() throws InterruptedException {
            System.out.println("--- 补充: Exchanger演示 ---\n");

            java.util.concurrent.Exchanger<String> exchanger = new java.util.concurrent.Exchanger<>();

            // 线程1
            new Thread(() -> {
                try {
                    String data = "来自线程1的数据";
                    System.out.println("线程1准备交换: " + data);

                    String received = exchanger.exchange(data);

                    System.out.println("线程1收到: " + received);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "Thread-1").start();

            // 线程2
            new Thread(() -> {
                try {
                    Thread.sleep(500); // 延迟一下
                    String data = "来自线程2的数据";
                    System.out.println("线程2准备交换: " + data);

                    String received = exchanger.exchange(data);

                    System.out.println("线程2收到: " + received);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "Thread-2").start();

            Thread.sleep(1500);
        }
    }
}
