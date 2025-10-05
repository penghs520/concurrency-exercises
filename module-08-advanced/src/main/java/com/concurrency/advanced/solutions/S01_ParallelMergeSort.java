package com.concurrency.advanced.solutions;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.util.Arrays;

/**
 * 解答1: 并行归并排序
 *
 * 核心要点：
 * 1. 使用RecursiveAction进行任务拆分
 * 2. 合理设置阈值避免过度拆分
 * 3. fork()异步执行，join()等待结果
 * 4. 归并时使用临时数组
 */
public class S01_ParallelMergeSort {

    /**
     * 并行归并排序入口
     */
    public static <T extends Comparable<T>> void sort(T[] array) {
        if (array == null || array.length <= 1) {
            return;
        }

        @SuppressWarnings("unchecked")
        T[] temp = (T[]) new Comparable[array.length];

        ForkJoinPool pool = ForkJoinPool.commonPool();
        MergeSortTask<T> task = new MergeSortTask<>(array, temp, 0, array.length - 1);
        pool.invoke(task);
    }

    /**
     * 归并排序任务
     */
    static class MergeSortTask<T extends Comparable<T>> extends RecursiveAction {
        private static final int THRESHOLD = 1000; // 阈值：小于此值使用串行排序

        private final T[] array;
        private final T[] temp;
        private final int left;
        private final int right;

        public MergeSortTask(T[] array, T[] temp, int left, int right) {
            this.array = array;
            this.temp = temp;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            int length = right - left + 1;

            if (length <= THRESHOLD) {
                // 任务足够小，使用插入排序（对小数组更高效）
                insertionSort(left, right);
            } else {
                // 拆分任务
                int mid = left + (right - left) / 2;

                MergeSortTask<T> leftTask = new MergeSortTask<>(array, temp, left, mid);
                MergeSortTask<T> rightTask = new MergeSortTask<>(array, temp, mid + 1, right);

                // 方式1：invokeAll（推荐）
                invokeAll(leftTask, rightTask);

                // 方式2：手动fork和join
                // leftTask.fork();
                // rightTask.compute();
                // leftTask.join();

                // 合并两个有序子数组
                merge(left, mid, right);
            }
        }

        /**
         * 合并两个有序子数组
         */
        private void merge(int left, int mid, int right) {
            // 复制到临时数组
            for (int i = left; i <= right; i++) {
                temp[i] = array[i];
            }

            // 双指针合并
            int i = left;       // 左子数组指针
            int j = mid + 1;    // 右子数组指针
            int k = left;       // 结果数组指针

            while (i <= mid && j <= right) {
                if (temp[i].compareTo(temp[j]) <= 0) {
                    array[k++] = temp[i++];
                } else {
                    array[k++] = temp[j++];
                }
            }

            // 复制剩余元素（只需复制左边，右边已在正确位置）
            while (i <= mid) {
                array[k++] = temp[i++];
            }
        }

        /**
         * 插入排序（用于小数组）
         * 对于小数组，插入排序比归并排序更高效
         */
        private void insertionSort(int left, int right) {
            for (int i = left + 1; i <= right; i++) {
                T key = array[i];
                int j = i - 1;

                while (j >= left && array[j].compareTo(key) > 0) {
                    array[j + 1] = array[j];
                    j--;
                }
                array[j + 1] = key;
            }
        }
    }

    // ========== 测试代码 ==========
    public static void main(String[] args) {
        System.out.println("=== 并行归并排序 - 参考答案 ===\n");

        // 测试1: 小数组
        testSmallArray();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试2: 大数组性能对比
        testPerformance();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试3: 边界情况
        testEdgeCases();
    }

    private static void testSmallArray() {
        System.out.println("--- 测试1: 小数组排序 ---");

        Integer[] array = {5, 2, 8, 1, 9, 3, 7, 4, 6};
        System.out.println("排序前: " + Arrays.toString(array));

        sort(array);
        System.out.println("排序后: " + Arrays.toString(array));

        boolean sorted = isSorted(array);
        System.out.println("验证: " + (sorted ? "✓ 正确" : "✗ 错误"));
    }

    private static void testPerformance() {
        System.out.println("--- 测试2: 性能对比 ---");

        int size = 1_000_000;
        Integer[] array1 = generateRandomArray(size);
        Integer[] array2 = Arrays.copyOf(array1, array1.length);

        // 串行排序（使用Arrays.sort作为基准）
        long start = System.currentTimeMillis();
        Arrays.sort(array1);
        long serialTime = System.currentTimeMillis() - start;

        // 并行排序
        start = System.currentTimeMillis();
        sort(array2);
        long parallelTime = System.currentTimeMillis() - start;

        System.out.println("数组大小: " + size);
        System.out.println("Arrays.sort: " + serialTime + "ms");
        System.out.println("并行归并: " + parallelTime + "ms");

        if (parallelTime > 0) {
            double speedup = (double) serialTime / parallelTime;
            System.out.println("加速比: " + String.format("%.2f", speedup) + "x");
        }

        boolean sorted = isSorted(array2);
        System.out.println("验证: " + (sorted ? "✓ 正确" : "✗ 错误"));
    }

    private static void testEdgeCases() {
        System.out.println("--- 测试3: 边界情况 ---");

        // 空数组
        Integer[] empty = {};
        sort(empty);
        System.out.println("空数组: " + (isSorted(empty) ? "✓" : "✗"));

        // 单元素
        Integer[] single = {42};
        sort(single);
        System.out.println("单元素: " + (isSorted(single) ? "✓" : "✗"));

        // 已排序
        Integer[] sorted = {1, 2, 3, 4, 5};
        sort(sorted);
        System.out.println("已排序: " + (isSorted(sorted) ? "✓" : "✗"));

        // 逆序
        Integer[] reversed = {5, 4, 3, 2, 1};
        sort(reversed);
        System.out.println("逆序: " + (isSorted(reversed) ? "✓" : "✗"));

        // 重复元素
        Integer[] duplicates = {3, 1, 4, 1, 5, 9, 2, 6, 5};
        sort(duplicates);
        System.out.println("重复元素: " + (isSorted(duplicates) ? "✓" : "✗"));
    }

    private static Integer[] generateRandomArray(int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = (int) (Math.random() * 1000000);
        }
        return array;
    }

    private static <T extends Comparable<T>> boolean isSorted(T[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i].compareTo(array[i - 1]) < 0) {
                return false;
            }
        }
        return true;
    }
}
