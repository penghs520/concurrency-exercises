package com.concurrency.atomic.demo;

import java.util.concurrent.atomic.*;

/**
 * Demo 03: LongAdder高性能计数器
 *
 * 本示例演示：
 * 1. LongAdder的基本用法
 * 2. LongAdder vs AtomicLong性能对比
 * 3. DoubleAdder的使用
 * 4. 适用场景分析
 */
public class D03_LongAdder {

    public static void main(String[] args) throws Exception {
        System.out.println("=== LongAdder高性能计数器演示 ===\n");

        demo1_LongAdderBasics();
        Thread.sleep(100);

        demo2_PerformanceComparison();
        Thread.sleep(100);

        demo3_DoubleAdder();
    }

    /**
     * Demo 1: LongAdder基础用法
     */
    private static void demo1_LongAdderBasics() {
        System.out.println("--- Demo 1: LongAdder基础用法 ---");

        LongAdder adder = new LongAdder();

        // 基本操作
        adder.increment();           // +1
        adder.increment();           // +1
        System.out.println("increment两次后: " + adder.sum());

        adder.add(10);               // +10
        System.out.println("add(10)后: " + adder.sum());

        adder.decrement();           // -1
        System.out.println("decrement后: " + adder.sum());

        // 获取并重置
        long sumAndReset = adder.sumThenReset();
        System.out.println("sumThenReset: " + sumAndReset);
        System.out.println("重置后: " + adder.sum());

        // 重置
        adder.add(100);
        System.out.println("add(100)后: " + adder.sum());
        adder.reset();
        System.out.println("reset后: " + adder.sum());

        System.out.println();
    }

    /**
     * Demo 2: LongAdder vs AtomicLong 性能对比
     */
    private static void demo2_PerformanceComparison() throws InterruptedException {
        System.out.println("--- Demo 2: 性能对比（高并发场景） ---");

        int threadCount = 20;
        int iterations = 500_000;

        // 测试AtomicLong
        System.out.println("测试AtomicLong (" + threadCount + "线程 x " + iterations + "次)...");
        AtomicLong atomicLong = new AtomicLong(0);
        long time1 = testAtomicLong(atomicLong, threadCount, iterations);
        System.out.println("AtomicLong: 结果=" + atomicLong.get() + ", 耗时=" + time1 + "ms");

        // 测试LongAdder
        System.out.println("\n测试LongAdder (" + threadCount + "线程 x " + iterations + "次)...");
        LongAdder longAdder = new LongAdder();
        long time2 = testLongAdder(longAdder, threadCount, iterations);
        System.out.println("LongAdder: 结果=" + longAdder.sum() + ", 耗时=" + time2 + "ms");

        // 性能对比
        System.out.println("\n性能分析:");
        System.out.println("LongAdder提升: " + String.format("%.2fx", (double) time1 / time2));
        System.out.println("说明: 线程越多，LongAdder优势越明显（分段累加，降低竞争）");

        System.out.println();
    }

    /**
     * 测试AtomicLong性能
     */
    private static long testAtomicLong(AtomicLong counter, int threadCount, int iterations)
            throws InterruptedException {
        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.incrementAndGet();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * 测试LongAdder性能
     */
    private static long testLongAdder(LongAdder counter, int threadCount, int iterations)
            throws InterruptedException {
        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * Demo 3: DoubleAdder的使用
     */
    private static void demo3_DoubleAdder() {
        System.out.println("--- Demo 3: DoubleAdder浮点数累加 ---");

        DoubleAdder doubleAdder = new DoubleAdder();

        doubleAdder.add(1.5);
        doubleAdder.add(2.3);
        doubleAdder.add(3.7);

        System.out.println("累加结果: " + doubleAdder.sum());

        // 并发累加
        DoubleAdder concurrentAdder = new DoubleAdder();
        Thread[] threads = new Thread[10];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    concurrentAdder.add(0.1 * index);
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("并发累加结果: " + String.format("%.2f", concurrentAdder.sum()));

        System.out.println();
    }

    /**
     * 额外演示：LongAccumulator的使用
     */
    @SuppressWarnings("unused")
    private static void demo4_LongAccumulator() {
        System.out.println("--- Demo 4: LongAccumulator自定义累加器 ---");

        // 自定义累加函数：取最大值
        LongAccumulator maxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);

        maxAccumulator.accumulate(10);
        maxAccumulator.accumulate(5);
        maxAccumulator.accumulate(20);
        maxAccumulator.accumulate(15);

        System.out.println("最大值: " + maxAccumulator.get());

        // 自定义累加函数：乘法
        LongAccumulator productAccumulator = new LongAccumulator((x, y) -> x * y, 1);

        productAccumulator.accumulate(2);
        productAccumulator.accumulate(3);
        productAccumulator.accumulate(4);

        System.out.println("乘积: " + productAccumulator.get());

        System.out.println();
    }

    /**
     * 使用建议输出
     */
    static {
        System.out.println("========================================");
        System.out.println("LongAdder vs AtomicLong 选择指南:");
        System.out.println("----------------------------------------");
        System.out.println("使用 AtomicLong:");
        System.out.println("  - 低并发场景（线程数 < 10）");
        System.out.println("  - 需要精确的中间值");
        System.out.println("  - 需要CAS操作");
        System.out.println();
        System.out.println("使用 LongAdder:");
        System.out.println("  - 高并发场景（线程数 >= 10）");
        System.out.println("  - 只需要最终累加结果");
        System.out.println("  - 统计、计数器场景");
        System.out.println("========================================\n");
    }
}
