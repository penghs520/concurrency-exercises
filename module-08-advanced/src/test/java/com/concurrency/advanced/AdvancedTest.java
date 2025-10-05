package com.concurrency.advanced;

import com.concurrency.advanced.solutions.*;
import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module 08: 高级主题测试
 *
 * 测试内容：
 * 1. 并行归并排序
 * 2. 并发跳表
 * 3. 并行数据聚合器
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdvancedTest {

    @BeforeAll
    static void setup() {
        System.out.println("\n=== Module 08: 高级主题测试 ===\n");
    }

    // ==================== 测试1: 并行归并排序 ====================

    @Test
    @Order(1)
    @DisplayName("测试1.1: 并行归并排序 - 小数组")
    void testParallelMergeSort_SmallArray() {
        System.out.println("测试1.1: 并行归并排序 - 小数组");

        Integer[] array = {5, 2, 8, 1, 9, 3, 7, 4, 6};
        S01_ParallelMergeSort.sort(array);

        assertTrue(isSorted(array), "数组应该被正确排序");
        assertEquals(1, array[0], "最小值应该在第一个位置");
        assertEquals(9, array[array.length - 1], "最大值应该在最后一个位置");

        System.out.println("  ✓ 小数组排序正确");
    }

    @Test
    @Order(2)
    @DisplayName("测试1.2: 并行归并排序 - 大数组")
    void testParallelMergeSort_LargeArray() {
        System.out.println("测试1.2: 并行归并排序 - 大数组");

        int size = 100000;
        Integer[] array = generateRandomArray(size);

        long start = System.currentTimeMillis();
        S01_ParallelMergeSort.sort(array);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(isSorted(array), "大数组应该被正确排序");
        System.out.println("  ✓ 大数组排序正确（耗时: " + elapsed + "ms）");
    }

    @Test
    @Order(3)
    @DisplayName("测试1.3: 并行归并排序 - 边界情况")
    void testParallelMergeSort_EdgeCases() {
        System.out.println("测试1.3: 并行归并排序 - 边界情况");

        // 空数组
        Integer[] empty = {};
        S01_ParallelMergeSort.sort(empty);
        assertEquals(0, empty.length);

        // 单元素
        Integer[] single = {42};
        S01_ParallelMergeSort.sort(single);
        assertEquals(42, single[0]);

        // 已排序
        Integer[] sorted = {1, 2, 3, 4, 5};
        S01_ParallelMergeSort.sort(sorted);
        assertTrue(isSorted(sorted));

        // 逆序
        Integer[] reversed = {5, 4, 3, 2, 1};
        S01_ParallelMergeSort.sort(reversed);
        assertTrue(isSorted(reversed));

        System.out.println("  ✓ 所有边界情况通过");
    }

    // ==================== 测试2: 并发跳表 ====================

    @Test
    @Order(4)
    @DisplayName("测试2.1: 并发跳表 - 基本操作")
    void testConcurrentSkipList_BasicOperations() {
        System.out.println("测试2.1: 并发跳表 - 基本操作");

        S02_ConcurrentSkipList.ConcurrentSkipListSet skipList =
                new S02_ConcurrentSkipList.ConcurrentSkipListSet();

        // 测试添加
        assertTrue(skipList.add(5), "应该成功添加5");
        assertTrue(skipList.add(3), "应该成功添加3");
        assertTrue(skipList.add(7), "应该成功添加7");
        assertFalse(skipList.add(5), "重复添加5应该返回false");

        // 测试查找
        assertTrue(skipList.contains(5), "应该找到5");
        assertTrue(skipList.contains(3), "应该找到3");
        assertFalse(skipList.contains(10), "不应该找到10");

        // 测试删除
        assertTrue(skipList.remove(5), "应该成功删除5");
        assertFalse(skipList.contains(5), "删除后不应该找到5");
        assertFalse(skipList.remove(5), "删除不存在的元素应该返回false");

        System.out.println("  ✓ 基本操作正确");
    }

    @Test
    @Order(5)
    @DisplayName("测试2.2: 并发跳表 - 并发添加")
    void testConcurrentSkipList_ConcurrentAdd() throws InterruptedException {
        System.out.println("测试2.2: 并发跳表 - 并发添加");

        S02_ConcurrentSkipList.ConcurrentSkipListSet skipList =
                new S02_ConcurrentSkipList.ConcurrentSkipListSet();

        int numThreads = 4;
        int opsPerThread = 100;
        Thread[] threads = new Thread[numThreads];

        // 并发添加
        for (int i = 0; i < numThreads; i++) {
            int start = i * opsPerThread;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    skipList.add(start + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有元素都被添加
        for (int i = 0; i < numThreads * opsPerThread; i++) {
            assertTrue(skipList.contains(i), "应该包含元素 " + i);
        }

        System.out.println("  ✓ 并发添加正确");
    }

    @Test
    @Order(6)
    @DisplayName("测试2.3: 并发跳表 - 并发读写")
    void testConcurrentSkipList_ConcurrentReadWrite() throws InterruptedException {
        System.out.println("测试2.3: 并发跳表 - 并发读写");

        S02_ConcurrentSkipList.ConcurrentSkipListSet skipList =
                new S02_ConcurrentSkipList.ConcurrentSkipListSet();

        // 预先添加一些数据
        for (int i = 0; i < 100; i++) {
            skipList.add(i);
        }

        // 并发读写
        Thread writer = new Thread(() -> {
            for (int i = 100; i < 200; i++) {
                skipList.add(i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread reader = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                skipList.contains(i);
            }
        });

        writer.start();
        reader.start();

        writer.join();
        reader.join();

        // 验证数据完整性
        for (int i = 0; i < 200; i++) {
            assertTrue(skipList.contains(i), "应该包含元素 " + i);
        }

        System.out.println("  ✓ 并发读写正确");
    }

    // ==================== 测试3: 并行数据聚合器 ====================

    @Test
    @Order(7)
    @DisplayName("测试3.1: 并行数据聚合器 - 基本功能")
    void testDataAggregator_Basic() throws InterruptedException {
        System.out.println("测试3.1: 并行数据聚合器 - 基本功能");

        List<S03_DataAggregator.DataChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            List<Integer> data = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                data.add(j);
            }
            chunks.add(new S03_DataAggregator.DataChunk(data));
        }

        S03_DataAggregator.ParallelDataAggregator aggregator =
                new S03_DataAggregator.ParallelDataAggregator(4, chunks);

        S03_DataAggregator.AggregateResult result = aggregator.aggregate();

        assertNotNull(result, "结果不应该为null");
        assertTrue(result.getCount() > 0, "应该有统计数据");

        System.out.println("  ✓ 基本聚合功能正确");
    }

    @Test
    @Order(8)
    @DisplayName("测试3.2: 并行数据聚合器 - 性能测试")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testDataAggregator_Performance() throws InterruptedException {
        System.out.println("测试3.2: 并行数据聚合器 - 性能测试");

        List<S03_DataAggregator.DataChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            List<Integer> data = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                data.add((int) (Math.random() * 100));
            }
            chunks.add(new S03_DataAggregator.DataChunk(data));
        }

        S03_DataAggregator.ParallelDataAggregator aggregator =
                new S03_DataAggregator.ParallelDataAggregator(4, chunks);

        long start = System.currentTimeMillis();
        S03_DataAggregator.AggregateResult result = aggregator.aggregate();
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(result);
        System.out.println("  ✓ 性能测试通过（耗时: " + elapsed + "ms）");
    }

    @Test
    @Order(9)
    @DisplayName("测试3.3: ForkJoin聚合任务")
    void testAggregateTask() {
        System.out.println("测试3.3: ForkJoin聚合任务");

        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            data.add(i);
        }

        S03_DataAggregator.AggregateTask task =
                new S03_DataAggregator.AggregateTask(data, 0, data.size());

        java.util.concurrent.ForkJoinPool pool = new java.util.concurrent.ForkJoinPool();
        S03_DataAggregator.AggregateResult result = pool.invoke(task);

        assertNotNull(result);
        assertEquals(1000, result.getCount(), "应该统计1000个元素");
        assertEquals(500500, result.getSum(), "总和应该是500500");
        assertEquals(1, result.getMin(), "最小值应该是1");
        assertEquals(1000, result.getMax(), "最大值应该是1000");

        System.out.println("  ✓ ForkJoin聚合正确");
    }

    // ==================== 辅助方法 ====================

    private <T extends Comparable<T>> boolean isSorted(T[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i].compareTo(array[i - 1]) < 0) {
                return false;
            }
        }
        return true;
    }

    private Integer[] generateRandomArray(int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = (int) (Math.random() * 1000000);
        }
        return array;
    }

    @AfterAll
    static void summary() {
        System.out.println("\n=== 测试完成 ===");
        System.out.println("✓ 所有高级主题测试通过！");
        System.out.println("\n恭喜！你已经掌握了：");
        System.out.println("  - ForkJoin框架与工作窃取");
        System.out.println("  - StampedLock乐观锁");
        System.out.println("  - Phaser多阶段同步");
        System.out.println("  - 并发数据结构设计");
        System.out.println("\n继续深入学习并发编程的高级技巧！\n");
    }
}
