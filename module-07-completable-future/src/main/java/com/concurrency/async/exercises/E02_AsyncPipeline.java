package com.concurrency.async.exercises;

import java.util.concurrent.CompletableFuture;

/**
 * 练习2: 异步数据处理管道
 *
 * 任务描述：
 * 实现一个异步数据处理流水线，用于处理日志数据。
 * 处理流程：原始日志 → 解析 → 过滤 → 转换 → 存储
 *
 * 要求实现：
 * 1. 实现完整的异步处理管道：
 *    步骤1: loadRawLog() - 加载原始日志（100ms）
 *    步骤2: parseLog() - 解析日志（80ms）
 *    步骤3: filterLog() - 过滤无效日志（50ms）
 *    步骤4: enrichLog() - 丰富日志信息（120ms）
 *    步骤5: saveLog() - 保存到数据库（150ms）
 *
 * 2. 每个步骤都可能失败，需要适当的异常处理
 *
 * 3. 如果过滤步骤返回null（无效日志），应该短路后续步骤
 *
 * 4. 使用thenCompose链接各步骤（因为每步都返回CompletableFuture）
 *
 * 5. 实现进度跟踪：每完成一步打印进度
 *
 * 难度：🟡 中等
 * 预计时间：30分钟
 */
public class E02_AsyncPipeline {

    public static void main(String[] args) throws Exception {
        E02_AsyncPipeline exercise = new E02_AsyncPipeline();

        System.out.println("=== 练习2: 异步数据处理管道 ===\n");

        // TODO: 实现异步管道
        String logId = "LOG-12345";
        CompletableFuture<String> pipeline = exercise.processLogPipeline(logId);

        String result = pipeline.get();
        System.out.println("\n最终结果: " + result);
    }

    /**
     * TODO: 实现此方法
     *
     * 提示：
     * 1. 使用 thenCompose() 链接返回CompletableFuture的方法
     * 2. 使用 thenApply() 处理普通返回值
     * 3. 使用 exceptionally() 或 handle() 处理异常
     * 4. 使用 whenComplete() 打印进度
     * 5. 注意过滤步骤可能返回null，需要特殊处理
     *
     * @param logId 日志ID
     * @return 处理结果
     */
    public CompletableFuture<String> processLogPipeline(String logId) {
        // TODO: 在这里实现你的异步管道

        // 提示：大致结构如下
        return loadRawLog(logId)
                .thenCompose(rawLog -> parseLog(rawLog))
                // TODO: 添加后续步骤
                // 提示: 使用 thenCompose 继续链接 filterLog, enrichLog, saveLog
                .thenCompose(parsedLog -> CompletableFuture.completedFuture("TODO: 实现完整管道"))
                .exceptionally(ex -> "处理失败: " + ex.getMessage());
    }

    // ========== 管道步骤（不要修改） ==========

    /**
     * 步骤1: 加载原始日志
     */
    private CompletableFuture<RawLog> loadRawLog(String logId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[1/5] 加载原始日志: " + logId);
            sleep(100);
            return new RawLog(logId, "2024-01-01 10:00:00|ERROR|NullPointerException|...");
        });
    }

    /**
     * 步骤2: 解析日志
     */
    private CompletableFuture<ParsedLog> parseLog(RawLog rawLog) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[2/5] 解析日志...");
            sleep(80);

            // 可能解析失败
            if (rawLog.content.isEmpty()) {
                throw new RuntimeException("日志内容为空");
            }

            String[] parts = rawLog.content.split("\\|");
            return new ParsedLog(rawLog.id, parts[0], parts[1], parts[2]);
        });
    }

    /**
     * 步骤3: 过滤日志（可能返回null）
     */
    private CompletableFuture<ParsedLog> filterLog(ParsedLog log) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[3/5] 过滤日志...");
            sleep(50);

            // 只保留ERROR级别
            if (!log.level.equals("ERROR")) {
                System.out.println("  过滤掉非ERROR日志");
                return null; // 返回null表示过滤掉
            }

            return log;
        });
    }

    /**
     * 步骤4: 丰富日志信息
     */
    private CompletableFuture<EnrichedLog> enrichLog(ParsedLog log) {
        return CompletableFuture.supplyAsync(() -> {
            if (log == null) {
                return null; // 传递null
            }

            System.out.println("[4/5] 丰富日志信息...");
            sleep(120);

            // 添加额外信息
            String stackTrace = "at com.example.MyClass.method()...";
            return new EnrichedLog(log.id, log.timestamp, log.level, log.message, stackTrace);
        });
    }

    /**
     * 步骤5: 保存日志
     */
    private CompletableFuture<String> saveLog(EnrichedLog log) {
        return CompletableFuture.supplyAsync(() -> {
            if (log == null) {
                return "日志已过滤，无需保存";
            }

            System.out.println("[5/5] 保存日志到数据库...");
            sleep(150);

            // 模拟保存
            return "日志已保存: " + log.id;
        });
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 数据模型 ==========

    static class RawLog {
        String id;
        String content;

        RawLog(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }

    static class ParsedLog {
        String id;
        String timestamp;
        String level;
        String message;

        ParsedLog(String id, String timestamp, String level, String message) {
            this.id = id;
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }
    }

    static class EnrichedLog {
        String id;
        String timestamp;
        String level;
        String message;
        String stackTrace;

        EnrichedLog(String id, String timestamp, String level, String message, String stackTrace) {
            this.id = id;
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
            this.stackTrace = stackTrace;
        }
    }
}
