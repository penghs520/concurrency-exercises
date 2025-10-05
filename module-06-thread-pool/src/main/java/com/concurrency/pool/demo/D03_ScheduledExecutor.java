package com.concurrency.pool.demo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * Demo 03: 定时任务调度器
 *
 * 本示例演示：
 * 1. schedule - 延迟执行
 * 2. scheduleAtFixedRate - 固定频率
 * 3. scheduleWithFixedDelay - 固定延迟
 * 4. 三者的区别与适用场景
 */
public class D03_ScheduledExecutor {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        System.out.println("=== 定时任务调度器演示 ===\n");

        // Demo 1: schedule - 延迟执行
        demo1_Schedule();
        Thread.sleep(6000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 2: scheduleAtFixedRate - 固定频率
        demo2_ScheduleAtFixedRate();
        Thread.sleep(10000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 3: scheduleWithFixedDelay - 固定延迟
        demo3_ScheduleWithFixedDelay();
        Thread.sleep(10000);

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Demo 4: 对比区别
        demo4_Comparison();

        System.out.println("\n主线程执行完毕");
    }

    /**
     * Demo 1: schedule - 延迟执行一次
     */
    private static void demo1_Schedule() {
        System.out.println("--- Demo 1: schedule（延迟执行） ---");
        System.out.println("适用场景: 一次性延迟任务\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        System.out.println("[" + getCurrentTime() + "] 提交任务");

        // 1. 延迟执行Runnable
        scheduler.schedule(() -> {
            System.out.println("[" + getCurrentTime() + "] Runnable任务执行（延迟2秒）");
        }, 2, TimeUnit.SECONDS);

        // 2. 延迟执行Callable（有返回值）
        ScheduledFuture<String> future = scheduler.schedule(() -> {
            System.out.println("[" + getCurrentTime() + "] Callable任务执行（延迟3秒）");
            return "任务结果: 42";
        }, 3, TimeUnit.SECONDS);

        // 获取结果
        try {
            String result = future.get();
            System.out.println("[" + getCurrentTime() + "] 获取结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        scheduler.shutdown();
    }

    /**
     * Demo 2: scheduleAtFixedRate - 固定频率执行
     * 特点: 从任务开始时间计算周期，不考虑任务执行时间
     */
    private static void demo2_ScheduleAtFixedRate() {
        System.out.println("--- Demo 2: scheduleAtFixedRate（固定频率） ---");
        System.out.println("特点: 从任务开始时间计算周期\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        System.out.println("[" + getCurrentTime() + "] 开始调度");
        System.out.println("配置: 初始延迟1秒，每隔2秒执行一次\n");

        // 初始延迟1秒，之后每隔2秒执行一次
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[" + getCurrentTime() + "] 任务开始");
            sleep(500);  // 任务执行500ms
            System.out.println("[" + getCurrentTime() + "] 任务结束（耗时500ms）");
        }, 1, 2, TimeUnit.SECONDS);

        // 运行8秒后关闭
        sleep(8000);
        scheduler.shutdown();

        System.out.println("\n说明: 任务执行时间 < 周期，按固定频率执行");
        System.out.println("执行时间点: 1s, 3s, 5s, 7s...");
    }

    /**
     * Demo 3: scheduleWithFixedDelay - 固定延迟执行
     * 特点: 从任务结束时间计算周期
     */
    private static void demo3_ScheduleWithFixedDelay() {
        System.out.println("--- Demo 3: scheduleWithFixedDelay（固定延迟） ---");
        System.out.println("特点: 从任务结束时间计算周期\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        System.out.println("[" + getCurrentTime() + "] 开始调度");
        System.out.println("配置: 初始延迟1秒，任务结束后延迟2秒再执行\n");

        // 初始延迟1秒，任务结束后延迟2秒再执行下一次
        scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("[" + getCurrentTime() + "] 任务开始");
            sleep(500);  // 任务执行500ms
            System.out.println("[" + getCurrentTime() + "] 任务结束（耗时500ms）");
        }, 1, 2, TimeUnit.SECONDS);

        // 运行8秒后关闭
        sleep(8000);
        scheduler.shutdown();

        System.out.println("\n说明: 任务结束后等待固定时间");
        System.out.println("执行时间点: 1s, 3.5s, 6s...");
        System.out.println("计算: 1s + (500ms执行 + 2s延迟) = 3.5s");
    }

    /**
     * Demo 4: 对比两种周期执行方式
     */
    private static void demo4_Comparison() throws InterruptedException {
        System.out.println("--- Demo 4: FixedRate vs FixedDelay ---\n");

        // 场景：任务执行时间可能超过周期
        System.out.println("场景: 任务执行时间3秒，周期2秒\n");

        // 1. scheduleAtFixedRate
        System.out.println("1. scheduleAtFixedRate（固定频率）:");
        ScheduledExecutorService scheduler1 = Executors.newScheduledThreadPool(1);

        final int[] count1 = {0};
        scheduler1.scheduleAtFixedRate(() -> {
            int taskId = ++count1[0];
            System.out.printf("[%s] FixedRate 任务%d 开始%n", getCurrentTime(), taskId);
            sleep(3000);  // 执行3秒（超过周期2秒）
            System.out.printf("[%s] FixedRate 任务%d 结束%n", getCurrentTime(), taskId);
        }, 0, 2, TimeUnit.SECONDS);

        sleep(10000);
        scheduler1.shutdown();

        System.out.println("\n说明: 任务执行3秒 > 周期2秒，下次任务会立即执行");
        System.out.println("实际周期: 0s -> 3s -> 6s -> 9s...\n");

        Thread.sleep(2000);

        // 2. scheduleWithFixedDelay
        System.out.println("2. scheduleWithFixedDelay（固定延迟）:");
        ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);

        final int[] count2 = {0};
        scheduler2.scheduleWithFixedDelay(() -> {
            int taskId = ++count2[0];
            System.out.printf("[%s] FixedDelay 任务%d 开始%n", getCurrentTime(), taskId);
            sleep(3000);  // 执行3秒
            System.out.printf("[%s] FixedDelay 任务%d 结束%n", getCurrentTime(), taskId);
        }, 0, 2, TimeUnit.SECONDS);

        sleep(10000);
        scheduler2.shutdown();

        System.out.println("\n说明: 任务执行3秒，然后延迟2秒");
        System.out.println("实际周期: 0s -> 5s -> 10s...");
        System.out.println("计算: 3s执行 + 2s延迟 = 5s\n");

        printComparisonTable();
    }

    /**
     * 打印对比表格
     */
    private static void printComparisonTable() {
        System.out.println("\n=== 对比总结 ===\n");
        System.out.println("| 方法 | 周期计算方式 | 任务执行时间 > 周期 | 适用场景 |");
        System.out.println("|------|-------------|-------------------|----------|");
        System.out.println("| scheduleAtFixedRate | 上次开始时间 + 周期 | 立即执行下次 | 数据同步、心跳 |");
        System.out.println("| scheduleWithFixedDelay | 上次结束时间 + 延迟 | 等待延迟后执行 | 轮询、定时清理 |");

        System.out.println("\n使用建议:");
        System.out.println("1. 固定频率（FixedRate）适用于:");
        System.out.println("   - 按固定频率执行，不受任务执行时间影响");
        System.out.println("   - 例如: 每秒采集一次数据、心跳检测");

        System.out.println("\n2. 固定延迟（FixedDelay）适用于:");
        System.out.println("   - 任务完成后等待固定时间再执行");
        System.out.println("   - 例如: 轮询检查、定时清理、重试机制");
    }

    /**
     * 实际应用示例
     */
    public static void realWorldExamples() {
        System.out.println("\n=== 实际应用示例 ===\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        // 示例1: 心跳检测（固定频率）
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[" + getCurrentTime() + "] 发送心跳包");
            // 发送心跳到服务器
        }, 0, 30, TimeUnit.SECONDS);

        // 示例2: 缓存清理（固定延迟）
        scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("[" + getCurrentTime() + "] 清理过期缓存");
            // 清理缓存逻辑
        }, 1, 60, TimeUnit.SECONDS);

        // 示例3: 延迟任务（一次性）
        scheduler.schedule(() -> {
            System.out.println("[" + getCurrentTime() + "] 发送延迟邮件");
            // 发送邮件
        }, 5, TimeUnit.MINUTES);

        // 示例4: 定时报表（固定频率）
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[" + getCurrentTime() + "] 生成每日报表");
            // 生成报表逻辑
        }, 0, 1, TimeUnit.DAYS);
    }

    /**
     * 获取当前时间
     */
    private static String getCurrentTime() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    /**
     * 辅助方法: 休眠
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
