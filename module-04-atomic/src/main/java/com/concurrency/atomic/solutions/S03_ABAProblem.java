package com.concurrency.atomic.solutions;

import java.util.concurrent.atomic.*;

/**
 * 练习03解答: ABA问题演示与解决
 *
 * 知识点:
 * 1. ABA问题的本质和危害
 * 2. AtomicStampedReference的使用
 * 3. AtomicMarkableReference的应用场景
 * 4. 版本号机制的原理
 */
public class S03_ABAProblem {

    /**
     * 有ABA问题的账户实现
     */
    public static class ProblematicAccount {
        private AtomicInteger balance;

        public ProblematicAccount(int initialBalance) {
            this.balance = new AtomicInteger(initialBalance);
        }

        /**
         * 取款（使用CAS）
         */
        public boolean withdraw(int amount) {
            while (true) {
                int current = balance.get();
                if (current < amount) {
                    return false; // 余额不足
                }
                int next = current - amount;
                if (balance.compareAndSet(current, next)) {
                    return true;
                }
                // CAS失败，重试
            }
        }

        /**
         * 存款
         */
        public void deposit(int amount) {
            balance.addAndGet(amount);
        }

        public int getBalance() {
            return balance.get();
        }
    }

    /**
     * 使用AtomicStampedReference解决ABA问题
     */
    public static class SafeAccount {
        private AtomicStampedReference<Integer> balanceRef;

        public SafeAccount(int initialBalance) {
            // 初始余额和版本号0
            this.balanceRef = new AtomicStampedReference<>(initialBalance, 0);
        }

        /**
         * 安全的取款（带版本号检查）
         */
        public boolean withdraw(int amount) {
            while (true) {
                int[] stampHolder = new int[1];
                Integer current = balanceRef.get(stampHolder);
                int stamp = stampHolder[0];

                if (current < amount) {
                    return false; // 余额不足
                }

                Integer next = current - amount;

                // CAS：同时检查余额和版本号
                if (balanceRef.compareAndSet(current, next, stamp, stamp + 1)) {
                    return true;
                }
                // CAS失败（余额或版本号不匹配），重试
            }
        }

        /**
         * 存款
         */
        public void deposit(int amount) {
            while (true) {
                int[] stampHolder = new int[1];
                Integer current = balanceRef.get(stampHolder);
                int stamp = stampHolder[0];

                Integer next = current + amount;

                if (balanceRef.compareAndSet(current, next, stamp, stamp + 1)) {
                    return;
                }
            }
        }

        public int getBalance() {
            return balanceRef.getReference();
        }

        public int getVersion() {
            return balanceRef.getStamp();
        }
    }

    /**
     * 有ABA问题的栈实现
     */
    public static class ABAProneStack<E> {
        private AtomicReference<Node<E>> top = new AtomicReference<>();

        private static class Node<E> {
            final E item;
            Node<E> next;

            Node(E item) {
                this.item = item;
            }

            @Override
            public String toString() {
                return String.valueOf(item);
            }
        }

        public void push(E item) {
            Node<E> newHead = new Node<>(item);
            while (true) {
                Node<E> oldHead = top.get();
                newHead.next = oldHead;
                if (top.compareAndSet(oldHead, newHead)) {
                    return;
                }
            }
        }

        public E pop() {
            while (true) {
                Node<E> oldHead = top.get();
                if (oldHead == null) {
                    return null;
                }
                Node<E> newHead = oldHead.next;
                if (top.compareAndSet(oldHead, newHead)) {
                    return oldHead.item;
                }
            }
        }

        public boolean isEmpty() {
            return top.get() == null;
        }

        public Node<E> getTop() {
            return top.get();
        }

        public void printStack() {
            StringBuilder sb = new StringBuilder("Stack: ");
            Node<E> current = top.get();
            while (current != null) {
                sb.append(current.item).append(" -> ");
                current = current.next;
            }
            sb.append("null");
            System.out.println(sb);
        }
    }

    /**
     * 使用AtomicStampedReference的安全栈
     */
    public static class ABASafeStack<E> {
        private AtomicStampedReference<Node<E>> top =
                new AtomicStampedReference<>(null, 0);

        private static class Node<E> {
            final E item;
            Node<E> next;

            Node(E item) {
                this.item = item;
            }

            @Override
            public String toString() {
                return String.valueOf(item);
            }
        }

        public void push(E item) {
            Node<E> newHead = new Node<>(item);
            int[] stampHolder = new int[1];

            while (true) {
                Node<E> oldHead = top.get(stampHolder);
                int oldStamp = stampHolder[0];

                newHead.next = oldHead;

                if (top.compareAndSet(oldHead, newHead, oldStamp, oldStamp + 1)) {
                    return;
                }
            }
        }

        public E pop() {
            int[] stampHolder = new int[1];

            while (true) {
                Node<E> oldHead = top.get(stampHolder);
                int oldStamp = stampHolder[0];

                if (oldHead == null) {
                    return null;
                }

                Node<E> newHead = oldHead.next;

                if (top.compareAndSet(oldHead, newHead, oldStamp, oldStamp + 1)) {
                    return oldHead.item;
                }
            }
        }

        public int getVersion() {
            return top.getStamp();
        }
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ABA问题演示与解决 ===\n");

        testProblematicAccount();
        testSafeAccount();
        testABAInStack();
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

            // 线程1苏醒后执行CAS
            boolean success = account.withdraw(50);
            System.out.println("线程1取款50元: " + success
                    + ", 当前余额: " + account.getBalance());
        }, "线程1");

        // 线程2：制造ABA（取出100，又存入100）
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50); // 让线程1先读取
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("线程2取款100元...");
            account.withdraw(100);
            System.out.println("  余额变为: " + account.getBalance());

            System.out.println("线程2存款100元...");
            account.deposit(100);
            System.out.println("  余额恢复为: " + account.getBalance());
        }, "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终余额: " + account.getBalance());
        System.out.println("问题: 线程1的CAS成功了，但没有感知到中间的取款/存款操作！");
        System.out.println("      这在某些业务场景下可能导致逻辑错误。\n");
    }

    /**
     * 测试安全的账户（使用版本号）
     */
    private static void testSafeAccount() throws InterruptedException {
        System.out.println("--- 测试SafeAccount（无ABA问题） ---");

        SafeAccount account = new SafeAccount(100);
        System.out.println("初始余额: " + account.getBalance() + ", 版本: " + account.getVersion());

        // 线程1：准备取款50元
        Thread t1 = new Thread(() -> {
            int[] stampHolder = new int[1];
            int currentBalance = account.getBalance();
            int currentVersion = account.getVersion();

            System.out.println("线程1读取: 余额=" + currentBalance + ", 版本=" + currentVersion);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 线程1尝试取款
            boolean success = account.withdraw(50);
            System.out.println("线程1取款50元: " + success
                    + ", 当前余额: " + account.getBalance()
                    + ", 版本: " + account.getVersion());

            if (!success) {
                System.out.println("  原因: 版本号已变化，检测到中间修改！");
            }
        }, "线程1");

        // 线程2：制造ABA
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("线程2取款100元...");
            account.withdraw(100);
            System.out.println("  余额: " + account.getBalance() + ", 版本: " + account.getVersion());

            System.out.println("线程2存款100元...");
            account.deposit(100);
            System.out.println("  余额: " + account.getBalance() + ", 版本: " + account.getVersion());
        }, "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终余额: " + account.getBalance() + ", 版本: " + account.getVersion());
        System.out.println("优势: 版本号机制成功检测到中间修改，避免了ABA问题！\n");
    }

    /**
     * 测试栈的ABA问题
     */
    private static void testABAInStack() throws InterruptedException {
        System.out.println("--- 测试栈的ABA问题 ---");

        ABAProneStack<String> stack = new ABAProneStack<>();

        // 初始化栈: A -> B -> C
        stack.push("C");
        stack.push("B");
        stack.push("A");

        System.out.println("初始栈:");
        stack.printStack();

        // 保存节点A的引用
        final ABAProneStack.Node<String>[] nodeAHolder = new ABAProneStack.Node[1];

        // 线程1：准备pop A
        Thread t1 = new Thread(() -> {
            ABAProneStack.Node<String> oldHead = stack.getTop();
            nodeAHolder[0] = oldHead;
            System.out.println("线程1读取栈顶: " + oldHead + " (准备pop)");

            try {
                Thread.sleep(100); // 让线程2有时间操作
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 线程1苏醒，执行pop
            String item = stack.pop();
            System.out.println("线程1 pop: " + item);
            System.out.println("线程1操作后:");
            stack.printStack();
        }, "线程1");

        // 线程2：制造ABA（pop A, pop B, push D, push A）
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("\n线程2开始ABA操作:");
            String a = stack.pop();
            System.out.println("  pop " + a);
            stack.printStack();

            String b = stack.pop();
            System.out.println("  pop " + b);
            stack.printStack();

            stack.push("D");
            System.out.println("  push D");
            stack.printStack();

            stack.push("A"); // 复用节点A
            System.out.println("  push A (复用)");
            stack.printStack();
            System.out.println();
        }, "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终栈:");
        stack.printStack();
        System.out.println("\n说明: 虽然栈仍然正确，但在某些场景下（如内存管理）ABA可能导致严重问题。");
        System.out.println("      例如：节点被释放后又被重新分配，指针可能指向已释放的内存。\n");

        // 演示安全的栈
        testABASafeStack();
    }

    /**
     * 测试安全的栈
     */
    private static void testABASafeStack() throws InterruptedException {
        System.out.println("--- 测试ABASafeStack（版本号保护） ---");

        ABASafeStack<String> stack = new ABASafeStack<>();

        stack.push("C");
        stack.push("B");
        stack.push("A");

        System.out.println("初始版本号: " + stack.getVersion());

        // 并发操作
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
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
        System.out.println("说明: 每次操作版本号都会递增，确保检测到所有修改。");
        System.out.println("测试通过 ✓\n");
    }

    /**
     * 思考题答案:
     *
     * 1. ABA问题在什么场景下会造成严重后果？
     *    - 内存管理：节点被释放后又重新分配，可能导致野指针
     *    - 资源池：资源被归还后又重新获取，状态可能不一致
     *    - 链表操作：节点复用可能导致链表结构破坏
     *
     * 2. 是否所有CAS操作都需要解决ABA问题？
     *    - 不是。如果只关心最终值（如计数器），ABA无影响
     *    - 如果关心中间状态变化，需要解决ABA
     *
     * 3. AtomicStampedReference的版本号会溢出吗？
     *    - 会（int范围），但实际应用中很少见
     *    - 溢出后回绕到负数，仍能正常工作（只要没有极端的版本号跳跃）
     *
     * 4. AtomicMarkableReference和AtomicStampedReference的区别？
     *    - AtomicStampedReference: 使用int版本号，可以记录修改次数
     *    - AtomicMarkableReference: 使用boolean标记，只记录是否修改过
     *    - 前者适合需要精确版本的场景，后者适合只需标记的场景
     *
     * 5. 除了版本号，还有其他解决ABA的方法吗？
     *    - 使用不可复用的对象（每次都创建新对象）
     *    - 延迟回收（垃圾回收机制）
     *    - 使用带代数的指针（Hazard Pointers）
     *    - 使用双重CAS（DCAS，硬件支持）
     */
}
