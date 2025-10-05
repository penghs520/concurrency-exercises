package com.concurrency.atomic;

import com.concurrency.atomic.solutions.S01_ClickCounter;
import com.concurrency.atomic.solutions.S02_LockFreeStack;
import com.concurrency.atomic.solutions.S03_ABAProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module 04: 原子类单元测试
 *
 * 测试内容:
 * 1. 原子类基本操作
 * 2. CAS机制
 * 3. 点击计数器
 * 4. 无锁栈
 * 5. ABA问题
 */
@DisplayName("原子类测试")
public class AtomicTest {

    @Test
    @DisplayName("测试AtomicInteger基本操作")
    public void testAtomicIntegerBasics() {
        AtomicInteger ai = new AtomicInteger(0);

        // 测试自增
        assertEquals(1, ai.incrementAndGet());
        assertEquals(1, ai.getAndIncrement());
        assertEquals(2, ai.get());

        // 测试加法
        assertEquals(7, ai.addAndGet(5));
        assertEquals(7, ai.getAndAdd(3));
        assertEquals(10, ai.get());

        // 测试CAS
        assertTrue(ai.compareAndSet(10, 20));
        assertFalse(ai.compareAndSet(10, 30));
        assertEquals(20, ai.get());
    }

    @Test
    @DisplayName("测试AtomicBoolean单次初始化")
    public void testAtomicBooleanSingleInit() throws InterruptedException {
        AtomicBoolean initialized = new AtomicBoolean(false);
        AtomicInteger initCount = new AtomicInteger(0);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                if (initialized.compareAndSet(false, true)) {
                    initCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // 只有一个线程成功初始化
        assertEquals(1, initCount.get());
        assertTrue(initialized.get());
    }

    @Test
    @DisplayName("测试AtomicReference")
    public void testAtomicReference() {
        AtomicReference<String> ref = new AtomicReference<>("initial");

        assertEquals("initial", ref.get());

        // CAS更新
        assertTrue(ref.compareAndSet("initial", "updated"));
        assertEquals("updated", ref.get());

        assertFalse(ref.compareAndSet("initial", "failed"));

        // getAndSet
        assertEquals("updated", ref.getAndSet("final"));
        assertEquals("final", ref.get());
    }

    @Test
    @DisplayName("测试LongAdder并发累加")
    public void testLongAdderConcurrent() throws InterruptedException {
        LongAdder adder = new LongAdder();

        int threadCount = 10;
        int incrementsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    adder.increment();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        long expected = (long) threadCount * incrementsPerThread;
        assertEquals(expected, adder.sum());
    }

    @Test
    @DisplayName("测试AtomicStampedReference版本号")
    public void testAtomicStampedReference() {
        AtomicStampedReference<Integer> asr = new AtomicStampedReference<>(100, 0);

        int[] stampHolder = new int[1];
        Integer value = asr.get(stampHolder);

        assertEquals(100, value);
        assertEquals(0, stampHolder[0]);

        // CAS成功
        assertTrue(asr.compareAndSet(100, 200, 0, 1));
        assertEquals(200, (int) asr.getReference());
        assertEquals(1, asr.getStamp());

        // CAS失败（版本号不匹配）
        assertFalse(asr.compareAndSet(200, 300, 0, 2));
        assertEquals(200, (int) asr.getReference());
        assertEquals(1, asr.getStamp());
    }

    @Test
    @DisplayName("测试点击计数器 - BasicClickCounter")
    public void testBasicClickCounter() throws InterruptedException {
        S01_ClickCounter.BasicClickCounter counter = new S01_ClickCounter.BasicClickCounter();

        int threadCount = 10;
        int clicksPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < clicksPerThread; j++) {
                    counter.click();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        long expected = (long) threadCount * clicksPerThread;
        assertEquals(expected, counter.getTotalClicks());

        // 测试reset
        counter.reset();
        assertEquals(0, counter.getTotalClicks());
    }

    @Test
    @DisplayName("测试点击计数器 - HighPerformanceClickCounter")
    public void testHighPerformanceClickCounter() throws InterruptedException {
        S01_ClickCounter.HighPerformanceClickCounter counter =
                new S01_ClickCounter.HighPerformanceClickCounter();

        int threadCount = 10;
        int clicksPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < clicksPerThread; j++) {
                    counter.click();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        long expected = (long) threadCount * clicksPerThread;
        assertEquals(expected, counter.getTotalClicks());
    }

    @Test
    @DisplayName("测试点击计数器 - CategorizedClickCounter")
    public void testCategorizedClickCounter() throws InterruptedException {
        S01_ClickCounter.CategorizedClickCounter counter =
                new S01_ClickCounter.CategorizedClickCounter();

        String[] categories = {"首页", "搜索", "推荐"};
        int threadCount = 9; // 3的倍数
        int clicksPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                String category = categories[threadId % categories.length];
                for (int j = 0; j < clicksPerThread; j++) {
                    counter.click(category);
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // 每个类别应该有相同的点击数
        long expectedPerCategory = (long) (threadCount / categories.length) * clicksPerThread;
        for (String category : categories) {
            assertEquals(expectedPerCategory, counter.getClicks(category));
        }

        long totalExpected = (long) threadCount * clicksPerThread;
        assertEquals(totalExpected, counter.getTotalClicks());
    }

    @Test
    @DisplayName("测试无锁栈 - 基本操作")
    public void testLockFreeStackBasics() {
        S02_LockFreeStack.BasicLockFreeStack<Integer> stack =
                new S02_LockFreeStack.BasicLockFreeStack<>();

        assertTrue(stack.isEmpty());
        assertNull(stack.pop());

        stack.push(1);
        stack.push(2);
        stack.push(3);

        assertFalse(stack.isEmpty());
        assertEquals(3, stack.size());

        assertEquals(3, stack.peek());
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertNull(stack.pop());

        assertTrue(stack.isEmpty());
    }

    @Test
    @DisplayName("测试无锁栈 - 并发push")
    public void testLockFreeStackConcurrentPush() throws InterruptedException {
        S02_LockFreeStack.BasicLockFreeStack<Integer> stack =
                new S02_LockFreeStack.BasicLockFreeStack<>();

        int threadCount = 10;
        int itemsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < itemsPerThread; j++) {
                    stack.push(threadId * 10000 + j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        int expected = threadCount * itemsPerThread;
        assertEquals(expected, stack.size());
    }

    @Test
    @DisplayName("测试无锁栈 - 并发push/pop")
    public void testLockFreeStackConcurrentPushPop() throws InterruptedException {
        S02_LockFreeStack.BasicLockFreeStack<Integer> stack =
                new S02_LockFreeStack.BasicLockFreeStack<>();

        // 预填充
        for (int i = 0; i < 1000; i++) {
            stack.push(i);
        }

        int threadCount = 20;
        int operations = 500;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < operations; j++) {
                    if (threadId % 2 == 0) {
                        stack.push(threadId * 10000 + j);
                    } else {
                        stack.pop();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // 栈应该仍然有效
        assertFalse(stack.isEmpty());
        assertNotNull(stack.pop());
    }

    @Test
    @DisplayName("测试ABA安全的栈")
    public void testABASafeStack() throws InterruptedException {
        S02_LockFreeStack.ABASafeLockFreeStack<Integer> stack =
                new S02_LockFreeStack.ABASafeLockFreeStack<>();

        int initialVersion = stack.getVersion();

        stack.push(1);
        assertTrue(stack.getVersion() > initialVersion);

        stack.push(2);
        stack.pop();

        // 版本号应该持续增加
        int currentVersion = stack.getVersion();
        assertTrue(currentVersion > initialVersion);
    }

    @Test
    @DisplayName("测试ProblematicAccount")
    public void testProblematicAccount() {
        S03_ABAProblem.ProblematicAccount account =
                new S03_ABAProblem.ProblematicAccount(100);

        assertEquals(100, account.getBalance());

        assertTrue(account.withdraw(50));
        assertEquals(50, account.getBalance());

        assertFalse(account.withdraw(100)); // 余额不足
        assertEquals(50, account.getBalance());

        account.deposit(100);
        assertEquals(150, account.getBalance());
    }

    @Test
    @DisplayName("测试SafeAccount版本号机制")
    public void testSafeAccount() {
        S03_ABAProblem.SafeAccount account =
                new S03_ABAProblem.SafeAccount(100);

        int initialVersion = account.getVersion();
        assertEquals(100, account.getBalance());

        account.withdraw(30);
        assertTrue(account.getVersion() > initialVersion);
        assertEquals(70, account.getBalance());

        account.deposit(50);
        assertEquals(120, account.getBalance());

        // 版本号应该递增
        assertTrue(account.getVersion() > initialVersion + 1);
    }

    @Test
    @DisplayName("测试SafeAccount并发操作")
    public void testSafeAccountConcurrent() throws InterruptedException {
        S03_ABAProblem.SafeAccount account =
                new S03_ABAProblem.SafeAccount(10000);

        int threadCount = 10;
        int operations = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < operations; j++) {
                    if (threadId % 2 == 0) {
                        account.deposit(10);
                    } else {
                        account.withdraw(10);
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // 余额应该保持一致
        assertEquals(10000, account.getBalance());

        // 版本号应该等于总操作数
        int expectedVersion = threadCount * operations;
        assertEquals(expectedVersion, account.getVersion());
    }

    @Test
    @DisplayName("测试ABA栈基本操作")
    public void testABAProneStack() {
        S03_ABAProblem.ABAProneStack<String> stack =
                new S03_ABAProblem.ABAProneStack<>();

        stack.push("A");
        stack.push("B");
        stack.push("C");

        assertEquals("C", stack.pop());
        assertEquals("B", stack.pop());
        assertEquals("A", stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    @DisplayName("性能测试 - AtomicLong vs LongAdder")
    public void performanceTestAtomicVsAdder() throws InterruptedException {
        int threadCount = 20;
        int iterations = 100_000;

        // 测试AtomicLong
        AtomicLong atomicLong = new AtomicLong();
        long time1 = measurePerformance("AtomicLong", () -> {
            try {
                runConcurrentIncrements(atomicLong, threadCount, iterations);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 测试LongAdder
        LongAdder longAdder = new LongAdder();
        long time2 = measurePerformance("LongAdder", () -> {
            try {
                runConcurrentIncrements(longAdder, threadCount, iterations);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("\n性能对比:");
        System.out.println("AtomicLong: " + time1 + "ms");
        System.out.println("LongAdder: " + time2 + "ms");
        System.out.println("提升: " + String.format("%.2fx", (double) time1 / time2));

        // 验证结果
        long expected = (long) threadCount * iterations;
        assertEquals(expected, atomicLong.get());
        assertEquals(expected, longAdder.sum());
    }

    private void runConcurrentIncrements(Object counter, int threadCount, int iterations)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    if (counter instanceof AtomicLong) {
                        ((AtomicLong) counter).incrementAndGet();
                    } else {
                        ((LongAdder) counter).increment();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
    }

    private long measurePerformance(String name, Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(name + " 耗时: " + elapsed + "ms");
        return elapsed;
    }
}
