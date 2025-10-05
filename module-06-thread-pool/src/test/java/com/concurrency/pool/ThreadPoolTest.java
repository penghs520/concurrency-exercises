package com.concurrency.pool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 线程池测试类
 *
 * 测试内容:
 * 1. 线程池基本功能
 * 2. 任务执行流程
 * 3. 拒绝策略
 * 4. 动态参数调整
 * 5. 定时任务
 */
@DisplayName("线程池测试")
public class ThreadPoolTest {

    @Test
    @DisplayName("测试1: 线程池基本创建与使用")
    @Timeout(10)
    void testBasicThreadPool() throws Exception {
        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // 提交任务
        CountDownLatch latch = new CountDownLatch(4);
        for (int i = 0; i < 4; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(100);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 等待任务完成
        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在5秒内完成");

        // 验证
        assertTrue(executor.getCompletedTaskCount() >= 4, "应至少完成4个任务");

        // 关闭
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("测试2: 任务执行流程")
    @Timeout(10)
    void testTaskExecutionFlow() throws Exception {
        // 配置: 核心2，最大5，队列3
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 5, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3)
        );

        // 提交8个任务
        CountDownLatch latch = new CountDownLatch(8);
        for (int i = 0; i < 8; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(200);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(10); // 避免提交过快
        }

        // 验证线程数
        assertTrue(executor.getPoolSize() <= 5, "线程数不应超过最大值");

        // 等待任务完成
        assertTrue(latch.await(10, TimeUnit.SECONDS), "所有任务应完成");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试3: AbortPolicy拒绝策略")
    @Timeout(5)
    void testAbortPolicy() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // 第一个任务会执行
        executor.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 第二个任务应被拒绝（队列容量为0，线程池满）
        assertThrows(RejectedExecutionException.class, () -> {
            executor.execute(() -> {});
        });

        executor.shutdown();
    }

    @Test
    @DisplayName("测试4: CallerRunsPolicy拒绝策略")
    @Timeout(5)
    void testCallerRunsPolicy() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        AtomicInteger counter = new AtomicInteger(0);
        String mainThreadName = Thread.currentThread().getName();

        // 第一个任务占用线程池
        executor.execute(() -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(50);

        // 第二个任务应由主线程执行
        AtomicInteger callerRunCount = new AtomicInteger(0);
        executor.execute(() -> {
            if (Thread.currentThread().getName().equals(mainThreadName)) {
                callerRunCount.incrementAndGet();
            }
            counter.incrementAndGet();
        });

        Thread.sleep(200);

        assertEquals(2, counter.get(), "应执行2个任务");
        assertEquals(1, callerRunCount.get(), "应有1个任务由主线程执行");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试5: 动态调整核心线程数")
    @Timeout(5)
    void testDynamicCorePoolSize() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 10, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        // 初始核心线程数为2
        assertEquals(2, executor.getCorePoolSize());

        // 动态调整为5
        executor.setCorePoolSize(5);
        assertEquals(5, executor.getCorePoolSize());

        // 调整为3
        executor.setCorePoolSize(3);
        assertEquals(3, executor.getCorePoolSize());

        executor.shutdown();
    }

    @Test
    @DisplayName("测试6: 定时任务 - schedule")
    @Timeout(5)
    void testSchedule() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();

        // 延迟1秒执行
        scheduler.schedule(() -> {
            latch.countDown();
        }, 1, TimeUnit.SECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "任务应在2秒内完成");

        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 1000, "任务应至少延迟1秒执行");

        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试7: 定时任务 - scheduleAtFixedRate")
    @Timeout(6)
    void testScheduleAtFixedRate() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // 初始延迟0秒，每隔1秒执行一次
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            counter.incrementAndGet();
        }, 0, 1, TimeUnit.SECONDS);

        // 运行3秒
        Thread.sleep(3200);

        future.cancel(false);
        scheduler.shutdown();

        // 应执行至少3次（0s, 1s, 2s, 3s）
        assertTrue(counter.get() >= 3, "应至少执行3次，实际: " + counter.get());

        scheduler.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试8: 定时任务 - scheduleWithFixedDelay")
    @Timeout(6)
    void testScheduleWithFixedDelay() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        AtomicInteger counter = new AtomicInteger(0);

        // 初始延迟0秒，任务结束后延迟1秒再执行
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(500); // 任务执行500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, 1, TimeUnit.SECONDS);

        // 运行5秒
        Thread.sleep(5000);

        future.cancel(false);
        scheduler.shutdown();

        // 任务周期 = 500ms执行 + 1000ms延迟 = 1500ms
        // 5秒内应执行约3次
        assertTrue(counter.get() >= 2 && counter.get() <= 4,
                "应执行2-4次，实际: " + counter.get());

        scheduler.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试9: 线程池监控指标")
    @Timeout(5)
    void testPoolMetrics() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                3, 10, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50)
        );

        // 提交20个任务
        CountDownLatch latch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(100);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(50);

        // 验证监控指标
        assertTrue(executor.getPoolSize() > 0, "线程池应有线程");
        assertTrue(executor.getActiveCount() > 0, "应有活动线程");
        assertTrue(executor.getTaskCount() >= 20, "总任务数应>=20");

        latch.await(5, TimeUnit.SECONDS);

        assertTrue(executor.getCompletedTaskCount() >= 20, "已完成任务数应>=20");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试10: 优雅关闭")
    @Timeout(5)
    void testGracefulShutdown() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        // 提交任务
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(100);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 优雅关闭
        executor.shutdown();
        assertFalse(executor.isTerminated(), "应未立即终止");
        assertTrue(executor.isShutdown(), "应处于关闭状态");

        // 等待任务完成
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "应在5秒内终止");
        assertTrue(executor.isTerminated(), "应已终止");
        assertEquals(0, latch.getCount(), "所有任务应完成");
    }

    @Test
    @DisplayName("测试11: submit返回Future")
    @Timeout(5)
    void testSubmitWithFuture() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交Callable任务
        Future<Integer> future = executor.submit(() -> {
            Thread.sleep(100);
            return 42;
        });

        assertFalse(future.isDone(), "任务应未完成");

        Integer result = future.get(2, TimeUnit.SECONDS);
        assertEquals(42, result, "应返回正确结果");
        assertTrue(future.isDone(), "任务应已完成");

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试12: 任务异常处理")
    @Timeout(5)
    void testTaskException() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 提交会抛异常的任务
        Future<?> future = executor.submit(() -> {
            throw new RuntimeException("测试异常");
        });

        // 异常应在调用get()时抛出
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get();
        });

        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("测试异常", exception.getCause().getMessage());

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
}
