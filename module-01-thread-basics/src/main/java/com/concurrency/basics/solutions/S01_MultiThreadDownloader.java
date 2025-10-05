package com.concurrency.basics.solutions;

import java.util.ArrayList;
import java.util.List;

/**
 * 练习1参考答案: 多线程文件下载器
 *
 * 核心知识点：
 * 1. 线程的创建与启动
 * 2. join()方法等待线程完成
 * 3. 线程间数据共享
 */
public class S01_MultiThreadDownloader {

    private static final long FILE_SIZE = 1000;
    private static final int THREAD_COUNT = 4;

    public static void main(String[] args) {
        System.out.println("=== 多线程文件下载器（参考答案） ===");
        System.out.println("文件大小: " + FILE_SIZE + " 字节");
        System.out.println("线程数量: " + THREAD_COUNT + "\n");

        MultiThreadDownloader downloader = new MultiThreadDownloader(FILE_SIZE, THREAD_COUNT);
        downloader.download();
    }

    static class MultiThreadDownloader {
        private final long fileSize;
        private final int threadCount;
        private final List<Thread> threads = new ArrayList<>();
        private final List<DownloadTask> tasks = new ArrayList<>();

        public MultiThreadDownloader(long fileSize, int threadCount) {
            this.fileSize = fileSize;
            this.threadCount = threadCount;
        }

        /**
         * 开始下载
         */
        public void download() {
            long startTime = System.currentTimeMillis();

            // 1. 计算每个线程的下载范围
            long blockSize = fileSize / threadCount;

            // 2. 创建并启动所有下载线程
            for (int i = 0; i < threadCount; i++) {
                // 计算当前线程的下载范围
                long startPos = i * blockSize;
                long endPos = (i == threadCount - 1) ? fileSize : (i + 1) * blockSize;

                // 创建下载任务
                DownloadTask task = new DownloadTask(i, startPos, endPos);
                tasks.add(task);

                // 创建并启动线程
                Thread thread = new Thread(task, "Downloader-" + i);
                threads.add(thread);
                thread.start();
            }

            // 3. 等待所有线程完成
            for (Thread thread : threads) {
                try {
                    thread.join(); // 等待线程完成
                } catch (InterruptedException e) {
                    System.err.println("等待线程 " + thread.getName() + " 被中断");
                    Thread.currentThread().interrupt();
                }
            }

            // 4. 合并文件
            mergeFiles();

            long endTime = System.currentTimeMillis();
            System.out.println("\n✓ 下载完成! 耗时: " + (endTime - startTime) + "ms");
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
            System.out.println("✓ 合并完成，总字节数: " + totalBytes);

            // 验证完整性
            if (totalBytes == fileSize) {
                System.out.println("✓ 文件完整性校验通过");
            } else {
                System.err.println("✗ 文件不完整! 预期: " + fileSize + ", 实际: " + totalBytes);
            }
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
                String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " 开始下载: [" + startPos + " - " + endPos + ")");

                // 模拟下载过程
                long bytesToDownload = endPos - startPos;

                try {
                    // 模拟网络延迟（100-300ms）
                    Thread.sleep(100 + (long) (Math.random() * 200));

                    // 模拟下载进度
                    for (long i = 0; i < bytesToDownload; i += bytesToDownload / 5) {
                        downloadedBytes += bytesToDownload / 5;
                        // 打印进度
                        // System.out.println(threadName + " 进度: " +
                        //     String.format("%.1f%%", downloadedBytes * 100.0 / bytesToDownload));
                        Thread.sleep(20);
                    }

                    downloadedBytes = bytesToDownload; // 确保精确

                    System.out.println(threadName + " ✓ 下载完成: " + downloadedBytes + " 字节");

                } catch (InterruptedException e) {
                    System.err.println(threadName + " ✗ 被中断");
                    Thread.currentThread().interrupt();
                }
            }

            public long getDownloadedBytes() {
                return downloadedBytes;
            }
        }
    }

    /**
     * 【扩展优化方案】
     *
     * 1. 进度条显示：
     *    使用AtomicLong统计总下载量，主线程定时打印进度
     *
     * 2. 异常处理：
     *    - 网络异常重试机制
     *    - 下载失败的线程标记
     *
     * 3. 断点续传：
     *    - 记录每个线程的下载位置
     *    - 下次启动时从断点继续
     *
     * 4. 动态调整线程数：
     *    - 根据文件大小自动计算最优线程数
     *    - 小文件单线程，大文件多线程
     *
     * 5. 真实网络下载：
     *    - 使用HttpURLConnection设置Range头
     *    - 支持HTTP/HTTPS协议
     */
}
