package com.concurrency.advanced.demo;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.Arrays;

/**
 * Demo 01: ForkJoin框架演示
 *
 * 本示例演示：
 * 1. RecursiveTask - 并行计算数组和
 * 2. RecursiveAction - 并行快速排序
 * 3. 工作窃取算法的效果
 * 4. 性能对比（串行 vs 并行）
 */
public class D01_ForkJoin {

    public static void main(String[] args) {
        System.out.println("=== ForkJoin框架演示 ===\n");

        // Demo 1: RecursiveTask - 计算数组和
        demo1_RecursiveTask();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 2: RecursiveAction - 并行排序
        demo2_RecursiveAction();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 3: 性能对比
        demo3_PerformanceComparison();
    }

    /**
     * Demo 1: 使用RecursiveTask并行计算数组和
     */
    private static void demo1_RecursiveTask() {
        System.out.println("--- Demo 1: RecursiveTask计算数组和 ---\n");

        int[] array = new int[10000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }

        ForkJoinPool pool = ForkJoinPool.commonPool();
        SumTask task = new SumTask(array, 0, array.length);

        long startTime = System.currentTimeMillis();
        Long result = pool.invoke(task);
        long endTime = System.currentTimeMillis();

        System.out.println("数组长度: " + array.length);
        System.out.println("并行计算结果: " + result);
        System.out.println("期望结果: " + (10000L * 10001L / 2));
        System.out.println("耗时: " + (endTime - startTime) + "ms");
        System.out.println("使用线程数: " + pool.getPoolSize());
    }

    /**
     * Demo 2: 使用RecursiveAction并行排序
     */
    private static void demo2_RecursiveAction() {
        System.out.println("--- Demo 2: RecursiveAction并行排序 ---\n");

        int[] array = new int[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = (int) (Math.random() * 1000);
        }

        System.out.println("排序前（前10个）: " + Arrays.toString(Arrays.copyOf(array, 10)));

        ForkJoinPool pool = new ForkJoinPool();
        SortAction task = new SortAction(array, 0, array.length);

        long startTime = System.nanoTime();
        pool.invoke(task);
        long endTime = System.nanoTime();

        System.out.println("排序后（前10个）: " + Arrays.toString(Arrays.copyOf(array, 10)));
        System.out.println("耗时: " + (endTime - startTime) / 1000 + "μs");

        // 验证是否正确排序
        boolean sorted = true;
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                sorted = false;
                break;
            }
        }
        System.out.println("排序验证: " + (sorted ? "✓ 正确" : "✗ 错误"));
    }

    /**
     * Demo 3: 性能对比（串行 vs 并行）
     */
    private static void demo3_PerformanceComparison() {
        System.out.println("--- Demo 3: 性能对比 ---\n");

        int size = 10_000_000;
        int[] array1 = new int[size];
        int[] array2 = new int[size];

        for (int i = 0; i < size; i++) {
            array1[i] = array2[i] = i + 1;
        }

        // 串行计算
        long startTime = System.currentTimeMillis();
        long serialSum = 0;
        for (int num : array1) {
            serialSum += num;
        }
        long serialTime = System.currentTimeMillis() - startTime;

        // 并行计算
        ForkJoinPool pool = ForkJoinPool.commonPool();
        SumTask task = new SumTask(array2, 0, array2.length);

        startTime = System.currentTimeMillis();
        long parallelSum = pool.invoke(task);
        long parallelTime = System.currentTimeMillis() - startTime;

        System.out.println("数组大小: " + size);
        System.out.println("\n串行计算:");
        System.out.println("  结果: " + serialSum);
        System.out.println("  耗时: " + serialTime + "ms");

        System.out.println("\n并行计算:");
        System.out.println("  结果: " + parallelSum);
        System.out.println("  耗时: " + parallelTime + "ms");
        System.out.println("  使用线程: " + pool.getPoolSize());

        if (parallelTime > 0) {
            double speedup = (double) serialTime / parallelTime;
            System.out.println("\n加速比: " + String.format("%.2f", speedup) + "x");
        }

        System.out.println("\n说明: 实际加速比取决于CPU核心数和数组大小");
    }

    /**
     * RecursiveTask示例：计算数组和
     */
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 1000; // 阈值
        private final int[] array;
        private final int start;
        private final int end;

        public SumTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;

            if (length <= THRESHOLD) {
                // 任务足够小，直接计算
                return computeDirectly();
            } else {
                // 任务太大，拆分成两个子任务
                int mid = start + length / 2;
                SumTask leftTask = new SumTask(array, start, mid);
                SumTask rightTask = new SumTask(array, mid, end);

                // 方式1：一个fork，一个compute（推荐）
                leftTask.fork(); // 异步执行左任务
                long rightResult = rightTask.compute(); // 当前线程执行右任务
                long leftResult = leftTask.join(); // 等待左任务完成

                return leftResult + rightResult;
            }
        }

        private long computeDirectly() {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        }
    }

    /**
     * RecursiveAction示例：快速排序
     */
    static class SortAction extends RecursiveAction {
        private static final int THRESHOLD = 100;
        private final int[] array;
        private final int start;
        private final int end;

        public SortAction(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int length = end - start;

            if (length <= THRESHOLD) {
                // 直接使用Arrays.sort
                Arrays.sort(array, start, end);
            } else {
                // 快速排序分区
                int pivot = partition(array, start, end);

                // 并行排序两个分区
                SortAction leftTask = new SortAction(array, start, pivot);
                SortAction rightTask = new SortAction(array, pivot + 1, end);

                invokeAll(leftTask, rightTask); // 并行执行
            }
        }

        private int partition(int[] array, int start, int end) {
            int pivot = array[start];
            int left = start;
            int right = end - 1;

            while (left < right) {
                while (left < right && array[right] >= pivot) {
                    right--;
                }
                array[left] = array[right];

                while (left < right && array[left] <= pivot) {
                    left++;
                }
                array[right] = array[left];
            }

            array[left] = pivot;
            return left;
        }
    }
}
