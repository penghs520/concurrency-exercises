package com.concurrency.collections.demo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demo 01: ConcurrentHashMap - 线程安全的Map
 *
 * 本示例演示:
 * 1. ConcurrentHashMap的基本操作
 * 2. 原子复合操作（putIfAbsent, compute, merge）
 * 3. 并发安全性验证
 * 4. 性能对比（vs Hashtable）
 */
public class D01_ConcurrentHashMap {

    public static void main(String[] args) throws Exception {
        System.out.println("=== ConcurrentHashMap演示 ===\n");

        // 1. 基本操作
        demo1_BasicOperations();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 原子复合操作
        demo2_AtomicOperations();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 3. 并发计数器
        demo3_ConcurrentCounter();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 4. 性能对比
        demo4_PerformanceComparison();
    }

    /**
     * Demo 1: 基本操作
     */
    private static void demo1_BasicOperations() {
        System.out.println("--- 1. 基本操作 ---");

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // 插入
        map.put("Alice", 25);
        map.put("Bob", 30);
        map.put("Charlie", 35);

        System.out.println("初始Map: " + map);

        // 获取
        Integer age = map.get("Alice");
        System.out.println("Alice的年龄: " + age);

        // 更新
        map.put("Alice", 26);
        System.out.println("更新后: " + map);

        // 删除
        map.remove("Bob");
        System.out.println("删除Bob后: " + map);

        // 注意: 不允许null键值
        try {
            map.put(null, 100); // 抛出异常
        } catch (NullPointerException e) {
            System.out.println("✗ 不允许null键: " + e.getClass().getSimpleName());
        }

        try {
            map.put("David", null); // 抛出异常
        } catch (NullPointerException e) {
            System.out.println("✗ 不允许null值: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Demo 2: 原子复合操作
     */
    private static void demo2_AtomicOperations() {
        System.out.println("--- 2. 原子复合操作 ---");

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // 1. putIfAbsent - 不存在则插入
        Integer oldValue = map.putIfAbsent("counter", 1);
        System.out.println("首次插入counter: " + oldValue + " (null表示成功)");

        oldValue = map.putIfAbsent("counter", 100);
        System.out.println("再次插入counter: " + oldValue + " (返回旧值，未插入)");
        System.out.println("当前值: " + map.get("counter"));

        // 2. compute - 原子计算
        System.out.println("\n使用compute递增:");
        map.compute("counter", (k, v) -> v == null ? 1 : v + 1);
        System.out.println("counter = " + map.get("counter"));

        // 3. computeIfAbsent - 延迟初始化
        System.out.println("\n使用computeIfAbsent:");
        Integer value = map.computeIfAbsent("expensive", k -> {
            System.out.println("  执行昂贵的计算...");
            return 42;
        });
        System.out.println("  结果: " + value);

        value = map.computeIfAbsent("expensive", k -> {
            System.out.println("  不会执行（已存在）");
            return 100;
        });
        System.out.println("  结果: " + value);

        // 4. merge - 原子合并
        System.out.println("\n使用merge计数:");
        map.put("score", 10);
        map.merge("score", 5, Integer::sum); // 10 + 5
        System.out.println("合并后score = " + map.get("score"));

        map.merge("newScore", 1, Integer::sum); // 不存在，直接插入1
        System.out.println("newScore = " + map.get("newScore"));

        // 5. computeIfPresent - 存在则更新
        System.out.println("\n使用computeIfPresent:");
        map.computeIfPresent("score", (k, v) -> v * 2);
        System.out.println("score翻倍 = " + map.get("score"));

        map.computeIfPresent("notExist", (k, v) -> v * 2); // 不存在，不执行
        System.out.println("notExist = " + map.get("notExist"));

        System.out.println("\n最终Map: " + map);
    }

    /**
     * Demo 3: 并发计数器
     * 演示多线程并发累加的安全性
     */
    private static void demo3_ConcurrentCounter() throws Exception {
        System.out.println("--- 3. 并发计数器 ---");

        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        final int THREAD_COUNT = 10;
        final int INCREMENTS_PER_THREAD = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        long startTime = System.currentTimeMillis();

        // 启动10个线程，每个线程递增1000次
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    // 使用computeIfAbsent + AtomicLong保证线程安全
                    counters.computeIfAbsent("total", k -> new AtomicLong())
                           .incrementAndGet();

                    // 每个线程自己的计数器
                    counters.computeIfAbsent("thread-" + threadId, k -> new AtomicLong())
                           .incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();

        // 验证结果
        long expectedTotal = THREAD_COUNT * INCREMENTS_PER_THREAD;
        long actualTotal = counters.get("total").get();

        System.out.println("期望总计数: " + expectedTotal);
        System.out.println("实际总计数: " + actualTotal);
        System.out.println("结果: " + (expectedTotal == actualTotal ? "✓ 正确" : "✗ 错误"));

        System.out.println("\n各线程计数:");
        for (int i = 0; i < Math.min(THREAD_COUNT, 3); i++) {
            System.out.println("  thread-" + i + ": " + counters.get("thread-" + i).get());
        }
        System.out.println("  ...");

        System.out.println("\n耗时: " + (endTime - startTime) + "ms");
    }

    /**
     * Demo 4: 性能对比
     * ConcurrentHashMap vs Hashtable
     */
    private static void demo4_PerformanceComparison() throws Exception {
        System.out.println("--- 4. 性能对比 (ConcurrentHashMap vs Hashtable) ---");

        final int OPERATIONS = 100000;
        final int THREAD_COUNT = 8;

        // 测试ConcurrentHashMap
        ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();
        long concurrentTime = testMapPerformance(concurrentMap, THREAD_COUNT, OPERATIONS);

        // 测试Hashtable (全表锁)
        java.util.Hashtable<Integer, Integer> hashtable = new java.util.Hashtable<>();
        long hashtableTime = testMapPerformance(hashtable, THREAD_COUNT, OPERATIONS);

        System.out.println("\n性能结果:");
        System.out.println("ConcurrentHashMap: " + concurrentTime + "ms");
        System.out.println("Hashtable:         " + hashtableTime + "ms");
        System.out.println("性能提升: " + String.format("%.1f", (double) hashtableTime / concurrentTime) + "x");
    }

    /**
     * 测试Map的并发性能
     */
    private static long testMapPerformance(
            java.util.Map<Integer, Integer> map,
            int threadCount,
            int operationsPerThread
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    int key = threadId * operationsPerThread + j;
                    map.put(key, key);
                    map.get(key);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        return System.currentTimeMillis() - startTime;
    }
}

/**
 * 【知识点总结】
 *
 * 1. ConcurrentHashMap特点:
 *    - 线程安全，高并发性能
 *    - 分段锁(JDK7) / CAS+synchronized(JDK8+)
 *    - 不允许null键值
 *    - 弱一致性迭代器
 *
 * 2. 原子操作:
 *    - putIfAbsent(K, V): 不存在则插入
 *    - compute(K, BiFunction): 原子计算
 *    - computeIfAbsent(K, Function): 延迟初始化
 *    - computeIfPresent(K, BiFunction): 存在则更新
 *    - merge(K, V, BiFunction): 原子合并
 *
 * 3. 常见用法:
 *    - 并发计数器: computeIfAbsent + AtomicLong
 *    - 缓存: computeIfAbsent(key, k -> loadValue(k))
 *    - 分组统计: merge(key, 1, Integer::sum)
 *
 * 4. 注意事项:
 *    - 复合操作必须使用原子方法
 *    - size()是估算值（弱一致性）
 *    - 迭代器不会抛ConcurrentModificationException
 *    - 性能优于Hashtable和Collections.synchronizedMap
 */
