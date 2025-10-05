package com.concurrency.atomic.exercises;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 练习02: 无锁栈（Lock-Free Stack）
 *
 * 难度: 🟡 中等
 *
 * 任务描述:
 * 使用AtomicReference和CAS操作实现一个无锁栈（Treiber Stack）。
 * 这是经典的无锁数据结构，广泛应用于并发编程。
 *
 * 功能要求:
 * 1. 实现push方法（压栈）
 * 2. 实现pop方法（出栈）
 * 3. 实现peek方法（查看栈顶但不移除）
 * 4. 实现isEmpty方法
 * 5. 保证线程安全（不使用锁）
 *
 * 核心挑战:
 * - 使用CAS实现原子地修改栈顶指针
 * - 处理并发冲突（CAS失败时重试）
 * - 避免ABA问题（思考是否需要版本号）
 *
 * 提示:
 * - 栈的结构：top -> Node1 -> Node2 -> ... -> null
 * - push: 新节点指向旧top，CAS更新top
 * - pop: CAS将top更新为top.next
 * - 注意空栈的边界情况
 *
 * 扩展思考:
 * - 这个实现是否有ABA问题？
 * - 如何使用AtomicStampedReference改进？
 * - 与synchronized实现的性能对比如何？
 */
public class E02_LockFreeStack<E> {

    /**
     * TODO: 栈顶指针，使用AtomicReference保证原子性
     */
    private AtomicReference<Node<E>> top = new AtomicReference<>();

    /**
     * 栈节点（已提供）
     */
    private static class Node<E> {
        final E item;
        Node<E> next;

        Node(E item) {
            this.item = item;
        }
    }

    /**
     * TODO: 实现压栈操作
     *
     * 算法思路:
     * 1. 创建新节点
     * 2. 读取当前栈顶
     * 3. 新节点的next指向当前栈顶
     * 4. CAS更新栈顶为新节点
     * 5. 如果CAS失败，重试（自旋）
     *
     * @param item 待压入的元素
     */
    public void push(E item) {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现push方法");
    }

    /**
     * TODO: 实现出栈操作
     *
     * 算法思路:
     * 1. 读取当前栈顶
     * 2. 如果栈为空，返回null
     * 3. 读取栈顶的next节点
     * 4. CAS将栈顶更新为next
     * 5. 如果CAS失败，重试
     * 6. 返回原栈顶的元素
     *
     * @return 栈顶元素，栈为空时返回null
     */
    public E pop() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现pop方法");
    }

    /**
     * TODO: 查看栈顶元素但不移除
     *
     * @return 栈顶元素，栈为空时返回null
     */
    public E peek() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现peek方法");
    }

    /**
     * TODO: 判断栈是否为空
     */
    public boolean isEmpty() {
        // TODO: 实现
        throw new UnsupportedOperationException("请实现isEmpty方法");
    }

    /**
     * 获取栈的大小（可选，性能较差）
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

    /**
     * 测试代码
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 测试无锁栈 ===\n");

        testBasicOperations();
        testConcurrentPush();
        testConcurrentPushPop();
    }

    /**
     * 测试基本操作
     */
    private static void testBasicOperations() {
        System.out.println("--- 测试基本操作 ---");

        E02_LockFreeStack<Integer> stack = new E02_LockFreeStack<>();

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

        System.out.println();
    }

    /**
     * 测试并发push
     */
    private static void testConcurrentPush() throws InterruptedException {
        System.out.println("--- 测试并发push ---");

        E02_LockFreeStack<Integer> stack = new E02_LockFreeStack<>();

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

        E02_LockFreeStack<Integer> stack = new E02_LockFreeStack<>();

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
        System.out.println("栈是否仍然有效: " + (!stack.isEmpty() ? "是 ✓" : "否"));
        System.out.println();
    }

    /**
     * TODO: 进阶任务 - 使用AtomicStampedReference避免ABA问题
     */
    @SuppressWarnings("unused")
    static class ABASafeLockFreeStack<E> {
        // TODO: 使用AtomicStampedReference实现
    }
}
