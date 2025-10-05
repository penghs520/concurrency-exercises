package com.concurrency.project.flashsale;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀系统测试入口
 *
 * 测试场景：
 * - 1000 个用户同时抢购
 * - 商品库存只有 100 件
 * - 预期结果：恰好生成 100 个订单，库存归零
 *
 * 运行方式：
 * mvn clean compile exec:java -Dexec.mainClass="com.concurrency.project.flashsale.Main"
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // TODO: 创建你的 FlashSaleService 实现
        // FlashSaleService service = new YourFlashSaleServiceImpl();

        // 示例：使用一个空实现进行演示
        FlashSaleService service = new BuggyFlashSaleService();

        // 初始化商品：iPhone 15 Pro，库存 100
        Product product = new Product(1001L, "iPhone 15 Pro", 7999.0, 100);
        service.addProduct(product);

        System.out.println("=== 秒杀系统测试开始 ===");
        System.out.println("商品信息: " + product);
        System.out.println("并发用户数: 1000");
        System.out.println("初始库存: " + service.getStock(product.getId()));
        System.out.println();

        // 执行并发测试
        long startTime = System.currentTimeMillis();
        int successCount = runConcurrentTest(service, product.getId(), 1000);
        long endTime = System.currentTimeMillis();

        // 输出测试结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("成功下单数: " + successCount);
        System.out.println("剩余库存: " + service.getStock(product.getId()));
        System.out.println("订单总数: " + service.getOrderCount());
        System.out.println("耗时: " + (endTime - startTime) + " ms");

        // 验证正确性
        int expectedOrders = 100;
        int actualOrders = service.getOrderCount();
        int finalStock = service.getStock(product.getId());

        System.out.println("\n=== 正确性验证 ===");
        if (actualOrders == expectedOrders && finalStock == 0) {
            System.out.println("✓ 测试通过！无超卖，库存一致");
        } else {
            System.out.println("✗ 测试失败！");
            System.out.println("  预期订单数: " + expectedOrders + ", 实际: " + actualOrders);
            System.out.println("  预期库存: 0, 实际: " + finalStock);
            if (actualOrders > expectedOrders) {
                System.out.println("  ⚠ 发生超卖！超卖数量: " + (actualOrders - expectedOrders));
            }
        }
    }

    /**
     * 运行并发测试
     *
     * @param service    秒杀服务
     * @param productId  商品ID
     * @param threadCount 并发线程数
     * @return 成功抢购的数量
     */
    private static int runConcurrentTest(FlashSaleService service, long productId, int threadCount)
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 提交所有任务
        for (int i = 0; i < threadCount; i++) {
            final long userId = 10000L + i;
            executor.submit(() -> {
                try {
                    // 等待统一开始信号（模拟真实秒杀场景）
                    startLatch.await();

                    // 执行秒杀
                    String orderId = service.buy(userId, productId);

                    if (orderId != null) {
                        successCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("用户 " + userId + " 秒杀异常: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 发出开始信号
        System.out.println("所有线程准备完毕，开始秒杀！");
        startLatch.countDown();

        // 等待所有线程完成
        latch.await();
        executor.shutdown();

        return successCounter.get();
    }

    /**
     * 一个有 bug 的实现（演示竞态条件）
     *
     * ⚠ 这个实现存在严重的并发问题！
     * 请实现正确的版本来修复它。
     */
    static class BuggyFlashSaleService implements FlashSaleService {
        private Product product;
        private int orderCount = 0;

        @Override
        public String buy(long userId, long productId) {
            // ❌ 错误：check-then-act 不是原子操作
            if (product.getStock() > 0) {
                // 模拟业务处理延迟
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // ❌ 错误：多个线程同时执行这里
                product.decreaseStock();
                orderCount++;

                return "ORDER-" + System.nanoTime();
            }
            return null;
        }

        @Override
        public int getStock(long productId) {
            return product.getStock();
        }

        @Override
        public void addProduct(Product product) {
            this.product = product;
        }

        @Override
        public int getOrderCount() {
            return orderCount;
        }
    }
}
