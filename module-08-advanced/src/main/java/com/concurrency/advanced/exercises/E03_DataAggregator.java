package com.concurrency.advanced.exercises;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 练习3: 并行数据聚合器 🟡 ⭐
 *
 * 任务描述：
 * 使用Phaser实现多阶段并行数据处理和聚合
 *
 * 场景：
 * 处理大量日志数据，分多个阶段：
 * 1. 数据加载（Load）
 * 2. 数据过滤（Filter）
 * 3. 数据聚合（Aggregate）
 * 4. 结果合并（Merge）
 *
 * 要求：
 * 1. 使用Phaser协调多个工作线程
 * 2. 每个阶段等待所有线程完成后再进入下一阶段
 * 3. 使用ForkJoin进行并行聚合
 * 4. 支持自定义聚合函数
 * 5. 输出每个阶段的进度
 *
 * 提示：
 * - Phaser的arriveAndAwaitAdvance()等待所有线程
 * - 可以使用onAdvance()打印阶段完成信息
 * - RecursiveTask用于并行聚合计算
 */
public class E03_DataAggregator {

    /**
     * TODO: 实现并行数据聚合器
     */
    static class ParallelDataAggregator {
        private final int numWorkers;
        private final Phaser phaser;
        private final List<DataChunk> chunks;
        private final List<DataChunk> filteredChunks;
        private final ForkJoinPool forkJoinPool;

        public ParallelDataAggregator(int numWorkers, List<DataChunk> data) {
            this.numWorkers = numWorkers;
            this.chunks = data;
            this.filteredChunks = new ArrayList<>();
            this.forkJoinPool = new ForkJoinPool();

            // TODO: 初始化Phaser，重写onAdvance方法打印阶段信息
            this.phaser = new Phaser(numWorkers) {
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    // TODO: 打印阶段完成信息
                    // 阶段0: 加载完成
                    // 阶段1: 过滤完成
                    // 阶段2: 聚合完成
                    // 阶段3: 合并完成（终止）
                    return false; // TODO: 修改终止条件
                }
            };
        }

        /**
         * TODO: 执行并行聚合
         *
         * @return 聚合结果
         */
        public AggregateResult aggregate() {
            // TODO: 启动多个Worker线程
            // TODO: 等待所有阶段完成
            // TODO: 返回最终聚合结果
            throw new UnsupportedOperationException("请实现aggregate方法");
        }

        /**
         * TODO: 工作线程
         */
        class AggregateWorker implements Runnable {
            private final int workerId;
            private final int startIndex;
            private final int endIndex;

            public AggregateWorker(int workerId, int startIndex, int endIndex) {
                this.workerId = workerId;
                this.startIndex = startIndex;
                this.endIndex = endIndex;
            }

            @Override
            public void run() {
                try {
                    // 阶段0: 加载数据
                    loadData();
                    phaser.arriveAndAwaitAdvance();

                    // 阶段1: 过滤数据
                    filterData();
                    phaser.arriveAndAwaitAdvance();

                    // 阶段2: 聚合数据
                    aggregateData();
                    phaser.arriveAndAwaitAdvance();

                    // 阶段3: 合并结果
                    if (workerId == 0) {
                        mergeResults();
                    }
                    phaser.arriveAndAwaitAdvance();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * TODO: 阶段1 - 加载数据
             */
            private void loadData() {
                // TODO: 模拟数据加载
                System.out.println("Worker-" + workerId + ": 加载数据 [" + startIndex + "-" + endIndex + "]");
                simulateWork(50);
            }

            /**
             * TODO: 阶段2 - 过滤数据
             */
            private void filterData() {
                // TODO: 过滤数据（例如：只保留值>50的数据）
                System.out.println("Worker-" + workerId + ": 过滤数据");
                simulateWork(50);
            }

            /**
             * TODO: 阶段3 - 聚合数据
             */
            private void aggregateData() {
                // TODO: 使用ForkJoin并行聚合
                System.out.println("Worker-" + workerId + ": 聚合数据");
                simulateWork(50);
            }

            /**
             * TODO: 阶段4 - 合并结果
             */
            private void mergeResults() {
                // TODO: 合并所有Worker的结果
                System.out.println("Worker-" + workerId + ": 合并最终结果");
                simulateWork(50);
            }

            private void simulateWork(int millis) {
                try {
                    Thread.sleep(millis + (int)(Math.random() * 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 数据块
     */
    static class DataChunk {
        private final List<Integer> data;

        public DataChunk(List<Integer> data) {
            this.data = data;
        }

        public List<Integer> getData() {
            return data;
        }

        public int size() {
            return data.size();
        }
    }

    /**
     * 聚合结果
     */
    static class AggregateResult {
        private long sum;
        private int count;
        private double average;
        private int max;
        private int min;

        public AggregateResult() {
            this.max = Integer.MIN_VALUE;
            this.min = Integer.MAX_VALUE;
        }

        public void merge(AggregateResult other) {
            this.sum += other.sum;
            this.count += other.count;
            this.max = Math.max(this.max, other.max);
            this.min = Math.min(this.min, other.min);
        }

        public void compute() {
            if (count > 0) {
                this.average = (double) sum / count;
            }
        }

        @Override
        public String toString() {
            return String.format("Sum=%d, Count=%d, Avg=%.2f, Max=%d, Min=%d",
                    sum, count, average, max, min);
        }

        // Getters and Setters
        public long getSum() { return sum; }
        public void setSum(long sum) { this.sum = sum; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getAverage() { return average; }
        public int getMax() { return max; }
        public void setMax(int max) { this.max = max; }
        public int getMin() { return min; }
        public void setMin(int min) { this.min = min; }
    }

    /**
     * TODO: ForkJoin聚合任务（可选）
     */
    static class AggregateTask extends RecursiveTask<AggregateResult> {
        private static final int THRESHOLD = 100;
        private final List<Integer> data;
        private final int start;
        private final int end;

        public AggregateTask(List<Integer> data, int start, int end) {
            this.data = data;
            this.start = start;
            this.end = end;
        }

        @Override
        protected AggregateResult compute() {
            // TODO: 实现并行聚合
            // 1. 如果数据量小于阈值，直接计算
            // 2. 否则拆分成子任务
            // 3. 合并子任务的结果
            throw new UnsupportedOperationException("请实现compute方法");
        }

        private AggregateResult computeDirectly() {
            AggregateResult result = new AggregateResult();
            for (int i = start; i < end; i++) {
                int value = data.get(i);
                result.setSum(result.getSum() + value);
                result.setCount(result.getCount() + 1);
                result.setMax(Math.max(result.getMax(), value));
                result.setMin(Math.min(result.getMin(), value));
            }
            result.compute();
            return result;
        }
    }

    // ========== 测试代码 ==========
    public static void main(String[] args) {
        System.out.println("=== 并行数据聚合器测试 ===\n");

        // 生成测试数据
        List<DataChunk> chunks = generateTestData(4, 1000);

        try {
            ParallelDataAggregator aggregator = new ParallelDataAggregator(4, chunks);

            System.out.println("开始并行聚合...\n");
            long startTime = System.currentTimeMillis();

            AggregateResult result = aggregator.aggregate();

            long elapsedTime = System.currentTimeMillis() - startTime;

            System.out.println("\n聚合完成！");
            System.out.println("结果: " + result);
            System.out.println("耗时: " + elapsedTime + "ms");

        } catch (UnsupportedOperationException e) {
            System.out.println("TODO: 请实现ParallelDataAggregator");
            demonstrateExpectedBehavior();
        }
    }

    private static List<DataChunk> generateTestData(int numChunks, int chunkSize) {
        List<DataChunk> chunks = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            List<Integer> data = new ArrayList<>();
            for (int j = 0; j < chunkSize; j++) {
                data.add((int) (Math.random() * 100));
            }
            chunks.add(new DataChunk(data));
        }
        return chunks;
    }

    private static void demonstrateExpectedBehavior() {
        System.out.println("\n=== 期望的输出示例 ===\n");
        System.out.println("Worker-0: 加载数据 [0-999]");
        System.out.println("Worker-1: 加载数据 [1000-1999]");
        System.out.println("Worker-2: 加载数据 [2000-2999]");
        System.out.println("Worker-3: 加载数据 [3000-3999]");
        System.out.println("\n*** 阶段[加载]完成 ***\n");
        System.out.println("Worker-0: 过滤数据");
        System.out.println("Worker-1: 过滤数据");
        System.out.println("Worker-2: 过滤数据");
        System.out.println("Worker-3: 过滤数据");
        System.out.println("\n*** 阶段[过滤]完成 ***\n");
        System.out.println("...");
    }
}
