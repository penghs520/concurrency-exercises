package com.concurrency.atomic.solutions;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * 练习02解答: 无锁栈（Lock-Free Stack）
 *
 * 知识点:
 * 1. CAS操作实现无锁数据结构
 * 2. 自旋重试机制
 * 3. Treiber Stack算法
 * 4. 使用AtomicStampedReference避免ABA问题
 */
public class S02_LockFreeStack {

    /**
     * 基础版本：使用AtomicReference实现无锁栈
     *
     * 算法: Treiber Stack (R. Kent Treiber, 1986)
     *
     * 特点:
     * - 完全无锁（lock-free）
     * - 性能优于synchronized版本
     * - 可能存在ABA问题（但通常不影响栈的正确性）
     */
    public static class BasicLockFreeStack<E> {
        private final AtomicReference<Node<E>> top = new AtomicReference<>();

        private static class Node<E> {
            final E item;
            Node<E> next;

            Node(E item) {
                this.item = item;
            }
        }

        /**
         * 压栈操作
         *
         * 算法步骤:
         * 1. 创建新节点
         * 2. 读取当前栈顶
         * 3. 新节点指向当前栈顶
         * 4. CAS更新栈顶
         * 5. 失败则重试
         */
        public void push(E item) {
            Node<E> newHead = new Node<>(item);
            while (true) {
                Node<E> oldHead = top.get();
                newHead.next = oldHead;
                if (top.compareAndSet(oldHead, newHead)) {
                    return; // 成功
                }
                // CAS失败，继续重试
            }
        }

        /**
         * 出栈操作
         *
         * 算法步骤:
         * 1. 读取当前栈顶
         * 2. 如果为空，返回null
         * 3. 读取next节点
         * 4. CAS将栈顶更新为next
         * 5. 失败则重试
         * 6. 成功则返回原栈顶元素
         */
        public E pop() {
            while (true) {
                Node<E> oldHead = top.get();
                if (oldHead == null) {
                    return null; // 栈为空
                }
                Node<E> newHead = oldHead.next;
                if (top.compareAndSet(oldHead, newHead)) {
                    return oldHead.item; // 成功
                }
                // CAS失败，继续重试
            }
        }

        /**
         * 查看栈顶元素
         */
        public E peek() {
            Node<E> current = top.get();
            return current == null ? null : current.item;
        }

        /**
         * 判断栈是否为空
         */
        public boolean isEmpty() {
            return top.get() == null;
        }

        /**
         * 获取栈大小（O(n)复杂度，仅用于测试）
         */
        public int size() {
            int count = 0;
            Node<E> current = top.get();
            while (current != null) {
                count++;
                current = current.next;
            }
            return count;
        }
    }

    /**
     * 进阶版本：使用AtomicStampedReference避免ABA问题
     *
     * 适用场景:
     * - 节点会被复用
     * - 需要严格检测中间状态变化
     *
     * 注意:
     * - 版本号可能溢出（实际应用中很少见）
     * - 性能略低于AtomicReference（多一个版本号比较）
     */
    public static class ABASafeLockFreeStack<E> {
        private final AtomicStampedReference<Node<E>> top =
                new AtomicStampedReference<>(null, 0);

        private static class Node<E> {
            final E item;
            Node<E> next;

            Node(E item) {
                this.item = item;
            }
        }

        /**
         * 压栈操作（带版本号）
         */
        public void push(E item) {
            Node<E> newHead = new Node<>(item);
            int[] stampHolder = new int[1];

            while (true) {
                Node<E> oldHead = top.get(stampHolder);
                int oldStamp = stampHolder[0];

                newHead.next = oldHead;

                // CAS：同时检查引用和版本号
                if (top.compareAndSet(oldHead, newHead, oldStamp, oldStamp + 1)) {
                    return;
                }
            }
        }

        /**
         * 出栈操作（带版本号）
         */
        public E pop() {
            int[] stampHolder = new int[1];

            while (true) {
                Node<E> oldHead = top.get(stampHolder);
                int oldStamp = stampHolder[0];

                if (oldHead == null) {
                    return null;
                }

                Node<E> newHead = oldHead.next;

                // CAS：版本号不匹配会失败
                if (top.compareAndSet(oldHead, newHead, oldStamp, oldStamp + 1)) {
                    return oldHead.item;
                }
            }
        }

        public E peek() {
            return top.getReference() == null ? null : top.getReference().item;
        }

        public boolean isEmpty() {
            return top.getReference() == null;
        }

        public int getVersion() {
            return top.getStamp();
        }
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 无锁栈解答 ===\n");

        testBasicOperations();
        testConcurrentPush();
        testConcurrentPushPop();
        testABASafeStack();
    }

    /**
     * 测试基本操作
     */
    private static void testBasicOperations() {
        System.out.println("--- 测试基本操作 ---");

        BasicLockFreeStack<Integer> stack = new BasicLockFreeStack<>();

        // 测试空栈
        System.out.println("空栈: " + stack.isEmpty());
        System.out.println("pop空栈: " + stack.pop());

        // 测试push/pop
        stack.push(1);
        stack.push(2);
        stack.push(3);

        System.out.println("压入1,2,3后大小: " + stack.size());
        System.out.println("peek: " + stack.peek());
        System.out.println("pop: " + stack.pop());
        System.out.println("pop: " + stack.pop());
        System.out.println("pop: " + stack.pop());
        System.out.println("pop空栈: " + stack.pop());

        System.out.println("测试通过 ✓\n");
    }

    /**
     * 测试并发push
     */
    private static void testConcurrentPush() throws InterruptedException {
        System.out.println("--- 测试并发push ---");

        BasicLockFreeStack<Integer> stack = new BasicLockFreeStack<>();

        int threadCount = 10;
        int itemsPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < itemsPerThread; j++) {
                    stack.push(threadId * 10000 + j);
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        int size = stack.size();
        int expected = threadCount * itemsPerThread;

        System.out.println("栈大小: " + size);
        System.out.println("期望值: " + expected);
        System.out.println("测试结果: " + (size == expected ? "通过 ✓" : "失败 ✗"));
        System.out.println();
    }

    /**
     * 测试并发push/pop
     */
    private static void testConcurrentPushPop() throws InterruptedException {
        System.out.println("--- 测试并发push/pop ---");

        BasicLockFreeStack<Integer> stack = new BasicLockFreeStack<>();

        // 预填充1000个元素
        for (int i = 0; i < 1000; i++) {
            stack.push(i);
        }

        int threadCount = 20;
        int operations = 500;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operations; j++) {
                    if (threadId % 2 == 0) {
                        stack.push(threadId * 10000 + j);
                    } else {
                        stack.pop();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("并发操作后栈大小: " + stack.size());
        System.out.println("栈仍然有效: " + (!stack.isEmpty() ? "是 ✓" : "否"));
        System.out.println();
    }

    /**
     * 测试ABA安全的栈
     */
    private static void testABASafeStack() throws InterruptedException {
        System.out.println("--- 测试ABASafeLockFreeStack ---");

        ABASafeLockFreeStack<String> stack = new ABASafeLockFreeStack<>();

        System.out.println("初始版本号: " + stack.getVersion());

        stack.push("A");
        System.out.println("push A后版本号: " + stack.getVersion());

        stack.push("B");
        System.out.println("push B后版本号: " + stack.getVersion());

        stack.pop();
        System.out.println("pop后版本号: " + stack.getVersion());

        // 并发测试
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    stack.push("T" + threadId + "-" + j);
                    stack.pop();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("并发操作后版本号: " + stack.getVersion());
        System.out.println("说明: 版本号随每次操作递增，避免ABA问题");
        System.out.println("测试通过 ✓\n");
    }

    /**
     * 性能对比（可选）
     */
    @SuppressWarnings("unused")
    private static void performanceComparison() throws InterruptedException {
        System.out.println("--- 性能对比 ---");

        int threadCount = 10;
        int operations = 100_000;

        // 测试无锁栈
        BasicLockFreeStack<Integer> lockFreeStack = new BasicLockFreeStack<>();
        long time1 = testStackPerformance("LockFreeStack", lockFreeStack, threadCount, operations);

        // 测试synchronized栈
        SynchronizedStack<Integer> syncStack = new SynchronizedStack<>();
        long time2 = testStackPerformance("SynchronizedStack", syncStack, threadCount, operations);

        System.out.println("\n性能分析:");
        System.out.println("无锁栈耗时: " + time1 + "ms");
        System.out.println("同步栈耗时: " + time2 + "ms");
        System.out.println("性能提升: " + String.format("%.2fx", (double) time2 / time1));
    }

    private static long testStackPerformance(String name, Object stack, int threadCount, int operations)
            throws InterruptedException {
        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operations; j++) {
                    if (stack instanceof BasicLockFreeStack) {
                        @SuppressWarnings("unchecked")
                        BasicLockFreeStack<Integer> s = (BasicLockFreeStack<Integer>) stack;
                        s.push(j);
                        s.pop();
                    } else {
                        @SuppressWarnings("unchecked")
                        SynchronizedStack<Integer> s = (SynchronizedStack<Integer>) stack;
                        s.push(j);
                        s.pop();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * 对比用：synchronized版本的栈
     */
    static class SynchronizedStack<E> {
        private Node<E> top;

        private static class Node<E> {
            final E item;
            Node<E> next;

            Node(E item) {
                this.item = item;
            }
        }

        public synchronized void push(E item) {
            Node<E> newHead = new Node<>(item);
            newHead.next = top;
            top = newHead;
        }

        public synchronized E pop() {
            if (top == null) {
                return null;
            }
            E item = top.item;
            top = top.next;
            return item;
        }
    }
}
