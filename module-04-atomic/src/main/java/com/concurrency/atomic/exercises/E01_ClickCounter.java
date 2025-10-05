package com.concurrency.atomic.exercises;

/**
 * 练习01: 线程安全的点击计数器
 *
 * 难度: 🟢 简单
 *
 * 任务描述:
 * 实现一个线程安全的点击计数器，用于统计网站的点击量。
 * 要求使用AtomicInteger或LongAdder实现，不允许使用synchronized。
 *
 * 功能要求:
 * 1. 实现单次点击计数（click方法）
 * 2. 实现批量点击计数（clickMultiple方法）
 * 3. 获取当前点击总数（getTotalClicks方法）
 * 4. 重置计数器（reset方法）
 * 5. 获取并重置（getAndReset方法）
 *
 * 进阶要求:
 * 1. 实现分类计数（记录不同来源的点击）
 * 2. 实现按时间段统计（如每分钟点击数）
 * 3. 比较不同实现的性能（AtomicLong vs LongAdder）
 *
 * 提示:
 * - 考虑高并发场景下的性能
 * - 注意sum()方法的开销
 * - 思考什么场景用AtomicLong，什么场景用LongAdder
 */
public class E01_ClickCounter {

    /**
     * TODO: 实现基础版本的点击计数器
     * 使用AtomicInteger或AtomicLong
     */
    static class BasicClickCounter {
        // TODO: 添加必要的字段

        /**
         * 单次点击
         */
        public void click() {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现click方法");
        }

        /**
         * 批量点击
         * @param count 点击次数
         */
        public void clickMultiple(int count) {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现clickMultiple方法");
        }

        /**
         * 获取总点击数
         */
        public long getTotalClicks() {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现getTotalClicks方法");
        }

        /**
         * 重置计数器
         */
        public void reset() {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现reset方法");
        }

        /**
         * 获取并重置
         * @return 重置前的总数
         */
        public long getAndReset() {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现getAndReset方法");
        }
    }

    /**
     * TODO: 实现高性能版本（使用LongAdder）
     */
    static class HighPerformanceClickCounter {
        // TODO: 使用LongAdder实现

        public void click() {
            throw new UnsupportedOperationException("请实现");
        }

        public void clickMultiple(int count) {
            throw new UnsupportedOperationException("请实现");
        }

        public long getTotalClicks() {
            throw new UnsupportedOperationException("请实现");
        }

        public void reset() {
            throw new UnsupportedOperationException("请实现");
        }

        public long getAndReset() {
            throw new UnsupportedOperationException("请实现");
        }
    }

    /**
     * TODO: 进阶任务 - 实现分类计数器
     * 记录不同来源的点击（如：首页、搜索页、推荐页）
     */
    static class CategorizedClickCounter {
        // TODO: 使用Map<String, LongAdder>存储不同类别的计数

        /**
         * 记录指定类别的点击
         * @param category 类别名称
         */
        public void click(String category) {
            throw new UnsupportedOperationException("请实现");
        }

        /**
         * 获取指定类别的点击数
         * @param category 类别名称
         */
        public long getClicks(String category) {
            throw new UnsupportedOperationException("请实现");
        }

        /**
         * 获取总点击数（所有类别）
         */
        public long getTotalClicks() {
            throw new UnsupportedOperationException("请实现");
        }

        /**
         * 获取所有类别的统计信息
         * @return Map<类别, 点击数>
         */
        public java.util.Map<String, Long> getStatistics() {
            throw new UnsupportedOperationException("请实现");
        }
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 测试点击计数器 ===\n");

        // 测试基础版本
        testBasicCounter();

        // 测试高性能版本
        // testHighPerformanceCounter();

        // 性能对比
        // performanceComparison();
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
        System.out.println();
    }

    /**
     * TODO: 实现性能对比测试
     */
    @SuppressWarnings("unused")
    private static void performanceComparison() throws InterruptedException {
        System.out.println("--- 性能对比 ---");
        // TODO: 对比AtomicLong和LongAdder的性能
    }
}
