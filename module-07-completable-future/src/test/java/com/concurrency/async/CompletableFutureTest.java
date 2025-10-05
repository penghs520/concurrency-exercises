package com.concurrency.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompletableFuture 测试类
 *
 * 测试要点：
 * 1. 基本创建和完成
 * 2. 链式调用
 * 3. 异常处理
 * 4. 组合多个Future
 * 5. 超时和取消
 */
@DisplayName("CompletableFuture测试")
class CompletableFutureTest {

    @Test
    @DisplayName("1. 创建CompletableFuture")
    void testCreation() throws Exception {
        // completedFuture
        CompletableFuture<String> completed = CompletableFuture.completedFuture("Hello");
        assertEquals("Hello", completed.get());
        assertTrue(completed.isDone());

        // supplyAsync
        CompletableFuture<Integer> supply = CompletableFuture.supplyAsync(() -> 42);
        assertEquals(42, supply.get());

        // runAsync
        CompletableFuture<Void> run = CompletableFuture.runAsync(() -> {
            // 执行无返回值任务
        });
        assertNull(run.get());

        // 手动完成
        CompletableFuture<String> manual = new CompletableFuture<>();
        manual.complete("Manual");
        assertEquals("Manual", manual.get());
    }

    @Test
    @DisplayName("2. thenApply - 转换结果")
    void testThenApply() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 10)
                .thenApply(x -> x * 2)
                .thenApply(x -> x + 5);

        assertEquals(25, future.get());
    }

    @Test
    @DisplayName("3. thenCompose - 扁平化")
    void testThenCompose() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "user")
                .thenCompose(name -> CompletableFuture.supplyAsync(() -> name.toUpperCase()));

        assertEquals("USER", future.get());
    }

    @Test
    @DisplayName("4. thenAccept - 消费结果")
    void testThenAccept() throws Exception {
        StringBuilder sb = new StringBuilder();

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Hello")
                .thenAccept(s -> sb.append(s));

        future.get();
        assertEquals("Hello", sb.toString());
    }

    @Test
    @DisplayName("5. thenRun - 执行操作")
    void testThenRun() throws Exception {
        boolean[] executed = {false};

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Done")
                .thenRun(() -> executed[0] = true);

        future.get();
        assertTrue(executed[0]);
    }

    @Test
    @DisplayName("6. exceptionally - 异常处理")
    void testExceptionally() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.<Integer>supplyAsync(() -> {
            throw new RuntimeException("Error");
        }).exceptionally(ex -> {
            assertTrue(ex.getCause() instanceof RuntimeException);
            return -1;
        });

        assertEquals(-1, future.get());
    }

    @Test
    @DisplayName("7. handle - 处理结果和异常")
    void testHandle() throws Exception {
        // 正常情况
        CompletableFuture<String> success = CompletableFuture.supplyAsync(() -> "OK")
                .handle((result, ex) -> ex == null ? "Success: " + result : "Error: " + ex.getMessage());

        assertEquals("Success: OK", success.get());

        // 异常情况
        CompletableFuture<String> failure = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Fail");
        }).handle((result, ex) -> ex == null ? "Success: " + result : "Error: " + ex.getCause().getMessage());

        assertEquals("Error: Fail", failure.get());
    }

    @Test
    @DisplayName("8. whenComplete - 完成时回调")
    void testWhenComplete() throws Exception {
        boolean[] called = {false};

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 42)
                .whenComplete((result, ex) -> {
                    called[0] = true;
                    assertNull(ex);
                    assertEquals(42, result);
                });

        assertEquals(42, future.get());
        assertTrue(called[0]);
    }

    @Test
    @DisplayName("9. thenCombine - 合并两个Future")
    void testThenCombine() throws Exception {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);

        CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> a + b);

        assertEquals(30, combined.get());
    }

    @Test
    @DisplayName("10. thenAcceptBoth - 消费两个结果")
    void testThenAcceptBoth() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");

        StringBuilder sb = new StringBuilder();
        CompletableFuture<Void> result = f1.thenAcceptBoth(f2, (s1, s2) -> {
            sb.append(s1).append(" ").append(s2);
        });

        result.get();
        assertEquals("Hello World", sb.toString());
    }

    @Test
    @DisplayName("11. applyToEither - 任一完成即处理")
    void testApplyToEither() throws Exception {
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
            sleep(10);
            return "Fast";
        });

        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Slow";
        });

        CompletableFuture<String> result = fast.applyToEither(slow, s -> s);

        assertEquals("Fast", result.get());
    }

    @Test
    @DisplayName("12. allOf - 等待所有完成")
    void testAllOf() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "B");
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "C");

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(f1, f2, f3);
        allFutures.get();

        assertTrue(f1.isDone());
        assertTrue(f2.isDone());
        assertTrue(f3.isDone());

        List<String> results = Arrays.asList(f1.get(), f2.get(), f3.get());
        assertTrue(results.contains("A"));
        assertTrue(results.contains("B"));
        assertTrue(results.contains("C"));
    }

    @Test
    @DisplayName("13. anyOf - 等待任一完成")
    void testAnyOf() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "A";
        });

        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
            sleep(10);
            return "B";
        });

        CompletableFuture<Object> fastest = CompletableFuture.anyOf(f1, f2);
        assertEquals("B", fastest.get());
    }

    @Test
    @DisplayName("14. 异常传播")
    void testExceptionPropagation() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.<Integer>supplyAsync(() -> {
            throw new RuntimeException("Stage 1 error");
        }).thenApply(x -> x * 2) // 不会执行
                .thenApply(x -> x + 1) // 不会执行
                .exceptionally(ex -> -1);

        assertEquals(-1, future.get());
    }

    @Test
    @DisplayName("15. 并行性能测试")
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    void testParallelPerformance() throws Exception {
        long start = System.currentTimeMillis();

        // 并行执行3个任务，每个100ms
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Task1";
        });

        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Task2";
        });

        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Task3";
        });

        CompletableFuture.allOf(f1, f2, f3).get();

        long duration = System.currentTimeMillis() - start;

        // 并行执行应该接近100ms，而非300ms
        assertTrue(duration < 200, "并行执行耗时应小于200ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("16. 手动完成异常")
    void testCompleteExceptionally() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Manual error"));

        assertThrows(ExecutionException.class, () -> future.get());
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("17. 取消Future")
    void testCancel() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Result";
        });

        future.cancel(true);

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("18. getNow - 立即获取")
    void testGetNow() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Result";
        });

        // 未完成时返回默认值
        String result = future.getNow("Default");
        assertEquals("Default", result);
    }

    @Test
    @DisplayName("19. join vs get")
    void testJoinVsGet() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");

        // join不抛出检查异常
        String result = future.join();
        assertEquals("Hello", result);

        // get抛出检查异常
        assertDoesNotThrow(() -> {
            String result2 = future.get();
            assertEquals("Hello", result2);
        });
    }

    @Test
    @DisplayName("20. 复杂组合场景")
    void testComplexCombination() throws Exception {
        // 模拟：获取用户 -> 获取订单 -> 计算总价
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> "User123");

        CompletableFuture<List<Integer>> ordersFuture = userFuture
                .thenCompose(user -> CompletableFuture.supplyAsync(() ->
                        Arrays.asList(100, 200, 300)
                ));

        CompletableFuture<Integer> totalFuture = ordersFuture
                .thenApply(orders -> orders.stream().mapToInt(Integer::intValue).sum());

        assertEquals(600, totalFuture.get());
    }

    // ========== 辅助方法 ==========

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
