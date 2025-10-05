package com.concurrency.collections.demo;

import java.util.*;
import java.util.concurrent.*;

/**
 * Demo 03: CopyOnWriteArrayList - 写时复制列表
 *
 * 本示例演示:
 * 1. CopyOnWriteArrayList的基本特性
 * 2. 读写分离机制
 * 3. 迭代器的快照特性
 * 4. 适用场景与性能对比
 */
public class D03_CopyOnWriteArrayList {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CopyOnWriteArrayList演示 ===\n");

        // 1. 基本特性
        demo1_BasicFeatures();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 迭代器快照特性
        demo2_SnapshotIterator();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 3. 并发安全性验证
        demo3_ConcurrentSafety();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 4. 性能对比
        demo4_PerformanceComparison();
        System.out.println("\n" + "=".repeat(50) + "\n");

        // 5. 典型应用场景
        demo5_TypicalUseCase();
    }

    /**
     * Demo 1: 基本特性
     */
    private static void demo1_BasicFeatures() {
        System.out.println("--- 1. 基本特性 ---");

        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

        // 添加元素
        list.add("A");
        list.add("B");
        list.add("C");
        System.out.println("初始列表: " + list);

        // 读操作（无锁）
        String element = list.get(1);
        System.out.println("读取索引1: " + element);

        // 写操作（复制整个数组）
        list.add("D");
        System.out.println("添加D后: " + list);

        // 删除操作（复制整个数组）
        list.remove("B");
        System.out.println("删除B后: " + list);

        // 批量操作
        System.out.println("\n批量操作:");
        list.addAll(Arrays.asList("E", "F", "G"));
        System.out.println("  批量添加后: " + list);
        System.out.println("  提示: 批量操作只复制一次数组，效率更高");
    }

    /**
     * Demo 2: 迭代器快照特性
     */
    private static void demo2_SnapshotIterator() {
        System.out.println("--- 2. 迭代器快照特性 ---");

        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(
                Arrays.asList("A", "B", "C")
        );

        System.out.println("初始列表: " + list);

        // 创建迭代器（获取当前快照）
        Iterator<String> iterator = list.iterator();

        // 迭代过程中修改列表
        list.add("D");
        list.add("E");
        System.out.println("\n迭代过程中添加了D和E");
        System.out.println("当前列表: " + list);

        // 迭代器仍然遍历旧快照
        System.out.println("\n迭代器输出（快照）:");
        while (iterator.hasNext()) {
            System.out.println("  " + iterator.next());
        }

        System.out.println("\n重新创建迭代器:");
        for (String s : list) {
            System.out.println("  " + s); // 新迭代器可以看到D和E
        }

        // 对比ArrayList（会抛异常）
        System.out.println("\n对比ArrayList:");
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("A", "B", "C"));
        Iterator<String> arrayIterator = arrayList.iterator();
        arrayList.add("D"); // 修改列表

        try {
            while (arrayIterator.hasNext()) {
                arrayIterator.next(); // 抛出ConcurrentModificationException
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("  ✗ ArrayList抛出异常: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Demo 3: 并发安全性验证
     */
    private static void demo3_ConcurrentSafety() throws Exception {
        System.out.println("--- 3. 并发安全性验证 ---");

        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
        final int THREAD_COUNT = 10;
        final int ADDITIONS_PER_THREAD = 100;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        long startTime = System.currentTimeMillis();

        // 10个线程并发添加
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < ADDITIONS_PER_THREAD; j++) {
                    list.add(threadId * 1000 + j);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        // 验证结果
        int expectedSize = THREAD_COUNT * ADDITIONS_PER_THREAD;
        int actualSize = list.size();

        System.out.println("期望大小: " + expectedSize);
        System.out.println("实际大小: " + actualSize);
        System.out.println("结果: " + (expectedSize == actualSize ? "✓ 正确" : "✗ 错误"));
        System.out.println("耗时: " + (endTime - startTime) + "ms");

        // 并发读取测试
        System.out.println("\n并发读取测试:");
        CountDownLatch readLatch = new CountDownLatch(THREAD_COUNT);
        ExecutorService readExecutor = Executors.newFixedThreadPool(THREAD_COUNT);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            readExecutor.submit(() -> {
                for (int j = 0; j < 10000; j++) {
                    int index = ThreadLocalRandom.current().nextInt(list.size());
                    list.get(index); // 并发读取，完全无锁
                }
                readLatch.countDown();
            });
        }

        readLatch.await();
        readExecutor.shutdown();

        endTime = System.currentTimeMillis();
        System.out.println("  10个线程各读取10000次");
        System.out.println("  耗时: " + (endTime - startTime) + "ms (完全无锁)");
    }

    /**
     * Demo 4: 性能对比
     * CopyOnWriteArrayList vs Collections.synchronizedList
     */
    private static void demo4_PerformanceComparison() throws Exception {
        System.out.println("--- 4. 性能对比 ---");

        final int READ_THREADS = 20;
        final int WRITE_THREADS = 2;
        final int READS_PER_THREAD = 10000;
        final int WRITES_PER_THREAD = 100;

        // 初始数据
        List<Integer> initData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            initData.add(i);
        }

        // 测试CopyOnWriteArrayList
        CopyOnWriteArrayList<Integer> cowList = new CopyOnWriteArrayList<>(initData);
        long cowTime = testListPerformance(cowList, READ_THREADS, WRITE_THREADS,
                READS_PER_THREAD, WRITES_PER_THREAD);

        // 测试SynchronizedList
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>(initData));
        long syncTime = testListPerformance(syncList, READ_THREADS, WRITE_THREADS,
                READS_PER_THREAD, WRITES_PER_THREAD);

        System.out.println("\n场景: 读多写少 (20个读线程, 2个写线程)");
        System.out.println("CopyOnWriteArrayList: " + cowTime + "ms");
        System.out.println("SynchronizedList:     " + syncTime + "ms");
        System.out.println("性能提升: " + String.format("%.1f", (double) syncTime / cowTime) + "x");

        System.out.println("\n结论:");
        System.out.println("  ✓ 读多写少场景，CopyOnWriteArrayList性能更好");
        System.out.println("  ✓ 写操作频繁场景，应使用SynchronizedList");
    }

    /**
     * 测试List的并发性能
     */
    private static long testListPerformance(
            List<Integer> list,
            int readThreads,
            int writeThreads,
            int readsPerThread,
            int writesPerThread
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(readThreads + writeThreads);
        CountDownLatch latch = new CountDownLatch(readThreads + writeThreads);
        long startTime = System.currentTimeMillis();

        // 读线程
        for (int i = 0; i < readThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < readsPerThread; j++) {
                        if (!list.isEmpty()) {
                            int index = ThreadLocalRandom.current().nextInt(list.size());
                            list.get(index);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 写线程
        for (int i = 0; i < writeThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < writesPerThread; j++) {
                        list.add(threadId * 1000 + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        return System.currentTimeMillis() - startTime;
    }

    /**
     * Demo 5: 典型应用场景 - 事件监听器管理
     */
    private static void demo5_TypicalUseCase() {
        System.out.println("--- 5. 典型应用场景: 事件监听器管理 ---");

        EventManager eventManager = new EventManager();

        // 注册监听器
        eventManager.addListener(event ->
            System.out.println("  [监听器1] 收到事件: " + event)
        );

        eventManager.addListener(event ->
            System.out.println("  [监听器2] 收到事件: " + event)
        );

        eventManager.addListener(event ->
            System.out.println("  [监听器3] 收到事件: " + event)
        );

        // 触发事件（读操作，无锁，高性能）
        System.out.println("\n触发事件:");
        eventManager.fireEvent("UserLogin");

        System.out.println("\n添加新监听器:");
        eventManager.addListener(event ->
            System.out.println("  [监听器4] 收到事件: " + event)
        );

        System.out.println("\n再次触发事件:");
        eventManager.fireEvent("DataUpdated");

        System.out.println("\n优势:");
        System.out.println("  ✓ 触发事件时无需加锁（读操作）");
        System.out.println("  ✓ 添加/删除监听器安全（写操作加锁）");
        System.out.println("  ✓ 迭代时不会抛ConcurrentModificationException");
    }

    /**
     * 事件管理器（使用CopyOnWriteArrayList）
     */
    static class EventManager {
        // 监听器列表：读多（触发事件）写少（添加/删除监听器）
        private final CopyOnWriteArrayList<EventListener> listeners
                = new CopyOnWriteArrayList<>();

        public void addListener(EventListener listener) {
            listeners.add(listener);
        }

        public void removeListener(EventListener listener) {
            listeners.remove(listener);
        }

        public void fireEvent(String event) {
            // 迭代监听器列表，无需加锁
            for (EventListener listener : listeners) {
                listener.onEvent(event);
            }
        }
    }

    @FunctionalInterface
    interface EventListener {
        void onEvent(String event);
    }
}

/**
 * 【知识点总结】
 *
 * 1. CopyOnWriteArrayList特点:
 *    - 读操作完全无锁（volatile数组）
 *    - 写操作复制整个数组（加锁）
 *    - 迭代器是快照，不会抛ConcurrentModificationException
 *    - 最终一致性（弱一致性）
 *
 * 2. 实现原理:
 *    - 内部维护volatile Object[] array
 *    - 写操作: 复制数组 -> 修改副本 -> 原子替换
 *    - 读操作: 直接访问数组，无需加锁
 *
 * 3. 适用场景:
 *    ✓ 读操作远多于写操作
 *    ✓ 集合较小（复制开销可接受）
 *    ✓ 可以容忍短暂的数据不一致
 *    典型应用：监听器列表、配置项、白名单/黑名单
 *
 * 4. 不适用场景:
 *    ✗ 写操作频繁（每次写都复制整个数组）
 *    ✗ 集合很大（内存和性能开销高）
 *    ✗ 需要实时一致性
 *
 * 5. 最佳实践:
 *    - 批量操作用addAll/removeAll（减少复制次数）
 *    - 不要在写密集场景使用
 *    - 理解迭代器的快照特性
 *    - 配合不可变对象使用更安全
 */
