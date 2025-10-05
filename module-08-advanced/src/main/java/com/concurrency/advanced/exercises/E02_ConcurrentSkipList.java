package com.concurrency.advanced.exercises;

import java.util.concurrent.locks.StampedLock;
import java.util.Random;

/**
 * 练习2: 并发跳表实现 🔴
 *
 * 任务描述：
 * 使用StampedLock实现一个简化的并发跳表（Skip List）
 *
 * 跳表介绍：
 * - 跳表是一种随机化的数据结构，类似于平衡树
 * - 通过多层索引加速查找，平均时间复杂度O(log n)
 * - Redis的有序集合底层实现之一
 *
 * 要求：
 * 1. 实现add()方法：添加元素（支持并发）
 * 2. 实现contains()方法：查找元素（使用乐观读优化）
 * 3. 实现remove()方法：删除元素
 * 4. 支持最大4层索引
 * 5. 使用StampedLock保证线程安全
 *
 * 提示：
 * - 跳表的层数通过随机数决定（抛硬币）
 * - 查找操作可以使用乐观读提高性能
 * - 修改操作需要使用写锁
 *
 * 简化要求：
 * - 只需支持Integer类型
 * - 不需要实现完整的跳表（最大4层即可）
 * - 查找优先实现，添加和删除可以简化
 */
public class E02_ConcurrentSkipList {

    /**
     * TODO: 实现并发跳表
     */
    static class ConcurrentSkipListSet {
        private static final int MAX_LEVEL = 4; // 最大层数
        private final Node head; // 头节点
        private final StampedLock lock = new StampedLock();
        private final Random random = new Random();

        public ConcurrentSkipListSet() {
            // 初始化头节点（所有层）
            this.head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        }

        /**
         * TODO: 添加元素
         *
         * @param value 要添加的值
         * @return 是否成功添加（已存在返回false）
         */
        public boolean add(int value) {
            // TODO: 实现添加逻辑
            // 1. 随机决定层数
            // 2. 从最高层开始查找插入位置
            // 3. 使用写锁保护插入操作
            // 4. 更新每一层的指针
            throw new UnsupportedOperationException("请实现add方法");
        }

        /**
         * TODO: 查找元素（乐观读）
         *
         * @param value 要查找的值
         * @return 是否存在
         */
        public boolean contains(int value) {
            // TODO: 实现查找逻辑（使用乐观读优化）
            // 1. 使用tryOptimisticRead()获取stamp
            // 2. 从最高层开始查找
            // 3. 如果validate失败，升级为悲观读
            throw new UnsupportedOperationException("请实现contains方法");
        }

        /**
         * TODO: 删除元素
         *
         * @param value 要删除的值
         * @return 是否成功删除（不存在返回false）
         */
        public boolean remove(int value) {
            // TODO: 实现删除逻辑
            // 1. 查找要删除的节点
            // 2. 使用写锁保护删除操作
            // 3. 更新每一层的指针
            throw new UnsupportedOperationException("请实现remove方法");
        }

        /**
         * 随机生成层数（抛硬币）
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
            final Node[] next; // 每一层的next指针

            Node(int value, int level) {
                this.value = value;
                this.next = new Node[level];
            }
        }

        /**
         * 打印跳表结构（调试用）
         */
        public void print() {
            System.out.println("SkipList structure:");
            for (int level = MAX_LEVEL - 1; level >= 0; level--) {
                System.out.print("Level " + level + ": ");
                Node current = head.next[level];
                while (current != null) {
                    System.out.print(current.value + " -> ");
                    current = current.next[level];
                }
                System.out.println("null");
            }
        }
    }

    // ========== 测试代码 ==========
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 并发跳表测试 ===\n");

        // 测试1: 基本操作
        testBasicOperations();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试2: 并发测试
        testConcurrency();
    }

    private static void testBasicOperations() {
        System.out.println("--- 测试1: 基本操作 ---\n");

        try {
            ConcurrentSkipListSet skipList = new ConcurrentSkipListSet();

            // 添加元素
            System.out.println("添加元素: 3, 1, 4, 1, 5, 9, 2, 6");
            skipList.add(3);
            skipList.add(1);
            skipList.add(4);
            skipList.add(1); // 重复
            skipList.add(5);
            skipList.add(9);
            skipList.add(2);
            skipList.add(6);

            // 打印结构
            skipList.print();

            // 查找
            System.out.println("\n查找测试:");
            System.out.println("contains(5): " + skipList.contains(5) + " (期望: true)");
            System.out.println("contains(7): " + skipList.contains(7) + " (期望: false)");

            // 删除
            System.out.println("\n删除元素: 4");
            skipList.remove(4);
            skipList.print();

        } catch (UnsupportedOperationException e) {
            System.out.println("TODO: 请实现跳表的基本操作");
        }
    }

    private static void testConcurrency() throws InterruptedException {
        System.out.println("--- 测试2: 并发测试 ---\n");

        try {
            ConcurrentSkipListSet skipList = new ConcurrentSkipListSet();
            int numThreads = 4;
            int opsPerThread = 1000;

            Thread[] threads = new Thread[numThreads];

            // 并发添加
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

            System.out.println("并发添加完成: " + numThreads + " 线程 x " + opsPerThread + " 操作");

            // 并发查找
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    int found = 0;
                    for (int j = 0; j < opsPerThread * numThreads; j++) {
                        if (skipList.contains(j)) {
                            found++;
                        }
                    }
                    System.out.println("查找到 " + found + " 个元素");
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("\n并发查找耗时: " + elapsedTime + "ms");

        } catch (UnsupportedOperationException e) {
            System.out.println("TODO: 请实现跳表的基本操作");
        }
    }
}
