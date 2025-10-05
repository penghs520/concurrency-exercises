package com.concurrency.project.flashsale;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀服务实现 - Version 1: synchronized 基础版本
 *
 * 实现思路：
 * 1. 使用 synchronized 关键字保证线程安全
 * 2. 整个 buy 方法作为临界区，保证原子性
 * 3. 简单直接，绝对防止超卖
 *
 * 优点：
 * - 实现简单，代码清晰易懂
 * - JVM 自动管理锁，不会忘记释放
 * - 绝对线程安全，不会出现竞态条件
 *
 * 缺点：
 * - 性能较低：synchronized 是重量级锁
 * - 锁粒度粗：所有商品共用一把锁（this 对象锁）
 * - 无法设置超时：线程可能长时间阻塞
 * - 适用场景有限：只适合低并发、商品少的场景
 *
 * 性能指标：
 * - 吞吐量：约 5000-8000 TPS
 * - P99 延迟：10-50ms
 *
 * 并发问题分析：
 * 为什么需要 synchronized？
 *
 * 错误示例（无锁）：
 *     Thread-1: if (stock > 0)  // stock = 1，通过检查
 *     Thread-2: if (stock > 0)  // stock = 1，也通过检查
 *     Thread-1: stock--          // stock = 0
 *     Thread-2: stock--          // stock = -1，超卖！
 *
 * 正确示例（有锁）：
 *     Thread-1: synchronized { if (stock > 0) stock-- }  // stock = 0
 *     Thread-2: synchronized { if (stock > 0) ... }      // 等待锁，获取后 stock = 0，失败
 */
public class FlashSaleServiceV1 implements FlashSaleService {

    /**
     * 商品库存映射
     * 使用 ConcurrentHashMap 避免结构性修改的并发问题
     */
    private final Map<Long, Product> productMap = new ConcurrentHashMap<>();

    /**
     * 订单计数器
     * 使用 AtomicInteger 保证计数准确（虽然在 synchronized 保护下不是必须的）
     */
    private final AtomicInteger orderCounter = new AtomicInteger(0);

    /**
     * 秒杀下单 - synchronized 实现
     *
     * 关键点：
     * 1. synchronized 修饰整个方法，锁对象是 this
     * 2. 检查库存和扣减库存在同一个临界区内，保证原子性
     * 3. 多个线程串行执行，不会同时进入
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 订单ID 或 null
     */
    @Override
    public synchronized String buy(long userId, long productId) {
        // 1. 获取商品
        Product product = productMap.get(productId);
        if (product == null) {
            return null;  // 商品不存在
        }

        // 2. 检查库存（临界区内，线程安全）
        if (product.getStock() <= 0) {
            return null;  // 库存不足
        }

        // 3. 扣减库存（临界区内，线程安全）
        product.decreaseStock();

        // 4. 生成订单
        Order order = new Order(userId, productId);

        // 5. 订单计数
        orderCounter.incrementAndGet();

        return order.getOrderId();
    }

    /**
     * 获取剩余库存
     *
     * 注意：虽然 getStock 本身是原子的，但如果外部代码基于返回值做决策，
     * 仍可能有 TOCTOU (Time-of-Check-Time-of-Use) 问题
     */
    @Override
    public int getStock(long productId) {
        Product product = productMap.get(productId);
        return product != null ? product.getStock() : 0;
    }

    @Override
    public void addProduct(Product product) {
        productMap.put(product.getId(), product);
    }

    @Override
    public int getOrderCount() {
        return orderCounter.get();
    }
}
