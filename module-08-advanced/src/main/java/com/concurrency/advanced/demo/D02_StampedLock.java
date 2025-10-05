package com.concurrency.advanced.demo;

import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.TimeUnit;

/**
 * Demo 02: StampedLock演示
 *
 * 本示例演示：
 * 1. 写锁（Write Lock）
 * 2. 悲观读锁（Pessimistic Read Lock）
 * 3. 乐观读（Optimistic Read）
 * 4. 锁升级（Lock Conversion）
 * 5. 性能对比
 */
public class D02_StampedLock {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== StampedLock演示 ===\n");

        // Demo 1: 基本用法
        demo1_BasicUsage();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 2: 乐观读演示
        demo2_OptimisticRead();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 3: 锁升级
        demo3_LockConversion();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 4: 性能对比
        demo4_PerformanceComparison();
    }

    /**
     * Demo 1: StampedLock基本用法
     */
    private static void demo1_BasicUsage() {
        System.out.println("--- Demo 1: StampedLock基本用法 ---\n");

        Point point = new Point(0, 0);

        // 写操作
        point.move(3, 4);
        System.out.println("移动到 (3, 4)");

        // 读操作
        double distance = point.distanceFromOrigin();
        System.out.println("到原点距离: " + distance);
        System.out.println("期望距离: " + Math.sqrt(3 * 3 + 4 * 4));
    }

    /**
     * Demo 2: 乐观读演示
     */
    private static void demo2_OptimisticRead() throws InterruptedException {
        System.out.println("--- Demo 2: 乐观读演示 ---\n");

        Point point = new Point(10, 20);

        // 读线程：使用乐观读
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                double distance = point.distanceFromOriginOptimistic();
                System.out.println("读线程" + i + ": 距离 = " + String.format("%.2f", distance));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Reader");

        // 写线程：偶尔修改
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(150);
                point.move(5, 5);
                System.out.println("写线程: 移动到 (15, 25)");

                Thread.sleep(200);
                point.move(10, 10);
                System.out.println("写线程: 移动到 (25, 35)");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Writer");

        reader.start();
        writer.start();

        reader.join();
        writer.join();

        System.out.println("\n说明: 读线程使用乐观读，写入时会自动验证并升级为悲观读");
    }

    /**
     * Demo 3: 锁升级示例
     */
    private static void demo3_LockConversion() {
        System.out.println("--- Demo 3: 锁升级 ---\n");

        Point point = new Point(0, 0);

        // 尝试在原点时移动
        point.moveIfAtOrigin(100, 200);
        System.out.println("条件移动后: (" + point.getX() + ", " + point.getY() + ")");

        // 再次尝试（不在原点）
        point.moveIfAtOrigin(999, 999);
        System.out.println("条件移动后: (" + point.getX() + ", " + point.getY() + ")");
    }

    /**
     * Demo 4: 性能对比
     */
    private static void demo4_PerformanceComparison() throws InterruptedException {
        System.out.println("--- Demo 4: 性能对比 ---\n");

        int readThreads = 8;
        int writeThreads = 2;
        int iterations = 100000;

        // 测试StampedLock
        Point stampedPoint = new Point(0, 0);
        long stampedTime = testPerformance(stampedPoint, readThreads, writeThreads, iterations, true);

        // 测试悲观读（模拟ReentrantReadWriteLock）
        Point pessimisticPoint = new Point(0, 0);
        long pessimisticTime = testPerformance(pessimisticPoint, readThreads, writeThreads, iterations, false);

        System.out.println("\n性能对比（读操作占90%）:");
        System.out.println("StampedLock乐观读: " + stampedTime + "ms");
        System.out.println("悲观读锁: " + pessimisticTime + "ms");
        if (pessimisticTime > 0) {
            double improvement = (double) (pessimisticTime - stampedTime) / pessimisticTime * 100;
            System.out.println("性能提升: " + String.format("%.1f", improvement) + "%");
        }
    }

    private static long testPerformance(Point point, int readThreads, int writeThreads,
                                       int iterations, boolean useOptimistic) throws InterruptedException {
        Thread[] threads = new Thread[readThreads + writeThreads];
        long startTime = System.currentTimeMillis();

        // 读线程
        for (int i = 0; i < readThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    if (useOptimistic) {
                        point.distanceFromOriginOptimistic();
                    } else {
                        point.distanceFromOriginPessimistic();
                    }
                }
            });
            threads[i].start();
        }

        // 写线程
        for (int i = 0; i < writeThreads; i++) {
            int offset = readThreads + i;
            threads[offset] = new Thread(() -> {
                for (int j = 0; j < iterations / 10; j++) {
                    point.move(1, 1);
                }
            });
            threads[offset].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 使用StampedLock的Point类
     */
    static class Point {
        private final StampedLock lock = new StampedLock();
        private double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * 写操作：移动点
         */
        public void move(double deltaX, double deltaY) {
            long stamp = lock.writeLock();
            try {
                x += deltaX;
                y += deltaY;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        /**
         * 乐观读：计算到原点的距离
         */
        public double distanceFromOriginOptimistic() {
            long stamp = lock.tryOptimisticRead(); // 获取乐观读stamp
            double currentX = x; // 读取数据
            double currentY = y;

            if (!lock.validate(stamp)) { // 验证期间是否有写入
                // 验证失败，升级为悲观读
                stamp = lock.readLock();
                try {
                    currentX = x;
                    currentY = y;
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            return Math.sqrt(currentX * currentX + currentY * currentY);
        }

        /**
         * 悲观读：计算到原点的距离
         */
        public double distanceFromOriginPessimistic() {
            long stamp = lock.readLock();
            try {
                return Math.sqrt(x * x + y * y);
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * 简化的乐观读（用于demo1）
         */
        public double distanceFromOrigin() {
            return distanceFromOriginOptimistic();
        }

        /**
         * 锁升级示例：如果在原点则移动
         */
        public void moveIfAtOrigin(double newX, double newY) {
            long stamp = lock.readLock();
            try {
                while (x == 0.0 && y == 0.0) {
                    // 尝试升级为写锁
                    long ws = lock.tryConvertToWriteLock(stamp);
                    if (ws != 0L) {
                        stamp = ws;
                        x = newX;
                        y = newY;
                        System.out.println("  锁升级成功，移动到 (" + newX + ", " + newY + ")");
                        return;
                    } else {
                        // 升级失败，释放读锁，获取写锁
                        lock.unlockRead(stamp);
                        stamp = lock.writeLock();
                        System.out.println("  锁升级失败，重新获取写锁");
                    }
                }
                System.out.println("  不在原点，无需移动");
            } finally {
                lock.unlock(stamp);
            }
        }

        public double getX() { return x; }
        public double getY() { return y; }
    }
}
