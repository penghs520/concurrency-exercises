package com.concurrency.advanced.solutions;

import java.util.concurrent.locks.StampedLock;
import java.util.Random;

/**
 * 解答2: 并发跳表实现
 *
 * 核心要点：
 * 1. 跳表通过多层索引加速查找（类似二分查找）
 * 2. 使用StampedLock的乐观读优化查询性能
 * 3. 随机层数保证跳表的平衡性
 * 4. 写操作使用写锁保证线程安全
 */
public class S02_ConcurrentSkipList {

    /**
     * 并发跳表实现
     */
    public static class ConcurrentSkipListSet {
        private static final int MAX_LEVEL = 4;
        private final Node head;
        private final StampedLock lock = new StampedLock();
        private final Random random = new Random();

        public ConcurrentSkipListSet() {
            this.head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        }

        /**
         * 添加元素
         */
        public boolean add(int value) {
            long stamp = lock.writeLock();
            try {
                // 检查是否已存在
                if (containsInternal(value)) {
                    return false;
                }

                // 随机决定层数
                int level = randomLevel();
                Node newNode = new Node(value, level);

                // 从最高层开始，找到每一层的插入位置
                Node current = head;
                for (int i = level - 1; i >= 0; i--) {
                    // 在第i层找到插入位置
                    while (current.next[i] != null && current.next[i].value < value) {
                        current = current.next[i];
                    }
                    // 插入新节点
                    newNode.next[i] = current.next[i];
                    current.next[i] = newNode;
                }

                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        /**
         * 查找元素（使用乐观读优化）
         */
        public boolean contains(int value) {
            // 尝试乐观读
            long stamp = lock.tryOptimisticRead();
            boolean found = containsInternal(value);

            if (!lock.validate(stamp)) {
                // 验证失败，升级为悲观读
                stamp = lock.readLock();
                try {
                    found = containsInternal(value);
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            return found;
        }

        /**
         * 内部查找方法（不加锁）
         */
        private boolean containsInternal(int value) {
            Node current = head;

            // 从最高层开始查找
            for (int i = MAX_LEVEL - 1; i >= 0; i--) {
                while (current.next[i] != null && current.next[i].value < value) {
                    current = current.next[i];
                }
            }

            // 检查第0层的下一个节点
            current = current.next[0];
            return current != null && current.value == value;
        }

        /**
         * 删除元素
         */
        public boolean remove(int value) {
            long stamp = lock.writeLock();
            try {
                if (!containsInternal(value)) {
                    return false;
                }

                // 记录每一层需要更新的节点
                Node[] update = new Node[MAX_LEVEL];
                Node current = head;

                // 找到每一层的前驱节点
                for (int i = MAX_LEVEL - 1; i >= 0; i--) {
                    while (current.next[i] != null && current.next[i].value < value) {
                        current = current.next[i];
                    }
                    update[i] = current;
                }

                // 要删除的节点
                Node nodeToRemove = current.next[0];

                if (nodeToRemove != null && nodeToRemove.value == value) {
                    // 更新每一层的指针
                    for (int i = 0; i < nodeToRemove.next.length; i++) {
                        update[i].next[i] = nodeToRemove.next[i];
                    }
                    return true;
                }

                return false;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        /**
         * 随机生成层数（抛硬币算法）
         * 每次50%概率增加一层
         */
        private int randomLevel() {
            int level = 1;
            while (level < MAX_LEVEL && random.nextBoolean()) {
                level++;
            }
            return level;
        }

        /**
         * 跳表节点
         */
        static class Node {
            final int value;
            final Node[] next;

            Node(int value, int level) {
                this.value = value;
                this.next = new Node[level];
            }
        }

        /**
         * 打印跳表结构
         */
        public void print() {
            System.out.println("SkipList structure:");
            for (int level = MAX_LEVEL - 1; level >= 0; level--) {
                System.out.print("Level " + level + ": HEAD");
                Node current = head.next[level];
                while (current != null) {
                    System.out.print(" -> " + current.value);
                    current = current.next[level];
                }
                System.out.println(" -> null");
            }
        }
    }

    // ========== 测试代码 ==========
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 并发跳表 - 参考答案 ===\n");

        // 测试1: 基本操作
        testBasicOperations();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试2: 并发测试
        testConcurrency();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试3: 性能测试
        testPerformance();
    }

    private static void testBasicOperations() {
        System.out.println("--- 测试1: 基本操作 ---\n");

        ConcurrentSkipListSet skipList = new ConcurrentSkipListSet();

        // 添加元素
        System.out.println("添加元素: 3, 1, 4, 1, 5, 9, 2, 6");
        System.out.println("add(3): " + skipList.add(3));
        System.out.println("add(1): " + skipList.add(1));
        System.out.println("add(4): " + skipList.add(4));
        System.out.println("add(1): " + skipList.add(1) + " (重复)");
        skipList.add(5);
        skipList.add(9);
        skipList.add(2);
        skipList.add(6);

        // 打印结构
        System.out.println();
        skipList.print();

        // 查找
        System.out.println("\n查找测试:");
        System.out.println("contains(5): " + skipList.contains(5) + " (期望: true)");
        System.out.println("contains(7): " + skipList.contains(7) + " (期望: false)");

        // 删除
        System.out.println("\n删除元素 4:");
        System.out.println("remove(4): " + skipList.remove(4));
        System.out.println("contains(4): " + skipList.contains(4) + " (期望: false)");

        System.out.println();
        skipList.print();
    }

    private static void testConcurrency() throws InterruptedException {
        System.out.println("--- 测试2: 并发测试 ---\n");

        ConcurrentSkipListSet skipList = new ConcurrentSkipListSet();
        int numThreads = 4;
        int opsPerThread = 1000;

        Thread[] threads = new Thread[numThreads];

        // 并发添加
        System.out.println("并发添加: " + numThreads + " 线程 x " + opsPerThread + " 操作");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            int start = i * opsPerThread;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    skipList.add(start + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("添加耗时: " + addTime + "ms");

        // 并发查找
        System.out.println("\n并发查找: " + numThreads + " 线程");
        startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                int found = 0;
                for (int j = 0; j < opsPerThread * numThreads; j++) {
                    if (skipList.contains(j)) {
                        found++;
                    }
                }
                System.out.println(Thread.currentThread().getName() + " 找到 " + found + " 个元素");
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long searchTime = System.currentTimeMillis() - startTime;
        System.out.println("\n查找耗时: " + searchTime + "ms");
    }

    private static void testPerformance() {
        System.out.println("--- 测试3: 性能对比 ---\n");

        ConcurrentSkipListSet skipList = new ConcurrentSkipListSet();
        int size = 10000;

        // 添加元素
        for (int i = 0; i < size; i++) {
            skipList.add((int) (Math.random() * size * 10));
        }

        // 测试查找性能
        long startTime = System.nanoTime();
        int found = 0;
        for (int i = 0; i < size * 10; i++) {
            if (skipList.contains(i)) {
                found++;
            }
        }
        long elapsedTime = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("数据规模: " + size);
        System.out.println("查找次数: " + (size * 10));
        System.out.println("找到元素: " + found);
        System.out.println("耗时: " + elapsedTime + "ms");
        System.out.println("平均查找: " + String.format("%.2f", (double) elapsedTime / (size * 10)) + "ms/op");
    }
}
