package com.concurrency.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发集合单元测试
 *
 * 测试覆盖:
 * 1. ConcurrentHashMap的并发操作
 * 2. BlockingQueue的阻塞特性
 * 3. CopyOnWriteArrayList的快照迭代
 * 4. 并发场景下的正确性验证
 */
@DisplayName("并发集合测试")
class ConcurrentCollectionsTest {

    @Test
    @DisplayName("ConcurrentHashMap - 并发put测试")
    @Timeout(10)
    void testConcurrentHashMapPut() throws Exception {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        final int THREAD_COUNT = 10;
        final int ITEMS_PER_THREAD = 100;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                        int key = threadId * ITEMS_PER_THREAD + j;
                        map.put(key, "Value-" + key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 验证所有元素都被正确插入
        assertEquals(THREAD_COUNT * ITEMS_PER_THREAD, map.size(),
                "所有元素应该被正确插入");
    }

    @Test
    @DisplayName("ConcurrentHashMap - 原子操作测试")
    void testConcurrentHashMapAtomicOperations() throws Exception {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        final int THREAD_COUNT = 10;
        final int INCREMENTS = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < INCREMENTS; j++) {
                        // 使用merge原子递增
                        map.merge("counter", 1, Integer::sum);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(THREAD_COUNT * INCREMENTS, map.get("counter"),
                "计数器应该正确累加");
    }

    @Test
    @DisplayName("BlockingQueue - put/take阻塞测试")
    @Timeout(5)
    void testBlockingQueueBlocking() throws Exception {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

        // 填满队列
        queue.put("Item1");
        assertTrue(queue.offer("Item2", 100, TimeUnit.MILLISECONDS) == false,
                "队列满时offer应该超时返回false");

        // 取出元素
        assertEquals("Item1", queue.take(), "应该取出第一个元素");

        // 从空队列取
        assertNull(queue.poll(100, TimeUnit.MILLISECONDS),
                "空队列poll应该超时返回null");
    }

    @Test
    @DisplayName("BlockingQueue - 生产者消费者测试")
    @Timeout(10)
    void testProducerConsumer() throws Exception {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
        AtomicInteger sum = new AtomicInteger(0);
        final int ITEMS = 100;

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= ITEMS; i++) {
                    queue.put(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < ITEMS; i++) {
                    Integer item = queue.take();
                    sum.addAndGet(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        assertEquals(ITEMS * (ITEMS + 1) / 2, sum.get(),
                "消费者应该收到所有元素");
    }

    @Test
    @DisplayName("PriorityBlockingQueue - 优先级测试")
    void testPriorityBlockingQueue() throws Exception {
        PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>();

        // 乱序插入
        queue.add(5);
        queue.add(1);
        queue.add(3);
        queue.add(2);
        queue.add(4);

        // 验证按优先级取出
        assertEquals(1, queue.take());
        assertEquals(2, queue.take());
        assertEquals(3, queue.take());
        assertEquals(4, queue.take());
        assertEquals(5, queue.take());
    }

    @Test
    @DisplayName("CopyOnWriteArrayList - 迭代器快照测试")
    void testCopyOnWriteArrayListSnapshot() {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        // 创建迭代器（获取快照）
        Iterator<String> iterator = list.iterator();

        // 修改列表
        list.add("D");
        list.remove("A");

        // 迭代器仍然看到旧快照
        List<String> iteratorResult = new ArrayList<>();
        iterator.forEachRemaining(iteratorResult::add);

        assertEquals(Arrays.asList("A", "B", "C"), iteratorResult,
                "迭代器应该看到创建时的快照");

        assertEquals(Arrays.asList("B", "C", "D"), list,
                "列表应该反映最新修改");
    }

    @Test
    @DisplayName("CopyOnWriteArrayList - 并发读写测试")
    @Timeout(10)
    void testCopyOnWriteArrayListConcurrency() throws Exception {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
        final int READERS = 10;
        final int WRITERS = 2;
        final int OPERATIONS = 100;

        // 初始数据
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(READERS + WRITERS);
        CountDownLatch latch = new CountDownLatch(READERS + WRITERS);
        AtomicInteger errors = new AtomicInteger(0);

        // 读线程
        for (int i = 0; i < READERS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS; j++) {
                        // 迭代不会抛ConcurrentModificationException
                        for (Integer num : list) {
                            // 读取操作
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 写线程
        for (int i = 0; i < WRITERS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS; j++) {
                        list.add(j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(0, errors.get(), "不应该有并发异常");
        assertEquals(10 + WRITERS * OPERATIONS, list.size(),
                "列表大小应该正确");
    }

    @Test
    @DisplayName("ConcurrentSkipListMap - 有序性测试")
    void testConcurrentSkipListMap() {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();

        // 乱序插入
        map.put(5, "E");
        map.put(1, "A");
        map.put(3, "C");
        map.put(2, "B");
        map.put(4, "D");

        // 验证有序
        List<Integer> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), keys,
                "键应该有序");

        // 范围查询
        NavigableMap<Integer, String> subMap = map.subMap(2, true, 4, true);
        assertEquals(Arrays.asList(2, 3, 4), new ArrayList<>(subMap.keySet()),
                "范围查询应该正确");
    }

    @Test
    @DisplayName("DelayQueue - 延迟测试")
    @Timeout(5)
    void testDelayQueue() throws Exception {
        DelayQueue<DelayedTask> queue = new DelayQueue<>();
        long now = System.currentTimeMillis();

        // 添加不同延迟的任务
        queue.put(new DelayedTask("Task-3", 300));
        queue.put(new DelayedTask("Task-1", 100));
        queue.put(new DelayedTask("Task-2", 200));

        // 验证按延迟时间取出
        DelayedTask task1 = queue.take();
        assertEquals("Task-1", task1.name);
        assertTrue(System.currentTimeMillis() - now >= 100,
                "应该至少延迟100ms");

        DelayedTask task2 = queue.take();
        assertEquals("Task-2", task2.name);
        assertTrue(System.currentTimeMillis() - now >= 200,
                "应该至少延迟200ms");

        DelayedTask task3 = queue.take();
        assertEquals("Task-3", task3.name);
        assertTrue(System.currentTimeMillis() - now >= 300,
                "应该至少延迟300ms");
    }

    @Test
    @DisplayName("并发集合性能对比")
    @Timeout(10)
    void testPerformanceComparison() throws Exception {
        final int OPERATIONS = 10000;

        // ConcurrentHashMap
        long concurrentTime = testMapPerformance(new ConcurrentHashMap<>(), OPERATIONS);

        // Hashtable
        long hashtableTime = testMapPerformance(new Hashtable<>(), OPERATIONS);

        System.out.println("ConcurrentHashMap: " + concurrentTime + "ms");
        System.out.println("Hashtable: " + hashtableTime + "ms");

//        assertTrue(concurrentTime < hashtableTime,
//                "ConcurrentHashMap应该比Hashtable更快（ps：也不一定）");
    }

    private long testMapPerformance(Map<Integer, Integer> map, int operations) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        long start = System.currentTimeMillis();

        for (int i = 0; i < 4; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        int key = threadId * operations + j;
                        map.put(key, key);
                        map.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        return System.currentTimeMillis() - start;
    }

    /**
     * 延迟任务类
     */
    static class DelayedTask implements Delayed {
        final String name;
        final long expire;

        DelayedTask(String name, long delayMs) {
            this.name = name;
            this.expire = System.currentTimeMillis() + delayMs;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = expire - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.expire, ((DelayedTask) o).expire);
        }
    }
}

/**
 * 【测试运行说明】
 *
 * 1. 运行所有测试:
 *    mvn test -Dtest=ConcurrentCollectionsTest
 *
 * 2. 运行单个测试:
 *    mvn test -Dtest=ConcurrentCollectionsTest#testConcurrentHashMapPut
 *
 * 3. 测试覆盖:
 *    - ConcurrentHashMap的并发安全性
 *    - BlockingQueue的阻塞特性
 *    - CopyOnWriteArrayList的快照迭代
 *    - 各种并发集合的特性验证
 *
 * 【注意事项】
 *
 * 1. 超时设置:
 *    - 使用@Timeout防止测试死锁
 *    - 合理设置超时时间
 *
 * 2. 并发测试:
 *    - 使用CountDownLatch同步
 *    - 验证并发操作的正确性
 *    - 检测并发异常
 *
 * 3. 性能测试:
 *    - 对比不同实现的性能
 *    - 注意预热和多次测试
 *    - 结果可能因环境而异
 */
