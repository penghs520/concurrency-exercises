package com.concurrency.async.solutions;

import java.util.concurrent.CompletableFuture;

/**
 * 练习2参考答案: 异步数据处理管道
 *
 * 实现要点：
 * 1. 使用thenCompose链接返回CompletableFuture的方法
 * 2. 使用whenComplete打印进度
 * 3. 处理null值（过滤步骤可能返回null）
 * 4. 使用exceptionally统一异常处理
 */
public class S02_AsyncPipeline {

    public static void main(String[] args) throws Exception {
        S02_AsyncPipeline solution = new S02_AsyncPipeline();

        System.out.println("=== 练习2参考答案: 异步数据处理管道 ===\n");

        String logId = "LOG-12345";
        CompletableFuture<String> pipeline = solution.processLogPipeline(logId);

        String result = pipeline.get();
        System.out.println("\n最终结果: " + result);
    }

    /**
     * 方案1: 完整的异步管道（推荐）
     */
    public CompletableFuture<String> processLogPipeline(String logId) {
        return loadRawLog(logId)
                .whenComplete((result, ex) -> {
                    if (ex == null) System.out.println("  ✓ 步骤1完成");
                })
                .thenCompose(rawLog -> parseLog(rawLog))
                .whenComplete((result, ex) -> {
                    if (ex == null) System.out.println("  ✓ 步骤2完成");
                })
                .thenCompose(parsedLog -> filterLog(parsedLog))
                .whenComplete((result, ex) -> {
                    if (ex == null) System.out.println("  ✓ 步骤3完成");
                })
                .thenCompose(parsedLog -> {
                    // 处理过滤返回null的情况
                    if (parsedLog == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return enrichLog(parsedLog);
                })
                .whenComplete((result, ex) -> {
                    if (ex == null) System.out.println("  ✓ 步骤4完成");
                })
                .thenCompose(enrichedLog -> saveLog(enrichedLog))
                .whenComplete((result, ex) -> {
                    if (ex == null) System.out.println("  ✓ 步骤5完成");
                })
                .exceptionally(ex -> {
                    System.err.println("  ✗ 管道处理失败: " + ex.getCause().getMessage());
                    return "处理失败: " + ex.getCause().getMessage();
                });
    }

    /**
     * 方案2: 使用handle处理每步的异常
     */
    public CompletableFuture<String> processLogPipelineV2(String logId) {
        return loadRawLog(logId)
                .thenCompose(rawLog -> parseLog(rawLog))
                .handle((parsedLog, ex) -> {
                    if (ex != null) {
                        System.err.println("解析失败: " + ex.getMessage());
                        return null; // 返回null表示跳过后续步骤
                    }
                    return parsedLog;
                })
                .thenCompose(parsedLog -> {
                    if (parsedLog == null) {
                        return CompletableFuture.completedFuture((ParsedLog) null);
                    }
                    return filterLog(parsedLog);
                })
                .thenCompose(parsedLog -> {
                    if (parsedLog == null) {
                        return CompletableFuture.completedFuture((EnrichedLog) null);
                    }
                    return enrichLog(parsedLog);
                })
                .thenCompose(enrichedLog -> saveLog(enrichedLog))
                .exceptionally(ex -> "处理失败: " + ex.getMessage());
    }

    /**
     * 方案3: 使用自定义工具方法简化
     */
    public CompletableFuture<String> processLogPipelineV3(String logId) {
        return loadRawLog(logId)
                .thenCompose(this::parseLog)
                .thenCompose(this::filterLog)
                .thenCompose(log -> log == null ?
                        CompletableFuture.completedFuture(null) : enrichLog(log))
                .thenCompose(this::saveLog)
                .exceptionally(ex -> "处理失败: " + ex.getCause().getMessage());
    }

    // ========== 管道步骤 ==========

    private CompletableFuture<RawLog> loadRawLog(String logId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[1/5] 加载原始日志: " + logId);
            sleep(100);
            return new RawLog(logId, "2024-01-01 10:00:00|ERROR|NullPointerException|...");
        });
    }

    private CompletableFuture<ParsedLog> parseLog(RawLog rawLog) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[2/5] 解析日志...");
            sleep(80);

            if (rawLog.content.isEmpty()) {
                throw new RuntimeException("日志内容为空");
            }

            String[] parts = rawLog.content.split("\\|");
            return new ParsedLog(rawLog.id, parts[0], parts[1], parts[2]);
        });
    }

    private CompletableFuture<ParsedLog> filterLog(ParsedLog log) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[3/5] 过滤日志...");
            sleep(50);

            if (!log.level.equals("ERROR")) {
                System.out.println("  过滤掉非ERROR日志");
                return null;
            }

            return log;
        });
    }

    private CompletableFuture<EnrichedLog> enrichLog(ParsedLog log) {
        return CompletableFuture.supplyAsync(() -> {
            if (log == null) {
                return null;
            }

            System.out.println("[4/5] 丰富日志信息...");
            sleep(120);

            String stackTrace = "at com.example.MyClass.method()...";
            return new EnrichedLog(log.id, log.timestamp, log.level, log.message, stackTrace);
        });
    }

    private CompletableFuture<String> saveLog(EnrichedLog log) {
        return CompletableFuture.supplyAsync(() -> {
            if (log == null) {
                System.out.println("[5/5] 日志已过滤，无需保存");
                return "日志已过滤，无需保存";
            }

            System.out.println("[5/5] 保存日志到数据库...");
            sleep(150);

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
