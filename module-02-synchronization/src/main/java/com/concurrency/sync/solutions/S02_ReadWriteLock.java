package com.concurrency.sync.solutions;

/**
 * 练习02参考答案: 简易读写锁实现
 *
 * 实现要点:
 * 1. 维护读线程计数、写线程计数、等待写线程计数
 * 2. 读锁：多个线程可以同时持有，但需要检查写线程
 * 3. 写锁：独占，需要等待所有读/写线程释放
 * 4. 写优先：如果有写线程等待，阻止新的读线程获取锁
 */
public class S02_ReadWriteLock {

    private int readers = 0;           // 当前读线程数量
    private int writers = 0;           // 当前写线程数量（0或1）
    private int waitingWriters = 0;    // 等待的写线程数量

    public S02_ReadWriteLock() {
    }

    /**
     * 获取读锁
     * 条件: 没有写线程持有锁 且 没有写线程等待
     */
    public synchronized void readLock() throws InterruptedException {
        // 如果有写线程持有锁或等待，读线程需要等待
        while (writers > 0 || waitingWriters > 0) {
            wait();
        }
        readers++;
    }

    /**
     * 释放读锁
     */
    public synchronized void readUnlock() {
        readers--;
        // 如果是最后一个读线程，唤醒等待的写线程
        if (readers == 0) {
            notifyAll();
        }
    }

    /**
     * 获取写锁
     * 条件: 没有任何读线程和写线程
     */
    public synchronized void writeLock() throws InterruptedException {
        waitingWriters++;  // 标记有写线程在等待
        try {
            // 等待所有读线程和写线程释放锁
            while (readers > 0 || writers > 0) {
                wait();
            }
            writers++;
        } finally {
            waitingWriters--;
        }
    }

    /**
     * 释放写锁
     */
    public synchronized void writeUnlock() {
        writers--;
        notifyAll();  // 唤醒所有等待的读/写线程
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testReadWriteLock();
    }

    private static void testReadWriteLock() throws InterruptedException {
        S02_ReadWriteLock rwLock = new S02_ReadWriteLock();
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
