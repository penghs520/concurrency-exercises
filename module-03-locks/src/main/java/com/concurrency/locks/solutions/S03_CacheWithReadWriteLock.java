package com.concurrency.locks.solutions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 练习03参考答案: 读写锁缓存实现
 *
 * 实现要点:
 * 1. 使用ReadWriteLock提高读多写少场景的性能
 * 2. get方法使用读锁（允许并发读取）
 * 3. put/remove/clear使用写锁（独占写入）
 * 4. 缓存未命中时使用锁降级优化性能
 * 5. 双重检查避免重复加载
 */
public class S03_CacheWithReadWriteLock<K, V> {

    private final Map<K, V> cache = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    /**
     * 数据加载器接口
     * 用于在缓存未命中时加载数据
     */
    public interface DataLoader<K, V> {
        V load(K key);
    }

    private final DataLoader<K, V> loader;

    public S03_CacheWithReadWriteLock(DataLoader<K, V> loader) {
        this.loader = loader;
    }

    /**
     * 获取缓存数据
     * 使用锁降级模式：写锁 → 读锁
     *
     * 步骤:
     * 1. 先获取读锁，检查缓存
     * 2. 如果命中，返回数据
     * 3. 如果未命中：
     *    - 释放读锁
     *    - 获取写锁
     *    - 双重检查（其他线程可能已加载）
     *    - 加载数据
     *    - 存入缓存
     *    - 锁降级：获取读锁再释放写锁
     *    - 返回数据
     */
    public V get(K key) {
        V value;

        // 第一步：使用读锁尝试获取缓存
        readLock.lock();
        try {
            value = cache.get(key);
            if (value != null) {
                // 缓存命中，直接返回
                return value;
            }
        } finally {
            readLock.unlock();
        }

        // 第二步：缓存未命中，需要加载数据
        writeLock.lock();
        try {
            // 双重检查：其他线程可能已经加载了数据
            value = cache.get(key);
            if (value != null) {
                return value;
            }

            // 加载数据（模拟从数据库加载）
            value = loader.load(key);

            // 存入缓存
            cache.put(key, value);

            // 关键点：锁降级
            // 在释放写锁之前获取读锁，确保数据一致性
            readLock.lock();
        } finally {
            writeLock.unlock();
        }

        // 第三步：使用读锁返回数据
        try {
            return value;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 写入缓存
     * 使用写锁保护
     */
    public void put(K key, V value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 删除缓存
     * 使用写锁保护
     */
    public void remove(K key) {
        writeLock.lock();
        try {
            cache.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 清空缓存
     * 使用写锁保护
     */
    public void clear() {
        writeLock.lock();
        try {
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 获取缓存大小
     * 使用读锁保护
     */
    public int size() {
        readLock.lock();
        try {
            return cache.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 判断缓存是否包含指定key
     * 使用读锁保护
     */
    public boolean containsKey(K key) {
        readLock.lock();
        try {
            return cache.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testCache();
    }

    private static void testCache() throws InterruptedException {
        System.out.println("=== 缓存系统测试 ===\n");

        // 模拟从数据库加载数据
        DataLoader<String, String> dbLoader = key -> {
            System.out.println("  [从数据库加载: " + key + "]");
            try {
                Thread.sleep(500); // 模拟数据库查询耗时
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Data-" + key;
        };

        S03_CacheWithReadWriteLock<String, String> cache =
                new S03_CacheWithReadWriteLock<>(dbLoader);

        // 创建多个读线程
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            new Thread(() -> {
                // 每个线程读取3次相同的key
                for (int j = 1; j <= 3; j++) {
                    String value = cache.get("user:1");
                    System.out.println("读线程" + id + " 第" + j + "次读取: " + value);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "读线程-" + i).start();
        }

        Thread.sleep(2000);
        System.out.println("\n--- 测试缓存更新 ---");

        // 创建写线程
        new Thread(() -> {
            System.out.println("写线程: 更新缓存");
            cache.put("user:1", "Updated-Data");
        }, "写线程").start();

        Thread.sleep(500);

        // 再次读取
        new Thread(() -> {
            String value = cache.get("user:1");
            System.out.println("读线程: 读取更新后的数据: " + value);
        }, "读线程-验证").start();

        Thread.sleep(1000);

        System.out.println("\n--- 测试缓存未命中 ---");

        // 测试多个线程同时访问未缓存的key
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                String value = cache.get("user:2");
                System.out.println("并发读线程" + id + ": " + value);
            }, "并发读-" + i).start();
        }

        Thread.sleep(2000);

        System.out.println("\n缓存大小: " + cache.size());
        System.out.println("✓ 测试完成！");
    }
}
