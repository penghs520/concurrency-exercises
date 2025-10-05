package com.concurrency.collections.exercises;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 练习1: 线程安全的LRU缓存 🟢
 *
 * 【题目描述】
 * 实现一个线程安全的LRU（Least Recently Used）缓存，具有固定容量。
 * 当缓存满时，移除最久未使用的条目。
 *
 * 【要求】
 * 1. 使用ConcurrentHashMap作为底层存储
 * 2. 实现LRU淘汰策略（最久未使用）
 * 3. 支持并发的put和get操作
 * 4. 提供统计信息（命中率、缓存大小）
 * 5. 线程安全且高性能
 *
 * 【学习目标】
 * - ConcurrentHashMap的使用
 * - 原子操作（compute系列方法）
 * - 缓存设计模式
 * - 并发安全的数据结构
 *
 * 【难度】: 🟢 基础
 */
public class E01_ThreadSafeCache {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 线程安全LRU缓存 ===\n");

        // 创建容量为3的缓存
        LRUCache<String, String> cache = new LRUCache<>(3);

        // 测试基本功能
        testBasicOperations(cache);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试并发性能
        testConcurrency();
    }

    /**
     * 测试基本操作
     */
    private static void testBasicOperations(LRUCache<String, String> cache) {
        System.out.println("--- 基本操作测试 ---");

        // 添加元素
        cache.put("A", "ValueA");
        cache.put("B", "ValueB");
        cache.put("C", "ValueC");
        System.out.println("添加3个元素: A, B, C");
        System.out.println("缓存内容: " + cache);

        // 访问元素
        System.out.println("\n访问A: " + cache.get("A"));
        System.out.println("访问B: " + cache.get("B"));

        // 添加第4个元素，应该淘汰C（最久未使用）
        System.out.println("\n添加第4个元素D（容量满，应淘汰C）:");
        cache.put("D", "ValueD");
        System.out.println("缓存内容: " + cache);

        // 验证C已被淘汰
        System.out.println("\n访问C: " + cache.get("C") + " (已被淘汰)");
        System.out.println("访问A: " + cache.get("A") + " (仍在缓存)");

        // 统计信息
        System.out.println("\n" + cache.getStats());
    }

    /**
     * 测试并发性能
     */
    private static void testConcurrency() throws Exception {
        System.out.println("--- 并发性能测试 ---");

        LRUCache<Integer, String> cache = new LRUCache<>(1000);
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 10000;

        Thread[] threads = new Thread[THREAD_COUNT];

        long startTime = System.currentTimeMillis();

        // 启动多个线程并发访问缓存
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    int key = (threadId * OPERATIONS_PER_THREAD + j) % 2000;

                    // 50%写，50%读
                    if (j % 2 == 0) {
                        cache.put(key, "Value-" + key);
                    } else {
                        cache.get(key);
                    }
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("总操作数: " + (THREAD_COUNT * OPERATIONS_PER_THREAD));
        System.out.println("耗时: " + (endTime - startTime) + "ms");
        System.out.println(cache.getStats());
        System.out.println("\n✓ 并发测试完成，无异常");
    }

    /**
     * LRU缓存实现
     * TODO: 完成实现
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    static class LRUCache<K, V> {
        private final int capacity;
        private final ConcurrentHashMap<K, CacheEntry<V>> cache;
        private long hitCount = 0;
        private long missCount = 0;

        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new ConcurrentHashMap<>();
        }

        /**
         * 获取缓存值
         * TODO: 实现get方法
         *
         * 提示：
         * 1. 从cache中获取CacheEntry
         * 2. 如果存在，更新访问时间
         * 3. 更新命中/未命中统计
         * 4. 返回值
         */
        public V get(K key) {
            // TODO: 实现
            return null;
        }

        /**
         * 放入缓存
         * TODO: 实现put方法
         *
         * 提示：
         * 1. 如果缓存满了，需要淘汰最久未使用的条目
         * 2. 使用ConcurrentHashMap的compute方法保证原子性
         * 3. 创建新的CacheEntry并设置访问时间
         *
         * 简化版LRU策略提示：
         * - 可以简单地通过比较访问时间来找到最久未使用的条目
         * - 遍历所有条目，找到accessTime最小的
         */
        public void put(K key, V value) {
            // TODO: 实现

            // 提示：淘汰逻辑伪代码
            // if (cache.size() >= capacity && !cache.containsKey(key)) {
            //     K oldestKey = findLRUKey();
            //     cache.remove(oldestKey);
            // }
            // cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
        }

        /**
         * 查找最久未使用的键
         * TODO: 实现LRU查找
         */
        private K findLRUKey() {
            // TODO: 实现
            // 提示：遍历cache.entrySet()，找到accessTime最小的
            return null;
        }

        /**
         * 获取统计信息
         */
        public String getStats() {
            long total = hitCount + missCount;
            double hitRate = total == 0 ? 0 : (double) hitCount / total * 100;

            return String.format("统计信息: 大小=%d/%d, 命中率=%.1f%% (%d hits, %d misses)",
                    cache.size(), capacity, hitRate, hitCount, missCount);
        }

        @Override
        public String toString() {
            return cache.toString();
        }

        /**
         * 缓存条目
         */
        static class CacheEntry<V> {
            final V value;
            volatile long accessTime; // 最后访问时间

            CacheEntry(V value, long accessTime) {
                this.value = value;
                this.accessTime = accessTime;
            }

            @Override
            public String toString() {
                return String.valueOf(value);
            }
        }
    }
}

/**
 * 【参考输出】
 * === 线程安全LRU缓存 ===
 *
 * --- 基本操作测试 ---
 * 添加3个元素: A, B, C
 * 缓存内容: {A=ValueA, B=ValueB, C=ValueC}
 *
 * 访问A: ValueA
 * 访问B: ValueB
 *
 * 添加第4个元素D（容量满，应淘汰C）:
 * 缓存内容: {A=ValueA, B=ValueB, D=ValueD}
 *
 * 访问C: null (已被淘汰)
 * 访问A: ValueA (仍在缓存)
 *
 * 统计信息: 大小=3/3, 命中率=75.0% (3 hits, 1 misses)
 *
 * ==================================================
 *
 * --- 并发性能测试 ---
 * 总操作数: 100000
 * 耗时: XXXms
 * 统计信息: 大小=1000/1000, 命中率=XX.X% (XXXX hits, XXXX misses)
 *
 * ✓ 并发测试完成，无异常
 */

/**
 * 【扩展思考】
 *
 * 1. 性能优化:
 *    - 当前实现每次put都可能遍历整个map查找LRU，O(n)复杂度
 *    - 可以使用LinkedHashMap的访问顺序特性
 *    - 或使用单独的优先队列维护访问顺序
 *
 * 2. 更精确的LRU:
 *    - 使用双向链表 + HashMap（类似LinkedHashMap）
 *    - 访问时移动到链表头部
 *    - 淘汰时从链表尾部移除
 *
 * 3. 线程安全的权衡:
 *    - 完全无锁（CAS）vs 细粒度锁
 *    - 读写锁优化读性能
 *    - 分段锁减少竞争
 *
 * 4. 其他缓存策略:
 *    - LFU (Least Frequently Used)
 *    - FIFO (First In First Out)
 *    - TTL (Time To Live)
 */
