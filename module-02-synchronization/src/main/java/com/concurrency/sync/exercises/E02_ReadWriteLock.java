package com.concurrency.sync.exercises;

/**
 * 练习02: 简易读写锁实现 🟡
 *
 * 难度: 中级
 * 预计时间: 30分钟
 *
 * 任务描述:
 * 使用synchronized实现一个简单的读写锁，支持：
 * 1. readLock(): 获取读锁（多个读线程可以同时持有）
 * 2. readUnlock(): 释放读锁
 * 3. writeLock(): 获取写锁（独占，与读锁和其他写锁互斥）
 * 4. writeUnlock(): 释放写锁
 *
 * 读写锁规则:
 * - 多个读线程可以同时持有读锁
 * - 写线程独占，与所有读/写线程互斥
 * - 写优先：如果有写线程等待，不允许新的读线程获取锁
 *
 * 要求:
 * - 正确实现读写锁语义
 * - 避免死锁和饥饿
 * - 使用wait/notify协调
 *
 * 提示:
 * - 需要跟踪当前读线程数量和写线程数量
 * - 需要标记是否有写线程在等待
 */
public class E02_ReadWriteLock {

    // TODO: 添加必要的字段
    // 提示: readers计数、writers计数、waitingWriters计数

    public E02_ReadWriteLock() {
        // TODO: 初始化
    }

    /**
     * 获取读锁
     * 如果有写线程持有锁或等待，则等待
     */
    public void readLock() throws InterruptedException {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 释放读锁
     */
    public void readUnlock() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 获取写锁
     * 如果有任何读或写线程持有锁，则等待
     */
    public void writeLock() throws InterruptedException {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 释放写锁
     */
    public void writeUnlock() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现此方法");
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testReadWriteLock();
    }

    private static void testReadWriteLock() throws InterruptedException {
        E02_ReadWriteLock rwLock = new E02_ReadWriteLock();
        int[] sharedData = {0};

        // 创建多个读线程
        Thread[] readers = new Thread[3];
        for (int i = 0; i < readers.length; i++) {
            final int readerId = i + 1;
            readers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 3; j++) {
                        rwLock.readLock();
                        try {
                            System.out.println("读线程" + readerId + " 读取数据: " + sharedData[0]);
                            Thread.sleep(100);
                        } finally {
                            rwLock.readUnlock();
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "读线程" + readerId);
        }

        // 创建写线程
        Thread writer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    rwLock.writeLock();
                    try {
                        sharedData[0] = i * 10;
                        System.out.println("写线程 写入数据: " + sharedData[0]);
                        Thread.sleep(200);
                    } finally {
                        rwLock.writeUnlock();
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "写线程");

        // 启动所有线程
        for (Thread reader : readers) {
            reader.start();
        }
        writer.start();

        // 等待所有线程完成
        for (Thread reader : readers) {
            reader.join();
        }
        writer.join();

        System.out.println("测试完成！");
    }
}
