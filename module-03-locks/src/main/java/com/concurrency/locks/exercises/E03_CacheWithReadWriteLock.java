package com.concurrency.locks.exercises;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 练习03: 读写锁缓存实现 🟡
 *
 * 难度: 中等
 * 预计时间: 40分钟
 *
 * 任务描述:
 * 使用ReadWriteLock实现一个线程安全的缓存系统，支持读多写少的场景。
 *
 * 要求:
 * 1. 使用ReadWriteLock保护缓存数据
 * 2. 读操作使用读锁（共享锁）
 * 3. 写操作使用写锁（排他锁）
 * 4. 实现缓存未命中时的加载逻辑（模拟从数据库加载）
 * 5. 使用锁降级优化性能
 *
 * 提示:
 * - 使用HashMap作为底层存储
 * - get时先获取读锁，如果缓存未命中：
 *   1. 释放读锁
 *   2. 获取写锁
 *   3. 双重检查（其他线程可能已加载）
 *   4. 加载数据
 *   5. 锁降级：持有写锁时获取读锁，然后释放写锁
 * - put和remove使用写锁
 */
public class E03_CacheWithReadWriteLock<K, V> {

    // TODO: 添加必要的字段
    // private final Map<K, V> cache = new HashMap<>();
    // private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    // private final Lock readLock = rwLock.readLock();
    // private final Lock writeLock = rwLock.writeLock();

    /**
     * 数据加载器接口
     * 用于在缓存未命中时加载数据
     */
    public interface DataLoader<K, V> {
        V load(K key);
    }

    private final DataLoader<K, V> loader;

    public E03_CacheWithReadWriteLock(DataLoader<K, V> loader) {
        this.loader = loader;
        // TODO: 初始化字段
    }

    /**
     * 获取缓存数据
     * TODO: 实现缓存读取逻辑，支持缓存未命中时自动加载
     *
     * 步骤:
     * 1. 先获取读锁，检查缓存
     * 2. 如果命中，返回数据
     * 3. 如果未命中：
     *    - 释放读锁
     *    - 获取写锁
     *    - 双重检查（其他线程可能已加载）
     *    - 使用loader加载数据
     *    - 存入缓存
     *    - 锁降级：获取读锁再释放写锁
     *    - 返回数据
     */
    public V get(K key) {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 写入缓存
     * TODO: 使用写锁保护
     */
    public void put(K key, V value) {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 删除缓存
     * TODO: 使用写锁保护
     */
    public void remove(K key) {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 清空缓存
     * TODO: 使用写锁保护
     */
    public void clear() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 获取缓存大小
     * TODO: 使用读锁保护
     */
    public int size() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 判断缓存是否包含指定key
     * TODO: 使用读锁保护
     */
    public boolean containsKey(K key) {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
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

        E03_CacheWithReadWriteLock<String, String> cache =
                new E03_CacheWithReadWriteLock<>(dbLoader);

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
