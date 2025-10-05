package com.concurrency.sync.exercises;

/**
 * 练习03: 哲学家就餐问题 🔴 ⭐
 *
 * 难度: 高级（经典面试题）
 * 预计时间: 40分钟
 *
 * 问题描述:
 * 5个哲学家围坐在圆桌旁，每人之间有一根筷子（共5根）。
 * 哲学家的行为：思考 -> 拿起左筷子 -> 拿起右筷子 -> 吃饭 -> 放下筷子 -> 继续思考
 *
 * 问题:
 * 如果每个哲学家都先拿起左边的筷子，再等待右边的筷子，会发生死锁！
 *
 * 任务:
 * 实现一个避免死锁的解决方案。可选方案包括：
 *
 * 方案1: 资源排序
 * - 奇数号哲学家先拿左筷子再拿右筷子
 * - 偶数号哲学家先拿右筷子再拿左筷子
 *
 * 方案2: 限制同时就餐人数
 * - 最多允许4个哲学家同时拿筷子
 * - 使用Semaphore（或自己实现计数器）
 *
 * 方案3: 服务员模式
 * - 引入服务员，只有获得服务员许可才能拿筷子
 * - 服务员保证不会产生死锁
 *
 * 要求:
 * - 避免死锁
 * - 避免饥饿（每个哲学家都能就餐）
 * - 使用synchronized实现
 *
 * 提示:
 * - 筷子可以用Object表示，通过synchronized获取
 * - 注意获取筷子的顺序
 */
public class E03_PhilosophersDinner {

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
            // TODO: 实现就餐逻辑，避免死锁
            // 提示: 考虑使用锁排序或其他策略

            throw new UnsupportedOperationException("请实现此方法");

            /*
             * 参考框架:
             *
             * // 1. 获取筷子（注意顺序！）
             * synchronized (???) {
             *     System.out.println("哲学家" + id + " 拿起左筷子" + leftChopstick.getId());
             *
             *     synchronized (???) {
             *         System.out.println("哲学家" + id + " 拿起右筷子" + rightChopstick.getId());
             *
             *         // 2. 吃饭
             *         System.out.println("哲学家" + id + " 正在吃饭...");
             *         Thread.sleep((long) (Math.random() * 100));
             *         eatCount++;
             *
             *         // 3. 放下筷子（自动释放）
             *         System.out.println("哲学家" + id + " 放下筷子");
             *     }
             * }
             */
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
        System.out.println("如果程序正常结束，说明成功避免了死锁！");
    }
}
