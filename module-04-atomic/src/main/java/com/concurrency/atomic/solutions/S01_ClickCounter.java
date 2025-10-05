package com.concurrency.atomic.solutions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 练习01解答: 线程安全的点击计数器
 *
 * 知识点:
 * 1. AtomicLong的基本使用
 * 2. LongAdder的高性能累加
 * 3. 何时选择AtomicLong vs LongAdder
 * 4. ConcurrentHashMap + LongAdder的组合使用
 */
public class S01_ClickCounter {

    /**
     * 基础版本：使用AtomicLong
     */
    public static class BasicClickCounter {
        private final AtomicLong counter = new AtomicLong(0);

        /**
         * 单次点击
         */
        public void click() {
            counter.incrementAndGet();
        }

        /**
         * 批量点击
         */
        public void clickMultiple(int count) {
            counter.addAndGet(count);
        }

        /**
         * 获取总点击数
         */
        public long getTotalClicks() {
            return counter.get();
        }

        /**
         * 重置计数器
         */
        public void reset() {
            counter.set(0);
        }

        /**
         * 获取并重置
         */
        public long getAndReset() {
            return counter.getAndSet(0);
        }
    }

    /**
     * 高性能版本：使用LongAdder
     *
     * 优势:
     * - 高并发下性能更好（分段累加）
     * - 适合频繁写入的场景
     *
     * 劣势:
     * - sum()有一定开销（需要汇总）
     * - 不能用于CAS操作
     */
    public static class HighPerformanceClickCounter {
        private final LongAdder counter = new LongAdder();

        public void click() {
            counter.increment();
        }

        public void clickMultiple(int count) {
            counter.add(count);
        }

        public long getTotalClicks() {
            return counter.sum();
        }

        public void reset() {
            counter.reset();
        }

        public long getAndReset() {
            return counter.sumThenReset();
        }
    }

    /**
     * 进阶版本：分类计数器
     *
     * 使用场景:
     * - 统计不同来源的点击
     * - 分类统计、分组统计
     */
    public static class CategorizedClickCounter {
        private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();

        /**
         * 记录指定类别的点击
         * 使用computeIfAbsent确保线程安全地创建LongAdder
         */
        public void click(String category) {
            counters.computeIfAbsent(category, k -> new LongAdder()).increment();
        }

        /**
         * 批量点击
         */
        public void clickMultiple(String category, int count) {
            counters.computeIfAbsent(category, k -> new LongAdder()).add(count);
        }

        /**
         * 获取指定类别的点击数
         */
        public long getClicks(String category) {
            LongAdder adder = counters.get(category);
            return adder == null ? 0 : adder.sum();
        }

        /**
         * 获取总点击数（所有类别）
         */
        public long getTotalClicks() {
            return counters.values().stream()
                    .mapToLong(LongAdder::sum)
                    .sum();
        }

        /**
         * 获取所有类别的统计信息
         */
        public Map<String, Long> getStatistics() {
            Map<String, Long> stats = new ConcurrentHashMap<>();
            counters.forEach((category, adder) -> {
                stats.put(category, adder.sum());
            });
            return stats;
        }

        /**
         * 重置指定类别
         */
        public void reset(String category) {
            LongAdder adder = counters.get(category);
            if (adder != null) {
                adder.reset();
            }
        }

        /**
         * 重置所有类别
         */
        public void resetAll() {
            counters.values().forEach(LongAdder::reset);
        }
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 点击计数器解答 ===\n");

        testBasicCounter();
        testHighPerformanceCounter();
        testCategorizedCounter();
        performanceComparison();
    }

    /**
     * 测试基础计数器
     */
    private static void testBasicCounter() throws InterruptedException {
        System.out.println("--- 测试BasicClickCounter ---");

        BasicClickCounter counter = new BasicClickCounter();

        // 启动10个线程，每个线程点击1000次
        int threadCount = 10;
        int clicksPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < clicksPerThread; j++) {
                    counter.click();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long total = counter.getTotalClicks();
        long expected = (long) threadCount * clicksPerThread;

        System.out.println("总点击数: " + total);
        System.out.println("期望值: " + expected);
        System.out.println("测试结果: " + (total == expected ? "通过 ✓" : "失败 ✗"));

        // 测试getAndReset
        long resetValue = counter.getAndReset();
        System.out.println("getAndReset返回: " + resetValue);
        System.out.println("重置后: " + counter.getTotalClicks());

        System.out.println();
    }

    /**
     * 测试高性能计数器
     */
    private static void testHighPerformanceCounter() throws InterruptedException {
        System.out.println("--- 测试HighPerformanceClickCounter ---");

        HighPerformanceClickCounter counter = new HighPerformanceClickCounter();

        int threadCount = 10;
        int clicksPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < clicksPerThread; j++) {
                    counter.click();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long total = counter.getTotalClicks();
        long expected = (long) threadCount * clicksPerThread;

        System.out.println("总点击数: " + total);
        System.out.println("期望值: " + expected);
        System.out.println("测试结果: " + (total == expected ? "通过 ✓" : "失败 ✗"));

        System.out.println();
    }

    /**
     * 测试分类计数器
     */
    private static void testCategorizedCounter() throws InterruptedException {
        System.out.println("--- 测试CategorizedClickCounter ---");

        CategorizedClickCounter counter = new CategorizedClickCounter();

        String[] categories = {"首页", "搜索", "推荐", "详情"};
        int threadCount = 20;
        int clicksPerThread = 500;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < clicksPerThread; j++) {
                    String category = categories[threadId % categories.length];
                    counter.click(category);
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("分类统计:");
        Map<String, Long> stats = counter.getStatistics();
        stats.forEach((category, count) -> {
            System.out.println("  " + category + ": " + count);
        });

        long total = counter.getTotalClicks();
        long expected = (long) threadCount * clicksPerThread;
        System.out.println("总点击数: " + total);
        System.out.println("期望值: " + expected);
        System.out.println("测试结果: " + (total == expected ? "通过 ✓" : "失败 ✗"));

        System.out.println();
    }

    /**
     * 性能对比测试
     */
    private static void performanceComparison() throws InterruptedException {
        System.out.println("--- 性能对比: AtomicLong vs LongAdder ---");

        int threadCount = 20;
        int iterations = 500_000;

        // 测试AtomicLong
        BasicClickCounter basicCounter = new BasicClickCounter();
        long time1 = testPerformance("AtomicLong", basicCounter, threadCount, iterations);

        // 测试LongAdder
        HighPerformanceClickCounter hpCounter = new HighPerformanceClickCounter();
        long time2 = testPerformance("LongAdder", hpCounter, threadCount, iterations);

        // 性能对比
        System.out.println("\n性能分析:");
        System.out.println("AtomicLong耗时: " + time1 + "ms");
        System.out.println("LongAdder耗时: " + time2 + "ms");
        System.out.println("性能提升: " + String.format("%.2fx", (double) time1 / time2));
        System.out.println("\n说明: 高并发场景下，LongAdder通过分段累加显著降低竞争");
    }

    /**
     * 性能测试辅助方法
     */
    private static long testPerformance(String name, Object counter, int threadCount, int iterations)
            throws InterruptedException {
        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    if (counter instanceof BasicClickCounter) {
                        ((BasicClickCounter) counter).click();
                    } else {
                        ((HighPerformanceClickCounter) counter).click();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println(name + " 完成 (" + threadCount + "线程 x " + iterations + "次)");

        return elapsed;
    }
}
