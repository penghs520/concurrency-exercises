package com.concurrency.locks.exercises;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 练习01: 线程安全的银行转账 🟢
 *
 * 难度: 基础
 * 预计时间: 30分钟
 *
 * 任务描述:
 * 实现一个线程安全的银行转账系统，支持多线程并发转账操作。
 *
 * 要求:
 * 1. 使用ReentrantLock保护账户余额
 * 2. 使用tryLock避免死锁（账户之间相互转账）
 * 3. 转账操作要保证原子性（要么成功，要么失败）
 * 4. 余额不足时转账失败
 * 5. 正确处理异常情况
 *
 * 提示:
 * - 使用tryLock()而不是lock()，避免死锁
 * - 注意锁的获取顺序
 * - 确保finally中释放锁
 * - 先锁定转出账户，再锁定转入账户
 */
public class E01_BankTransfer {

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
         * TODO: 实现线程安全的转账逻辑
         *
         * @param to 转入账户
         * @param amount 转账金额
         * @return true表示转账成功，false表示失败
         */
        public boolean transfer(BankAccount to, int amount) {
            // TODO: 实现转账逻辑
            // 1. 验证参数（金额必须为正数）
            // 2. 使用tryLock获取两个账户的锁
            // 3. 检查余额是否足够
            // 4. 执行转账操作
            // 5. 释放锁

            throw new UnsupportedOperationException("请实现此方法");
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
