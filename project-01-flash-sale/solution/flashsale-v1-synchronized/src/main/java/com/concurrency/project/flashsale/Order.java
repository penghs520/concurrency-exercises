package com.concurrency.project.flashsale;

import java.util.UUID;

/**
 * 订单实体类
 *
 * 代表秒杀成功后生成的订单
 */
public class Order {
    private final String orderId;       // 订单ID
    private final long userId;          // 用户ID
    private final long productId;       // 商品ID
    private final long timestamp;       // 下单时间戳

    public Order(long userId, long productId) {
        this.orderId = UUID.randomUUID().toString();
        this.userId = userId;
        this.productId = productId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }

    public long getUserId() {
        return userId;
    }

    public long getProductId() {
        return productId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId=" + userId +
                ", productId=" + productId +
                ", timestamp=" + timestamp +
                '}';
    }
}
