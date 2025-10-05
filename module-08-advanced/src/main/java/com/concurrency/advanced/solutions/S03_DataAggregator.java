package com.concurrency.advanced.solutions;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 解答3: 并行数据聚合器
 *
 * 核心要点：
 * 1. 使用Phaser协调多个阶段的同步
 * 2. onAdvance()方法在每个阶段完成时调用
 * 3. 使用ForkJoin并行聚合数据
 * 4. 线程安全的结果合并
 */
public class S03_DataAggregator {

    /**
     * 并行数据聚合器
     */
    public static class ParallelDataAggregator {
        private final int numWorkers;
        private final Phaser phaser;
        private final List<DataChunk> chunks;
        private final List<DataChunk> filteredChunks;
        private final List<AggregateResult> workerResults;
        private final ForkJoinPool forkJoinPool;
        private AggregateResult finalResult;

        public ParallelDataAggregator(int numWorkers, List<DataChunk> data) {
            this.numWorkers = numWorkers;
            this.chunks = data;
            this.filteredChunks = Collections.synchronizedList(new ArrayList<>());
            this.workerResults = Collections.synchronizedList(new ArrayList<>());
            this.forkJoinPool = new ForkJoinPool();

            // 初始化Phaser
            this.phaser = new Phaser(numWorkers) {
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    String[] phaseNames = {"加载", "过滤", "聚合", "合并"};
                    if (phase < phaseNames.length) {
                        System.out.println("\n*** 阶段[" + phaseNames[phase] + "]完成 ***\n");
                    }
                    // 阶段3完成后终止
                    return phase >= 3;
                }
            };
        }

        /**
         * 执行并行聚合
         */
        public AggregateResult aggregate() throws InterruptedException {
            // 启动Worker线程
            Thread[] workers = new Thread[numWorkers];
            int chunkSize = (int) Math.ceil((double) chunks.size() / numWorkers);

            for (int i = 0; i < numWorkers; i++) {
                int startIndex = i * chunkSize;
                int endIndex = Math.min((i + 1) * chunkSize, chunks.size());

                workers[i] = new Thread(new AggregateWorker(i, startIndex, endIndex));
                workers[i].start();
            }

            // 等待所有Worker完成
            for (Thread worker : workers) {
                worker.join();
            }

            return finalResult;
        }

        /**
         * 工作线程
         */
        class AggregateWorker implements Runnable {
            private final int workerId;
            private final int startIndex;
            private final int endIndex;
            private AggregateResult localResult;

            public AggregateWorker(int workerId, int startIndex, int endIndex) {
                this.workerId = workerId;
                this.startIndex = startIndex;
                this.endIndex = endIndex;
                this.localResult = new AggregateResult();
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

                    // 阶段3: 合并结果（只由Worker-0执行）
                    if (workerId == 0) {
                        mergeResults();
                    }
                    phaser.arriveAndAwaitAdvance();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * 阶段1: 加载数据
             */
            private void loadData() {
                System.out.println("Worker-" + workerId + ": 加载数据 [" + startIndex + "-" + endIndex + ")");
                simulateWork(50);
                // 数据已在chunks中，这里只是模拟加载过程
            }

            /**
             * 阶段2: 过滤数据
             */
            private void filterData() {
                System.out.println("Worker-" + workerId + ": 过滤数据");
                simulateWork(50);

                // 过滤规则：只保留值 > 50 的数据
                for (int i = startIndex; i < endIndex; i++) {
                    DataChunk chunk = chunks.get(i);
                    List<Integer> filtered = new ArrayList<>();

                    for (Integer value : chunk.getData()) {
                        if (value > 50) {
                            filtered.add(value);
                        }
                    }

                    if (!filtered.isEmpty()) {
                        filteredChunks.add(new DataChunk(filtered));
                    }
                }

                System.out.println("Worker-" + workerId + ": 过滤完成，保留 " +
                        (endIndex - startIndex) + " 个数据块");
            }

            /**
             * 阶段3: 聚合数据
             */
            private void aggregateData() {
                System.out.println("Worker-" + workerId + ": 聚合数据");
                simulateWork(50);

                // 对分配的数据块进行聚合
                for (int i = startIndex; i < endIndex && i < filteredChunks.size(); i++) {
                    DataChunk chunk = filteredChunks.get(i);

                    // 使用ForkJoin并行聚合
                    AggregateTask task = new AggregateTask(chunk.getData(), 0, chunk.size());
                    AggregateResult chunkResult = forkJoinPool.invoke(task);

                    localResult.merge(chunkResult);
                }

                // 保存Worker的结果
                workerResults.add(localResult);

                System.out.println("Worker-" + workerId + ": 聚合完成，本地结果: " + localResult);
            }

            /**
             * 阶段4: 合并结果
             */
            private void mergeResults() {
                System.out.println("Worker-" + workerId + ": 合并所有Worker的结果");
                simulateWork(50);

                finalResult = new AggregateResult();
                for (AggregateResult result : workerResults) {
                    finalResult.merge(result);
                }
                finalResult.compute();

                System.out.println("Worker-" + workerId + ": 最终结果: " + finalResult);
            }

            private void simulateWork(int millis) {
                try {
                    Thread.sleep(millis + (int) (Math.random() * 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 数据块
     */
    public static class DataChunk {
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
    public static class AggregateResult {
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
     * ForkJoin聚合任务
     */
    public static class AggregateTask extends RecursiveTask<AggregateResult> {
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
            int length = end - start;

            if (length <= THRESHOLD) {
                // 直接计算
                return computeDirectly();
            } else {
                // 拆分任务
                int mid = start + length / 2;
                AggregateTask leftTask = new AggregateTask(data, start, mid);
                AggregateTask rightTask = new AggregateTask(data, mid, end);

                // 并行执行
                leftTask.fork();
                AggregateResult rightResult = rightTask.compute();
                AggregateResult leftResult = leftTask.join();

                // 合并结果
                leftResult.merge(rightResult);
                leftResult.compute();
                return leftResult;
            }
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
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 并行数据聚合器 - 参考答案 ===\n");

        // 测试1: 基本功能
        testBasicAggregation();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试2: 大数据集
        testLargeDataSet();
    }

    private static void testBasicAggregation() throws InterruptedException {
        System.out.println("--- 测试1: 基本聚合 ---\n");

        List<DataChunk> chunks = generateTestData(4, 100);

        ParallelDataAggregator aggregator = new ParallelDataAggregator(4, chunks);

        System.out.println("开始并行聚合...\n");
        long startTime = System.currentTimeMillis();

        AggregateResult result = aggregator.aggregate();

        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("\n聚合完成！");
        System.out.println("最终结果: " + result);
        System.out.println("总耗时: " + elapsedTime + "ms");
    }

    private static void testLargeDataSet() throws InterruptedException {
        System.out.println("--- 测试2: 大数据集 ---\n");

        List<DataChunk> chunks = generateTestData(8, 10000);

        ParallelDataAggregator aggregator = new ParallelDataAggregator(4, chunks);

        System.out.println("数据规模: 8 chunks x 10000 elements = 80000 elements");
        System.out.println("开始并行聚合...\n");

        long startTime = System.currentTimeMillis();
        AggregateResult result = aggregator.aggregate();
        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("\n聚合完成！");
        System.out.println("最终结果: " + result);
        System.out.println("总耗时: " + elapsedTime + "ms");
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
}
