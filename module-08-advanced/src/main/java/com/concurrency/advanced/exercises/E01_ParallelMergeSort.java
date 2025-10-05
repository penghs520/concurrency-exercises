package com.concurrency.advanced.exercises;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

/**
 * 练习1: 并行归并排序 🟡
 *
 * 任务描述：
 * 使用ForkJoin框架实现并行归并排序算法
 *
 * 要求：
 * 1. 继承RecursiveAction实现归并排序
 * 2. 设置合理的阈值（THRESHOLD），小于阈值时使用串行排序
 * 3. 实现merge方法合并两个有序数组
 * 4. 支持通用Comparable类型
 * 5. 对比串行和并行的性能差异
 *
 * 提示：
 * - 归并排序的时间复杂度：O(n log n)
 * - 合理的阈值可以避免过度拆分
 * - fork()用于异步执行子任务
 * - join()等待子任务完成
 *
 * 测试用例：
 * - 小数组（100个元素）
 * - 大数组（1000000个元素）
 * - 已排序数组
 * - 逆序数组
 */
public class E01_ParallelMergeSort {

    /**
     * TODO: 实现并行归并排序
     *
     * @param array 待排序数组
     * @param <T> 元素类型（需要实现Comparable）
     */
    public static <T extends Comparable<T>> void sort(T[] array) {
        // TODO: 创建ForkJoinPool
        // TODO: 创建MergeSortTask并执行
        throw new UnsupportedOperationException("请实现并行归并排序");
    }

    /**
     * TODO: 实现RecursiveAction用于归并排序
     */
    static class MergeSortTask<T extends Comparable<T>> extends RecursiveAction {
        private static final int THRESHOLD = 1000; // TODO: 根据实际情况调整

        private final T[] array;
        private final T[] temp;  // 临时数组
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
            // TODO: 实现归并排序逻辑
            // 1. 如果数组长度小于阈值，使用串行排序（insertionSort）
            // 2. 否则，拆分成两个子任务
            // 3. 使用fork()和join()并行执行
            // 4. 合并两个有序子数组
            throw new UnsupportedOperationException("请实现compute方法");
        }

        /**
         * TODO: 合并两个有序子数组
         *
         * @param left 左边界
         * @param mid 中间位置
         * @param right 右边界
         */
        private void merge(int left, int mid, int right) {
            // TODO: 实现归并逻辑
            // 1. 复制到临时数组
            // 2. 双指针合并
            // 3. 复制回原数组
            throw new UnsupportedOperationException("请实现merge方法");
        }

        /**
         * TODO: 插入排序（用于小数组）
         */
        private void insertionSort(int left, int right) {
            // TODO: 实现插入排序
            throw new UnsupportedOperationException("请实现insertionSort方法");
        }
    }

    /**
     * 串行归并排序（用于性能对比）
     */
    public static <T extends Comparable<T>> void serialSort(T[] array) {
        @SuppressWarnings("unchecked")
        T[] temp = (T[]) new Comparable[array.length];
        serialMergeSort(array, temp, 0, array.length - 1);
    }

    private static <T extends Comparable<T>> void serialMergeSort(T[] array, T[] temp, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            serialMergeSort(array, temp, left, mid);
            serialMergeSort(array, temp, mid + 1, right);
            serialMerge(array, temp, left, mid, right);
        }
    }

    private static <T extends Comparable<T>> void serialMerge(T[] array, T[] temp, int left, int mid, int right) {
        for (int i = left; i <= right; i++) {
            temp[i] = array[i];
        }

        int i = left, j = mid + 1, k = left;
        while (i <= mid && j <= right) {
            if (temp[i].compareTo(temp[j]) <= 0) {
                array[k++] = temp[i++];
            } else {
                array[k++] = temp[j++];
            }
        }

        while (i <= mid) {
            array[k++] = temp[i++];
        }
    }

    // ========== 测试代码 ==========
    public static void main(String[] args) {
        System.out.println("=== 并行归并排序测试 ===\n");

        // 测试1: 小数组
        testSmallArray();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试2: 大数组性能对比
        testPerformance();
    }

    private static void testSmallArray() {
        System.out.println("--- 测试1: 小数组排序 ---");

        Integer[] array = {5, 2, 8, 1, 9, 3, 7, 4, 6};
        System.out.println("排序前: " + java.util.Arrays.toString(array));

        try {
            sort(array);
            System.out.println("排序后: " + java.util.Arrays.toString(array));

            // 验证
            boolean sorted = isSorted(array);
            System.out.println("验证: " + (sorted ? "✓ 正确" : "✗ 错误"));
        } catch (UnsupportedOperationException e) {
            System.out.println("TODO: 请实现sort方法");
        }
    }

    private static void testPerformance() {
        System.out.println("--- 测试2: 性能对比 ---");

        int size = 1_000_000;
        Integer[] array1 = generateRandomArray(size);
        Integer[] array2 = java.util.Arrays.copyOf(array1, array1.length);

        // 串行排序
        long start = System.currentTimeMillis();
        serialSort(array1);
        long serialTime = System.currentTimeMillis() - start;

        // 并行排序
        try {
            start = System.currentTimeMillis();
            sort(array2);
            long parallelTime = System.currentTimeMillis() - start;

            System.out.println("数组大小: " + size);
            System.out.println("串行排序: " + serialTime + "ms");
            System.out.println("并行排序: " + parallelTime + "ms");
            if (parallelTime > 0) {
                System.out.println("加速比: " + String.format("%.2f", (double) serialTime / parallelTime) + "x");
            }

            // 验证
            boolean sorted = isSorted(array2);
            System.out.println("验证: " + (sorted ? "✓ 正确" : "✗ 错误"));
        } catch (UnsupportedOperationException e) {
            System.out.println("TODO: 请实现sort方法");
        }
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
