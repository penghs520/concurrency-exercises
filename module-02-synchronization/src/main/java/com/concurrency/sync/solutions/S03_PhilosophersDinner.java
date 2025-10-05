package com.concurrency.sync.solutions;

/**
 * 练习03参考答案: 哲学家就餐问题
 *
 * 解决方案: 资源排序（Lock Ordering）
 *
 * 核心思想:
 * - 为每根筷子分配一个编号
 * - 所有哲学家都按照筷子编号的顺序获取筷子
 * - 先获取编号小的筷子，再获取编号大的筷子
 *
 * 为什么能避免死锁？
 * - 破坏了"循环等待"条件
 * - 不会出现"A等B，B等C，C等A"的循环
 *
 * 其他解决方案:
 * 1. 限制同时就餐人数（最多4人）
 * 2. 奇偶策略（奇数号先左后右，偶数号先右后左）
 * 3. 服务员模式（引入中心协调者）
 */
public class S03_PhilosophersDinner {

    static class Chopstick {
        private final int id;

        public Chopstick(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    static class Philosopher implements Runnable {
        private final int id;
        private final Chopstick leftChopstick;
        private final Chopstick rightChopstick;
        private int eatCount = 0;

        public Philosopher(int id, Chopstick leftChopstick, Chopstick rightChopstick) {
            this.id = id;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 3; i++) {  // 每个哲学家吃3次
                    think();
                    eat();
                }
                System.out.println("哲学家" + id + " 完成就餐，共吃了" + eatCount + "次");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void think() throws InterruptedException {
            System.out.println("哲学家" + id + " 正在思考...");
            Thread.sleep((long) (Math.random() * 100));
        }

        private void eat() throws InterruptedException {
            // 关键：按照筷子编号的顺序获取锁
            Chopstick first = leftChopstick.getId() < rightChopstick.getId() ?
                    leftChopstick : rightChopstick;
            Chopstick second = first == leftChopstick ? rightChopstick : leftChopstick;

            // 先获取编号小的筷子
            synchronized (first) {
                System.out.println("哲学家" + id + " 拿起筷子" + first.getId());

                // 再获取编号大的筷子
                synchronized (second) {
                    System.out.println("哲学家" + id + " 拿起筷子" + second.getId());

                    // 吃饭
                    System.out.println("哲学家" + id + " 正在吃饭...");
                    Thread.sleep((long) (Math.random() * 100));
                    eatCount++;

                    System.out.println("哲学家" + id + " 放下筷子");
                }
            }
        }
    }

    // ==================== 测试代码 ====================

    public static void main(String[] args) throws InterruptedException {
        testPhilosophersDinner();
    }

    private static void testPhilosophersDinner() throws InterruptedException {
        final int NUM_PHILOSOPHERS = 5;
        Chopstick[] chopsticks = new Chopstick[NUM_PHILOSOPHERS];
        Philosopher[] philosophers = new Philosopher[NUM_PHILOSOPHERS];
        Thread[] threads = new Thread[NUM_PHILOSOPHERS];

        // 创建筷子
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            chopsticks[i] = new Chopstick(i);
        }

        // 创建哲学家和线程
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            Chopstick leftChopstick = chopsticks[i];
            Chopstick rightChopstick = chopsticks[(i + 1) % NUM_PHILOSOPHERS];
            philosophers[i] = new Philosopher(i, leftChopstick, rightChopstick);
            threads[i] = new Thread(philosophers[i], "哲学家" + i);
        }

        System.out.println("===== 哲学家就餐问题开始 =====");

        // 启动所有哲学家线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("===== 哲学家就餐问题结束 =====");
        System.out.println("程序正常结束，成功避免了死锁！");
    }

    /*
     * ==================== 其他解决方案 ====================
     *
     * 方案2: 奇偶策略
     * ----------------
     * private void eat() throws InterruptedException {
     *     if (id % 2 == 0) {
     *         // 偶数号哲学家：先拿左筷子，再拿右筷子
     *         synchronized (leftChopstick) {
     *             synchronized (rightChopstick) {
     *                 // 吃饭
     *             }
     *         }
     *     } else {
     *         // 奇数号哲学家：先拿右筷子，再拿左筷子
     *         synchronized (rightChopstick) {
     *             synchronized (leftChopstick) {
     *                 // 吃饭
     *             }
     *         }
     *     }
     * }
     *
     * 方案3: 限制同时就餐人数
     * ----------------------
     * // 使用Semaphore限制最多4个哲学家同时拿筷子
     * static final Semaphore semaphore = new Semaphore(4);
     *
     * private void eat() throws InterruptedException {
     *     semaphore.acquire();  // 获取许可
     *     try {
     *         synchronized (leftChopstick) {
     *             synchronized (rightChopstick) {
     *                 // 吃饭
     *             }
     *         }
     *     } finally {
     *         semaphore.release();  // 释放许可
     *     }
     * }
     */
}
