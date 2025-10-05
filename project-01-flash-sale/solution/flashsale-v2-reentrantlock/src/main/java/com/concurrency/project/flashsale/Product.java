package com.concurrency.project.flashsale;

/**
 * 商品实体类
 *
 * 代表秒杀商品的基本信息
 */
public class Product {
    private final long id;           // 商品ID
    private final String name;       // 商品名称
    private final double price;      // 商品价格
    private int stock;               // 库存数量（可变）

    public Product(long id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * 扣减库存
     * 注意：此方法不是线程安全的！
     */
    public void decreaseStock() {
        if (this.stock > 0) {
            this.stock--;
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                '}';
    }
}
