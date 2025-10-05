package com.concurrency.basics.exercises;

import java.util.ArrayList;
import java.util.List;

/**
 * 练习1: 多线程文件下载器 🟢
 *
 * 【题目描述】
 * 实现一个多线程文件下载器，将大文件分成多个部分并发下载，最后合并。
 *
 * 【要求】
 * 1. 将文件分成N个部分（N为线程数）
 * 2. 每个线程下载一部分
 * 3. 所有线程下载完成后，主线程合并文件
 * 4. 实时显示下载进度
 *
 * 【学习目标】
 * - 线程的创建与启动
 * - join()方法的使用
 * - 线程间协作
 *
 * 【难度】: 🟢 基础
 */
public class E01_MultiThreadDownloader {

    // 模拟的文件大小（字节）
    private static final long FILE_SIZE = 1000;
    // 线程数量
    private static final int THREAD_COUNT = 4;

    public static void main(String[] args) {
        System.out.println("=== 多线程文件下载器 ===");
        System.out.println("文件大小: " + FILE_SIZE + " 字节");
        System.out.println("线程数量: " + THREAD_COUNT + "\n");

        MultiThreadDownloader downloader = new MultiThreadDownloader(FILE_SIZE, THREAD_COUNT);
        downloader.download();
    }

    static class MultiThreadDownloader {
        private final long fileSize;
        private final int threadCount;
        private final List<DownloadTask> tasks = new ArrayList<>();

        public MultiThreadDownloader(long fileSize, int threadCount) {
            this.fileSize = fileSize;
            this.threadCount = threadCount;
        }

        /**
         * 开始下载
         * TODO: 实现多线程下载逻辑
         *
         * 提示：
         * 1. 计算每个线程下载的起始位置和结束位置
         * 2. 创建多个DownloadTask线程
         * 3. 启动所有线程
         * 4. 等待所有线程完成（使用join）
         * 5. 合并文件
         */
        public void download() {
            long startTime = System.currentTimeMillis();

            // TODO: 计算每个线程的下载范围
            long blockSize = fileSize / threadCount;

            // TODO: 创建并启动所有下载线程
            for (int i = 0; i < threadCount; i++) {
                // 计算当前线程的下载范围
                // 起始位置 = i * blockSize
                // 结束位置 = (i == threadCount - 1) ? fileSize : (i + 1) * blockSize

                // 创建下载任务
                // 提示: new DownloadTask(i, startPos, endPos)

                // 启动线程
                // 提示: thread.start()
            }

            // TODO: 等待所有线程完成
            // 提示: 使用thread.join()遍历所有线程

            // TODO: 合并文件
            mergeFiles();

            long endTime = System.currentTimeMillis();
            System.out.println("\n下载完成! 耗时: " + (endTime - startTime) + "ms");
        }

        /**
         * 合并所有下载的部分
         */
        private void mergeFiles() {
            System.out.println("\n开始合并文件...");
            long totalBytes = 0;
            for (DownloadTask task : tasks) {
                totalBytes += task.getDownloadedBytes();
            }
            System.out.println("合并完成，总字节数: " + totalBytes);
        }

        /**
         * 下载任务线程
         */
        class DownloadTask implements Runnable {
            private final int taskId;
            private final long startPos;
            private final long endPos;
            private long downloadedBytes = 0;

            public DownloadTask(int taskId, long startPos, long endPos) {
                this.taskId = taskId;
                this.startPos = startPos;
                this.endPos = endPos;
            }

            @Override
            public void run() {
                // TODO: 实现下载逻辑
                // 提示：
                // 1. 计算需要下载的字节数: endPos - startPos
                // 2. 模拟下载过程（使用Thread.sleep模拟网络延迟）
                // 3. 更新downloadedBytes
                // 4. 打印进度信息

                System.out.println("线程" + taskId + "开始下载: [" + startPos + " - " + endPos + "]");

                // 模拟下载过程
                long bytesToDownload = endPos - startPos;
                try {
                    // 模拟网络延迟
                    Thread.sleep(100 + (long)(Math.random() * 200));

                    downloadedBytes = bytesToDownload;

                    System.out.println("线程" + taskId + "下载完成: " + downloadedBytes + " 字节");
                } catch (InterruptedException e) {
                    System.err.println("线程" + taskId + "被中断");
                    Thread.currentThread().interrupt();
                }
            }

            public long getDownloadedBytes() {
                return downloadedBytes;
            }
        }
    }
}

/**
 * 【参考输出】
 * === 多线程文件下载器 ===
 * 文件大小: 1000 字节
 * 线程数量: 4
 *
 * 线程0开始下载: [0 - 250]
 * 线程1开始下载: [250 - 500]
 * 线程2开始下载: [500 - 750]
 * 线程3开始下载: [750 - 1000]
 * 线程1下载完成: 250 字节
 * 线程0下载完成: 250 字节
 * 线程3下载完成: 250 字节
 * 线程2下载完成: 250 字节
 *
 * 开始合并文件...
 * 合并完成，总字节数: 1000
 *
 * 下载完成! 耗时: 245ms
 */
