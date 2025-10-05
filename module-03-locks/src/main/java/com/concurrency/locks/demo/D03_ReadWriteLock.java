package com.concurrency.locks.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Demo 03: ReadWriteLock读写锁演示
 *
 * 本示例演示：
 * 1. ReadWriteLock的基本用法
 * 2. 读锁共享、写锁独占
 * 3. 读写锁的性能优势（读多写少场景）
 * 4. 锁降级（写锁→读锁）
 * 5. 缓存系统的实现
 */
public class D03_ReadWriteLock {

    public static void main(String[] args) throws Exception {
        System.out.println("=== ReadWriteLock读写锁演示 ===\n");

        // 演示1: 读锁共享、写锁独占
        demo1_ReadWriteBehavior();
        Thread.sleep(3000);

        // 演示2: 性能对比（读多写少场景）
        demo2_PerformanceComparison();
        Thread.sleep(2000);

        // 演示3: 锁降级
        demo3_LockDowngrade();

        // 演示4: 缓存系统
        demo4_CacheSystem();

        System.out.println("\n所有演示完成！");
    }

    /**
     * 演示1: 读锁共享、写锁独占
     * 多个读操作可以并发执行，写操作独占
     */
    private static void demo1_ReadWriteBehavior() {
        System.out.println("--- 演示1: 读锁共享、写锁独占 ---");

        SharedData data = new SharedData();

        // 3个读线程 - 可以并发执行
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                data.read();
            }, "读线程-" + id).start();
        }

        try {
            Thread.sleep(100); // 让读线程先启动
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 1个写线程 - 独占执行
        new Thread(() -> {
            data.write(100);
        }, "写线程").start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示2: 性能对比
     * 读多写少场景下，ReadWriteLock性能优于普通Lock
     */
    private static void demo2_PerformanceComparison() {
        System.out.println("--- 演示2: 性能对比（读多写少场景） ---");

        int readCount = 10;  // 10个读操作
        int writeCount = 2;  // 2个写操作

        System.out.println("\n【使用ReadWriteLock】");
        long rwTime = testPerformance(new DataWithReadWriteLock(), readCount, writeCount);

        System.out.println("\n【使用普通Lock】");
        long lockTime = testPerformance(new DataWithRegularLock(), readCount, writeCount);

        System.out.println("\n性能对比:");
        System.out.println("  ReadWriteLock: " + rwTime + "ms");
        System.out.println("  普通Lock: " + lockTime + "ms");
        System.out.println("  性能提升: " + ((lockTime - rwTime) * 100.0 / lockTime) + "%");

        System.out.println();
    }

    private static long testPerformance(DataStore store, int readCount, int writeCount) {
        long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[readCount + writeCount];

        // 创建读线程
        for (int i = 0; i < readCount; i++) {
            threads[i] = new Thread(() -> store.read(), "读-" + i);
        }

        // 创建写线程
        for (int i = 0; i < writeCount; i++) {
            threads[readCount + i] = new Thread(() -> store.write(42), "写-" + i);
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 演示3: 锁降级
     * 支持写锁→读锁的降级，不支持读锁→写锁的升级
     */
    private static void demo3_LockDowngrade() {
        System.out.println("--- 演示3: 锁降级 ---");

        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        Lock readLock = rwLock.readLock();
        Lock writeLock = rwLock.writeLock();

        // ✓ 锁降级：写锁 → 读锁
        System.out.println("【锁降级示例】");
        writeLock.lock();
        System.out.println("1. 获取写锁");
        try {
            System.out.println("2. 修改数据...");
            Thread.sleep(100);

            // 在持有写锁的情况下获取读锁
            readLock.lock();
            System.out.println("3. 获取读锁（持有写锁）");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
            System.out.println("4. 释放写锁（仍持有读锁）");
        }

        try {
            System.out.println("5. 使用读锁读取数据...");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            readLock.unlock();
            System.out.println("6. 释放读锁");
        }

        System.out.println("\n【锁升级会死锁】");
        System.out.println("注意：读锁无法升级为写锁，会导致死锁！");
        System.out.println("// readLock.lock();");
        System.out.println("// writeLock.lock();  // 死锁！");

        System.out.println();
    }

    /**
     * 演示4: 缓存系统
     * 使用ReadWriteLock实现高性能缓存
     */
    private static void demo4_CacheSystem() throws InterruptedException {
        System.out.println("--- 演示4: 缓存系统 ---");

        Cache<String, String> cache = new Cache<>();

        // 写入缓存
        new Thread(() -> {
            cache.put("user:1", "张三");
            cache.put("user:2", "李四");
            System.out.println("缓存初始化完成");
        }, "初始化").start();

        Thread.sleep(100);

        // 多个读线程并发读取
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                String value = cache.get("user:1");
                System.out.println("读线程" + id + " 读取: user:1 = " + value);
            }, "读-" + i).start();
        }

        Thread.sleep(500);

        // 更新缓存
        new Thread(() -> {
            cache.put("user:1", "王五");
            System.out.println("缓存已更新");
        }, "更新").start();

        Thread.sleep(100);

        // 再次读取
        String value = cache.get("user:1");
        System.out.println("最终值: user:1 = " + value);

        System.out.println();
    }

    // ==================== 辅助类 ====================

    /**
     * 共享数据类 - 演示读写锁行为
     */
    static class SharedData {
        private int data = 0;
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();

        public int read() {
            readLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 开始读取");
                Thread.sleep(1000); // 模拟读取耗时
                System.out.println(Thread.currentThread().getName() + " 读取完成: " + data);
                return data;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return -1;
            } finally {
                readLock.unlock();
            }
        }

        public void write(int value) {
            writeLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 开始写入");
                Thread.sleep(1000); // 模拟写入耗时
                data = value;
                System.out.println(Thread.currentThread().getName() + " 写入完成: " + data);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * 数据存储接口
     */
    interface DataStore {
        int read();
        void write(int value);
    }

    /**
     * 使用ReadWriteLock的实现
     */
    static class DataWithReadWriteLock implements DataStore {
        private int data = 0;
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        @Override
        public int read() {
            rwLock.readLock().lock();
            try {
                Thread.sleep(50); // 模拟读取
                return data;
            } catch (InterruptedException e) {
                return -1;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        @Override
        public void write(int value) {
            rwLock.writeLock().lock();
            try {
                Thread.sleep(100); // 模拟写入
                data = value;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    /**
     * 使用普通Lock的实现
     */
    static class DataWithRegularLock implements DataStore {
        private int data = 0;
        private final Lock lock = new java.util.concurrent.locks.ReentrantLock();

        @Override
        public int read() {
            lock.lock();
            try {
                Thread.sleep(50); // 模拟读取
                return data;
            } catch (InterruptedException e) {
                return -1;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void write(int value) {
            lock.lock();
            try {
                Thread.sleep(100); // 模拟写入
                data = value;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 缓存系统实现
     * 使用ReadWriteLock保证线程安全
     */
    static class Cache<K, V> {
        private final Map<K, V> map = new HashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        /**
         * 读取缓存 - 使用读锁
         */
        public V get(K key) {
            rwLock.readLock().lock();
            try {
                System.out.println("[" + Thread.currentThread().getName() + "] 读取缓存: " + key);
                return map.get(key);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        /**
         * 写入缓存 - 使用写锁
         */
        public void put(K key, V value) {
            rwLock.writeLock().lock();
            try {
                System.out.println("[" + Thread.currentThread().getName() + "] 写入缓存: " + key + " = " + value);
                map.put(key, value);
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        /**
         * 删除缓存 - 使用写锁
         */
        public void remove(K key) {
            rwLock.writeLock().lock();
            try {
                map.remove(key);
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        /**
         * 清空缓存 - 使用写锁
         */
        public void clear() {
            rwLock.writeLock().lock();
            try {
                map.clear();
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
}
