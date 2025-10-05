package com.concurrency.project.flashsale;

/**
 * 秒杀服务接口
 *
 * 这是你需要实现的核心接口。目标是在高并发场景下：
 * 1. 防止超卖（库存不能为负）
 * 2. 保证一致性（订单数 = 实际扣减的库存数）
 * 3. 提高性能（支持高并发）
 *
 * 实现提示：
 * - 从简单的 synchronized 开始
 * - 思考锁的粒度（全局锁 vs 商品级锁）
 * - 考虑使用 ReentrantLock 的高级特性
 * - 尝试无锁数据结构（AtomicInteger）
 */
public interface FlashSaleService {

    /**
     * 秒杀下单
     *
     * 当用户参与秒杀时调用此方法。需要：
     * 1. 检查库存是否充足
     * 2. 扣减库存（原子操作）
     * 3. 生成订单
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 订单ID（成功），null（失败：库存不足或其他原因）
     */
    String buy(long userId, long productId);

    /**
     * 获取剩余库存
     *
     * @param productId 商品ID
     * @return 剩余库存数量
     */
    int getStock(long productId);

    /**
     * 添加秒杀商品
     *
     * @param product 商品对象
     */
    void addProduct(Product product);

    /**
     * 获取成功的订单数量（用于测试验证）
     *
     * @return 订单总数
     */
    int getOrderCount();
}
