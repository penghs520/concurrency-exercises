package com.concurrency.project.flashsale;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Version 3 测试入口
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Version 3: 生产级优化版本 ===\n");

        FlashSaleServiceV3 service = new FlashSaleServiceV3();

        // 初始化商品
        Product product = new Product(1001L, "iPhone 15 Pro", 7999.0, 100);
        service.addProduct(product);

        System.out.println("商品信息: " + product);
        System.out.println("并发用户数: 1000");
        System.out.println("初始库存: " + service.getStock(product.getId()));
        System.out.println();

        // 执行测试
        long startTime = System.currentTimeMillis();
        int successCount = runConcurrentTest(service, product.getId(), 1000);
        long endTime = System.currentTimeMillis();

        // 输出结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("成功下单数: " + successCount);
        System.out.println("剩余库存: " + service.getStock(product.getId()));
        System.out.println("订单总数: " + service.getOrderCount());
        System.out.println("耗时: " + (endTime - startTime) + " ms");
        System.out.println("吞吐量: " + (successCount * 1000 / (endTime - startTime)) + " TPS");

        // 验证
        System.out.println("\n=== 正确性验证 ===");
        if (service.getOrderCount() == 100 && service.getStock(product.getId()) == 0) {
            System.out.println("✓ 测试通过！无超卖，库存一致");
        } else {
            System.out.println("✗ 测试失败！");
        }
    }

    private static int runConcurrentTest(FlashSaleServiceV3 service, long productId, int threadCount)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long userId = 10000L + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String orderId = service.buy(userId, productId);
                    if (orderId != null) {
                        successCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        System.out.println("开始秒杀！");
        startLatch.countDown();
        latch.await();
        executor.shutdown();

        return successCounter.get();
    }
}
