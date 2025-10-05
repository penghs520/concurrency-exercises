package com.concurrency.async.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo 02: 异常处理
 *
 * 本示例演示：
 * 1. exceptionally - 处理异常并返回默认值
 * 2. handle - 同时处理结果和异常
 * 3. whenComplete - 完成时回调（不改变结果）
 * 4. 异常传播机制
 */
public class D02_ErrorHandling {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture异常处理演示 ===\n");

        // 1. exceptionally - 处理异常
        demo1_Exceptionally();

        // 2. handle - 处理结果和异常
        demo2_Handle();

        // 3. whenComplete - 完成时回调
        demo3_WhenComplete();

        // 4. 异常传播
        demo4_ExceptionPropagation();

        System.out.println("\n主线程执行完毕");
    }

    /**
     * Demo 1: exceptionally - 处理异常并返回默认值
     */
    private static void demo1_Exceptionally() throws Exception {
        System.out.println("--- Demo 1: exceptionally ---");

        // 场景1: 正常执行
        CompletableFuture<Integer> normalFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("  正常执行...");
            return 42;
        }).exceptionally(ex -> {
            System.out.println("  异常处理: " + ex.getMessage());
            return -1; // 这个不会被调用
        });

        System.out.println("正常结果: " + normalFuture.get());

        // 场景2: 异常处理
        CompletableFuture<Integer> errorFuture = CompletableFuture.<Integer>supplyAsync(() -> {
            System.out.println("  执行出错...");
            throw new RuntimeException("模拟异常");
        }).exceptionally(ex -> {
            System.out.println("  捕获异常: " + ex.getCause().getMessage());
            return -1; // 返回默认值
        });

        System.out.println("异常处理后: " + errorFuture.get());

        // 场景3: 实际应用 - API调用失败回退
        CompletableFuture<String> apiCall = fetchDataFromAPI()
                .exceptionally(ex -> {
                    System.out.println("  API调用失败，使用缓存数据");
                    return "缓存数据";
                });

        System.out.println("API结果: " + apiCall.get());
    }

    /**
     * Demo 2: handle - 同时处理结果和异常
     */
    private static void demo2_Handle() throws Exception {
        System.out.println("\n--- Demo 2: handle ---");

        // handle总是被调用，无论是否有异常
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("随机异常");
            }
            return "成功";
        }).handle((result, ex) -> {
            if (ex != null) {
                System.out.println("  处理异常: " + ex.getCause().getMessage());
                return "错误已处理: " + ex.getCause().getMessage();
            } else {
                System.out.println("  处理结果: " + result);
                return "成功: " + result;
            }
        });

        System.out.println("handle结果: " + future1.get());

        // 实际应用：HTTP请求的统一处理
        CompletableFuture<Response> httpResponse = fetchUser(101)
                .handle((user, ex) -> {
                    if (ex != null) {
                        return new Response(500, "Internal Error", null);
                    }
                    return new Response(200, "OK", user);
                });

        Response response = httpResponse.get();
        System.out.println("HTTP响应: " + response);
    }

    /**
     * Demo 3: whenComplete - 完成时回调（不改变结果）
     */
    private static void demo3_WhenComplete() throws Exception {
        System.out.println("\n--- Demo 3: whenComplete ---");

        // whenComplete用于日志、清理等副作用，不改变结果
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("  开始计算...");
            return 100;
        }).whenComplete((result, ex) -> {
            if (ex != null) {
                System.out.println("  [日志] 计算失败: " + ex.getMessage());
            } else {
                System.out.println("  [日志] 计算成功，结果: " + result);
            }
            // 清理资源、发送通知等
        });

        System.out.println("原始结果: " + future.get()); // 仍然是100

        // 对比：whenComplete vs handle
        System.out.println("\nwhenComplete vs handle:");

        CompletableFuture<String> withWhenComplete = CompletableFuture.supplyAsync(() -> "数据")
                .whenComplete((result, ex) -> {
                    System.out.println("  whenComplete: " + result);
                    // 无法改变结果
                });

        CompletableFuture<String> withHandle = CompletableFuture.<String>supplyAsync(() -> "数据")
                .handle((result, ex) -> {
                    System.out.println("  handle: " + result);
                    return result + " (已处理)"; // 可以改变结果
                });

        System.out.println("whenComplete结果: " + withWhenComplete.get());
        System.out.println("handle结果: " + withHandle.get());
    }

    /**
     * Demo 4: 异常传播机制
     */
    private static void demo4_ExceptionPropagation() throws Exception {
        System.out.println("\n--- Demo 4: 异常传播 ---");

        // 异常会跳过所有中间步骤，直到遇到异常处理器
        CompletableFuture<Integer> pipeline = CompletableFuture.<Integer>supplyAsync(() -> {
            System.out.println("  步骤1: 初始化");
            throw new RuntimeException("步骤1失败");
        }).thenApply(x -> {
            System.out.println("  步骤2: 转换（不会执行）");
            return x * 2;
        }).thenApply(x -> {
            System.out.println("  步骤3: 处理（不会执行）");
            return x + 10;
        }).exceptionally(ex -> {
            System.out.println("  异常处理器捕获: " + ex.getCause().getMessage());
            return -1;
        }).thenApply(x -> {
            System.out.println("  步骤4: 继续处理（会执行）");
            return x * 100;
        });

        System.out.println("最终结果: " + pipeline.get());

        // 多个异常处理器
        System.out.println("\n多个异常处理器:");
        CompletableFuture<Integer> multiHandler = CompletableFuture.supplyAsync(() -> {
            return 10;
        }).thenApply(x -> {
            if (x > 5) {
                throw new RuntimeException("值太大");
            }
            return x * 2;
        }).exceptionally(ex -> {
            System.out.println("  第一个处理器: " + ex.getCause().getMessage());
            return 0;
        }).thenApply(x -> {
            if (x == 0) {
                throw new RuntimeException("值为零");
            }
            return x + 10;
        }).exceptionally(ex -> {
            System.out.println("  第二个处理器: " + ex.getCause().getMessage());
            return -1;
        });

        System.out.println("多处理器结果: " + multiHandler.get());
    }

    // ========== 辅助方法 ==========

    private static CompletableFuture<String> fetchDataFromAPI() {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟API调用失败
            if (ThreadLocalRandom.current().nextBoolean()) {
                throw new RuntimeException("API服务不可用");
            }
            return "API数据";
        });
    }

    private static CompletableFuture<String> fetchUser(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (userId > 100) {
                throw new RuntimeException("用户不存在");
            }
            return "User-" + userId;
        });
    }

    // 响应对象
    static class Response {
        int statusCode;
        String message;
        Object data;

        Response(int statusCode, String message, Object data) {
            this.statusCode = statusCode;
            this.message = message;
            this.data = data;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "statusCode=" + statusCode +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
