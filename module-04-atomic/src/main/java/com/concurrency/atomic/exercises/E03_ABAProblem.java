package com.concurrency.atomic.exercises;

import java.util.concurrent.atomic.*;

/**
 * 练习03: ABA问题演示与解决
 *
 * 难度: 🟡 中等
 *
 * 任务描述:
 * 1. 演示经典的ABA问题场景
 * 2. 使用AtomicStampedReference解决ABA问题
 * 3. 对比有ABA问题和无ABA问题的实现
 *
 * 学习目标:
 * - 理解ABA问题的本质和危害
 * - 掌握AtomicStampedReference的使用
 * - 了解版本号机制如何解决ABA问题
 *
 * 背景知识:
 * ABA问题是CAS操作的经典问题：
 * - 线程1读取值A
 * - 线程2将A改为B，再改回A
 * - 线程1执行CAS(A, C)成功，但中间状态已变化
 *
 * 典型场景:
 * - 栈操作中的节点复用
 * - 内存管理中的内存块复用
 * - 链表节点的复用
 */
public class E03_ABAProblem {

    /**
     * TODO: 任务1 - 演示有ABA问题的账户转账
     *
     * 场景:
     * - 初始余额100元
     * - 线程1：准备取款50元（读取余额100）
     * - 线程2：取出100元，又存入100元（制造ABA）
     * - 线程1：CAS成功，余额变为50元
     *
     * 问题:
     * - 线程1的CAS成功了，但没有感知到中间的变化
     * - 可能导致业务逻辑错误
     */
    static class ProblematicAccount {
        private AtomicInteger balance;

        public ProblematicAccount(int initialBalance) {
            this.balance = new AtomicInteger(initialBalance);
        }

        /**
         * TODO: 实现取款方法
         * 要求：使用CAS操作
         */
        public boolean withdraw(int amount) {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现withdraw方法");
        }

        /**
         * TODO: 实现存款方法
         */
        public void deposit(int amount) {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现deposit方法");
        }

        public int getBalance() {
            return balance.get();
        }
    }

    /**
     * TODO: 任务2 - 使用AtomicStampedReference解决ABA问题
     */
    static class SafeAccount {
        private AtomicStampedReference<Integer> balanceRef;

        public SafeAccount(int initialBalance) {
            // TODO: 初始化AtomicStampedReference，初始版本号为0
            throw new UnsupportedOperationException("请实现构造器");
        }

        /**
         * TODO: 实现安全的取款方法
         * 要求：使用版本号机制
         */
        public boolean withdraw(int amount) {
            // TODO: 实现
            // 提示：
            // 1. 获取当前余额和版本号
            // 2. 检查余额是否足够
            // 3. CAS更新余额和版本号
            throw new UnsupportedOperationException("请实现withdraw方法");
        }

        /**
         * TODO: 实现存款方法
         */
        public void deposit(int amount) {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现deposit方法");
        }

        public int getBalance() {
            return balanceRef.getReference();
        }

        public int getVersion() {
            return balanceRef.getStamp();
        }
    }

    /**
     * TODO: 任务3 - 实现有ABA问题的栈（演示节点复用问题）
     *
     * 场景:
     * 初始栈: A -> B -> C
     *
     * 线程1准备pop A:
     * 1. 读取 top = A, next = B
     *
     * 线程2执行:
     * 1. pop A (top = B)
     * 2. pop B (top = C)
     * 3. push D (top = D -> C)
     * 4. push A (top = A -> D -> C)  // A被复用！
     *
     * 线程1继续:
     * 1. CAS(top, A, B) 成功！
     * 2. top = B（野指针！B已经不在栈中）
     */
    static class ABAProneStack<E> {
        private AtomicReference<Node<E>> top = new AtomicReference<>();

        private static class Node<E> {
            final E item;
            Node<E> next;

            Node(E item) {
                this.item = item;
            }
        }

        /**
         * TODO: 实现push方法
         */
        public void push(E item) {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现push方法");
        }

        /**
         * TODO: 实现pop方法（有ABA问题）
         */
        public E pop() {
            // TODO: 实现
            throw new UnsupportedOperationException("请实现pop方法");
        }

        public boolean isEmpty() {
            return top.get() == null;
        }
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ABA问题演示与解决 ===\n");

        testProblematicAccount();
        // testSafeAccount();
        // testABAInStack();
    }

    /**
     * 测试有ABA问题的账户
     */
    private static void testProblematicAccount() throws InterruptedException {
        System.out.println("--- 测试ProblematicAccount（有ABA问题） ---");

        ProblematicAccount account = new ProblematicAccount(100);
        System.out.println("初始余额: " + account.getBalance());

        // 线程1：准备取款50元
        Thread t1 = new Thread(() -> {
            int currentBalance = account.getBalance();
            System.out.println("线程1读取余额: " + currentBalance);

            try {
                Thread.sleep(100); // 模拟业务处理延迟
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean success = account.withdraw(50);
            System.out.println("线程1取款50: " + success
                    + ", 当前余额: " + account.getBalance());
        }, "线程1");

        // 线程2：制造ABA（取出100，又存入100）
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50); // 让线程1先读取
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("线程2取款100...");
            account.withdraw(100);
            System.out.println("线程2存款100...");
            account.deposit(100);
            System.out.println("线程2完成ABA操作，余额恢复为: " + account.getBalance());
        }, "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终余额: " + account.getBalance());
        System.out.println("问题: 线程1没有感知到中间的变化！\n");
    }

    /**
     * TODO: 测试安全的账户（使用版本号）
     */
    @SuppressWarnings("unused")
    private static void testSafeAccount() throws InterruptedException {
        System.out.println("--- 测试SafeAccount（无ABA问题） ---");

        SafeAccount account = new SafeAccount(100);
        // TODO: 实现类似的测试逻辑
        // 对比结果：线程1的CAS会失败，因为版本号已改变

        System.out.println();
    }

    /**
     * TODO: 测试栈的ABA问题
     */
    @SuppressWarnings("unused")
    private static void testABAInStack() throws InterruptedException {
        System.out.println("--- 测试栈的ABA问题 ---");

        // TODO: 演示栈操作中的ABA问题
        // 提示：
        // 1. 创建栈并压入A, B, C
        // 2. 线程1准备pop A
        // 3. 线程2执行pop A, pop B, push D, push A
        // 4. 线程1的CAS会成功，但导致野指针

        System.out.println();
    }

    /**
     * TODO: 进阶任务 - 实现使用AtomicStampedReference的安全栈
     */
    @SuppressWarnings("unused")
    static class ABASafeStack<E> {
        // TODO: 使用AtomicStampedReference实现无ABA问题的栈
    }

    /**
     * 思考题:
     * 1. ABA问题在什么场景下会造成严重后果？
     * 2. 是否所有CAS操作都需要解决ABA问题？
     * 3. AtomicStampedReference的版本号会溢出吗？如果会，怎么办？
     * 4. AtomicMarkableReference和AtomicStampedReference的区别？
     * 5. 除了版本号，还有其他解决ABA的方法吗？
     */
}
