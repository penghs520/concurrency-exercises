package com.concurrency.locks.solutions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 练习01参考答案: 线程安全的银行转账
 *
 * 实现要点:
 * 1. 使用tryLock避免死锁
 * 2. 确保原子性：要么两个账户都锁定成功，要么都失败
 * 3. 正确的释放顺序：后获取的锁先释放
 * 4. 异常处理：确保锁一定被释放
 */
public class S01_BankTransfer {

    /**
     * 银行账户类
     */
    public static class BankAccount {
        private final String accountId;
        private int balance;
        private final Lock lock = new ReentrantLock();

        public BankAccount(String accountId, int initialBalance) {
            this.accountId = accountId;
            this.balance = initialBalance;
        }

        public String getAccountId() {
            return accountId;
        }

        public int getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }

        /**
         * 转账方法
         * 使用tryLock避免死锁
         *
         * @param to 转入账户
         * @param amount 转账金额
         * @return true表示转账成功，false表示失败
         */
        public boolean transfer(BankAccount to, int amount) {
            // 1. 验证参数
            if (amount <= 0) {
                throw new IllegalArgumentException("转账金额必须为正数");
            }

            if (to == null) {
                throw new IllegalArgumentException("转入账户不能为null");
            }

            if (this == to) {
                throw new IllegalArgumentException("不能向自己转账");
            }

            // 2. 尝试获取两个账户的锁
            // 关键点：使用tryLock避免死锁
            boolean fromLocked = false;
            boolean toLocked = false;

            try {
                // 尝试获取转出账户的锁（最多等待1秒）
                fromLocked = lock.tryLock(1, TimeUnit.SECONDS);
                if (!fromLocked) {
                    return false; // 无法获取锁，转账失败
                }

                // 尝试获取转入账户的锁
                toLocked = to.lock.tryLock(1, TimeUnit.SECONDS);
                if (!toLocked) {
                    return false; // 无法获取锁，转账失败
                }

                // 3. 检查余额
                if (balance < amount) {
                    // System.out.println("余额不足: " + accountId);
                    return false;
                }

                // 4. 执行转账（临界区）
                balance -= amount;
                to.balance += amount;

                return true;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } finally {
                // 5. 释放锁（后获取的锁先释放）
                if (toLocked) {
                    to.lock.unlock();
                }
                if (fromLocked) {
                    lock.unlock();
                }
            }
        }

        /**
         * 存款
         */
        public void deposit(int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("存款金额必须为正数");
            }

            lock.lock();
            try {
                balance += amount;
            } finally {
                lock.unlock();
            }
        }

        /**
         * 取款
         */
        public boolean withdraw(int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("取款金额必须为正数");
            }

            lock.lock();
            try {
                if (balance >= amount) {
                    balance -= amount;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public String toString() {
            return "Account{" + accountId + ", balance=" + getBalance() + "}";
        }
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testBankTransfer();
    }

    private static void testBankTransfer() throws InterruptedException {
        System.out.println("=== 银行转账测试 ===\n");

        // 创建账户
        BankAccount account1 = new BankAccount("A001", 1000);
        BankAccount account2 = new BankAccount("A002", 1000);
        BankAccount account3 = new BankAccount("A003", 1000);

        System.out.println("初始状态:");
        System.out.println("  " + account1);
        System.out.println("  " + account2);
        System.out.println("  " + account3);
        System.out.println();

        // 创建多个转账线程
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                boolean success = account1.transfer(account2, 100);
                System.out.println("A001 → A002: " + (success ? "成功" : "失败"));
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "转账1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                boolean success = account2.transfer(account3, 100);
                System.out.println("A002 → A003: " + (success ? "成功" : "失败"));
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "转账2");

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                boolean success = account3.transfer(account1, 100);
                System.out.println("A003 → A001: " + (success ? "成功" : "失败"));
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "转账3");

        // 启动线程
        t1.start();
        t2.start();
        t3.start();

        // 等待完成
        t1.join();
        t2.join();
        t3.join();

        System.out.println("\n最终状态:");
        System.out.println("  " + account1);
        System.out.println("  " + account2);
        System.out.println("  " + account3);

        int totalBalance = account1.getBalance() + account2.getBalance() + account3.getBalance();
        System.out.println("\n总余额: " + totalBalance + " (预期: 3000)");

        if (totalBalance == 3000) {
            System.out.println("✓ 测试通过！");
        } else {
            System.out.println("✗ 测试失败！余额不一致");
        }
    }
}
