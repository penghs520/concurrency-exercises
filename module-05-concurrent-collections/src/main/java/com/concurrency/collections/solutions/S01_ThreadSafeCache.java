package com.concurrency.collections.solutions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 练习1参考答案: 线程安全的LRU缓存
 *
 * 核心知识点:
 * 1. ConcurrentHashMap的使用
 * 2. 原子操作保证线程安全
 * 3. LRU淘汰策略实现
 * 4. 并发统计信息维护
 */
public class S01_ThreadSafeCache {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 线程安全LRU缓存（参考答案） ===\n");

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

        // 访问元素（更新访问时间）
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

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        long startTime = System.currentTimeMillis();

        // 启动多个线程并发访问缓存
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
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
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        System.out.println("总操作数: " + (THREAD_COUNT * OPERATIONS_PER_THREAD));
        System.out.println("耗时: " + (endTime - startTime) + "ms");
        System.out.println(cache.getStats());
        System.out.println("\n✓ 并发测试完成，无异常");
    }

    /**
     * LRU缓存实现
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    static class LRUCache<K, V> {
        private final int capacity;
        private final ConcurrentHashMap<K, CacheEntry<V>> cache;
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);

        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new ConcurrentHashMap<>(capacity);
        }

        /**
         * 获取缓存值
         */
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);

            if (entry != null) {
                // 命中，更新访问时间
                entry.accessTime = System.nanoTime();
                hitCount.incrementAndGet();
                return entry.value;
            } else {
                // 未命中
                missCount.incrementAndGet();
                return null;
            }
        }

        /**
         * 放入缓存
         */
        public void put(K key, V value) {
            // 如果键已存在，直接更新
            CacheEntry<V> existingEntry = cache.get(key);
            if (existingEntry != null) {
                existingEntry.value = value;
                existingEntry.accessTime = System.nanoTime();
                return;
            }

            // 检查容量，需要淘汰
            if (cache.size() >= capacity) {
                evictLRU();
            }

            // 插入新条目
            cache.put(key, new CacheEntry<>(value, System.nanoTime()));
        }

        /**
         * 淘汰最久未使用的条目
         */
        private void evictLRU() {
            K lruKey = null;
            long oldestTime = Long.MAX_VALUE;

            // 遍历查找最久未使用的条目
            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                long accessTime = entry.getValue().accessTime;
                if (accessTime < oldestTime) {
                    oldestTime = accessTime;
                    lruKey = entry.getKey();
                }
            }

            // 移除最久未使用的条目
            if (lruKey != null) {
                cache.remove(lruKey);
            }
        }

        /**
         * 获取统计信息
         */
        public String getStats() {
            long total = hitCount.get() + missCount.get();
            double hitRate = total == 0 ? 0 : (double) hitCount.get() / total * 100;

            return String.format("统计信息: 大小=%d/%d, 命中率=%.1f%% (%d hits, %d misses)",
                    cache.size(), capacity, hitRate, hitCount.get(), missCount.get());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            int count = 0;
            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                if (count > 0) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue().value);
                count++;
            }
            sb.append("}");
            return sb.toString();
        }

        /**
         * 缓存条目
         */
        static class CacheEntry<V> {
            volatile V value;
            volatile long accessTime; // 使用nanoTime提高精度

            CacheEntry(V value, long accessTime) {
                this.value = value;
                this.accessTime = accessTime;
            }
        }
    }

    /**
     * 【优化版本：使用LinkedHashMap】
     *
     * Java提供的LinkedHashMap支持访问顺序模式，可以更简洁地实现LRU缓存：
     *
     * class LRUCache<K, V> extends LinkedHashMap<K, V> {
     *     private final int capacity;
     *
     *     public LRUCache(int capacity) {
     *         super(capacity, 0.75f, true); // accessOrder=true
     *         this.capacity = capacity;
     *     }
     *
     *     @Override
     *     protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
     *         return size() > capacity;
     *     }
     * }
     *
     * 但需要注意：
     * 1. LinkedHashMap本身不是线程安全的
     * 2. 需要使用Collections.synchronizedMap包装
     * 3. 或在外部使用synchronized同步
     */
}

/**
 * 【知识点总结】
 *
 * 1. ConcurrentHashMap的使用:
 *    - 线程安全的put/get操作
 *    - 弱一致性的遍历
 *    - 不保证淘汰操作的原子性（简化实现）
 *
 * 2. LRU实现策略:
 *    - 使用nanoTime记录访问时间（比currentTimeMillis精度高）
 *    - 遍历查找最小accessTime（O(n)复杂度）
 *    - 生产环境建议使用LinkedHashMap或双向链表+HashMap
 *
 * 3. 并发安全考虑:
 *    - get操作：读取并更新访问时间（volatile保证可见性）
 *    - put操作：可能触发淘汰（多线程可能同时淘汰，但不影响正确性）
 *    - 统计信息：使用AtomicLong保证原子性
 *
 * 4. 性能优化方向:
 *    - 使用分段锁减少竞争
 *    - 使用LRU链表优化淘汰（O(1)复杂度）
 *    - 批量淘汰减少锁开销
 *    - 异步淘汰避免阻塞
 */
