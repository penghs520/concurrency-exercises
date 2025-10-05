package com.concurrency.collections.solutions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 练习3参考答案: 简易事件总线
 *
 * 核心知识点:
 * 1. CopyOnWriteArrayList的应用
 * 2. ConcurrentHashMap的使用
 * 3. 观察者模式实现
 * 4. 线程安全的事件分发
 */
public class S03_EventBus {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 事件总线系统（参考答案） ===\n");

        // 测试事件总线
        testEventBus();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试并发场景
        testConcurrentEventBus();
    }

    /**
     * 测试基本功能
     */
    private static void testEventBus() {
        System.out.println("--- 基本功能测试 ---\n");

        EventBus eventBus = new EventBus();

        // 订阅用户事件
        eventBus.subscribe(UserEvent.class, event -> {
            System.out.println("[监听器1] 收到用户事件: " + event);
        });

        eventBus.subscribe(UserEvent.class, event -> {
            System.out.println("[监听器2] 处理用户: " + event.username);
        });

        // 订阅订单事件
        eventBus.subscribe(OrderEvent.class, event -> {
            System.out.println("[监听器3] 收到订单事件: " + event);
        });

        // 发布事件
        System.out.println("发布UserEvent:");
        eventBus.publish(new UserEvent("Alice", "login"));

        System.out.println("\n发布OrderEvent:");
        eventBus.publish(new OrderEvent("ORD-001", 299.0));

        System.out.println("\n发布另一个UserEvent:");
        eventBus.publish(new UserEvent("Bob", "logout"));

        // 取消订阅测试
        System.out.println("\n--- 取消订阅测试 ---");
        EventListener<UserEvent> listener = event -> {
            System.out.println("[临时监听器] " + event);
        };

        eventBus.subscribe(UserEvent.class, listener);
        System.out.println("\n添加临时监听器后，发布事件:");
        eventBus.publish(new UserEvent("Charlie", "register"));

        eventBus.unsubscribe(UserEvent.class, listener);
        System.out.println("\n取消临时监听器后，发布事件:");
        eventBus.publish(new UserEvent("David", "login"));
    }

    /**
     * 测试并发场景
     */
    private static void testConcurrentEventBus() throws Exception {
        System.out.println("--- 并发测试 ---\n");

        EventBus eventBus = new EventBus();
        final int LISTENER_COUNT = 5;
        final int PUBLISHER_COUNT = 3;

        // 注册多个监听器
        for (int i = 0; i < LISTENER_COUNT; i++) {
            final int listenerId = i;
            eventBus.subscribe(MessageEvent.class, event -> {
                // 模拟处理耗时
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[监听器" + listenerId + "] 收到消息: " + event.message);
            });
        }

        // 多个线程并发发布事件
        Thread[] publishers = new Thread[PUBLISHER_COUNT];
        for (int i = 0; i < PUBLISHER_COUNT; i++) {
            final int publisherId = i;
            publishers[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    eventBus.publish(new MessageEvent(
                            "Publisher-" + publisherId + " Message-" + j
                    ));
                }
            });
            publishers[i].start();
        }

        // 等待所有发布者完成
        for (Thread publisher : publishers) {
            publisher.join();
        }

        Thread.sleep(500); // 等待所有监听器处理完成

        System.out.println("\n✓ 并发测试完成，无异常");
    }

    /**
     * 事件监听器接口
     */
    @FunctionalInterface
    interface EventListener<T> {
        void onEvent(T event);
    }

    /**
     * 事件总线实现
     */
    static class EventBus {
        // 存储事件类型到监听器列表的映射
        // ConcurrentHashMap保证并发安全
        // CopyOnWriteArrayList适合读多写少的监听器列表
        private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventListener<?>>> listeners;

        public EventBus() {
            this.listeners = new ConcurrentHashMap<>();
        }

        /**
         * 订阅事件
         */
        @SuppressWarnings("unchecked")
        public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
            // 使用computeIfAbsent原子地获取或创建监听器列表
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add((EventListener<?>) listener);
        }

        /**
         * 取消订阅
         */
        @SuppressWarnings("unchecked")
        public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
            CopyOnWriteArrayList<EventListener<?>> eventListeners = listeners.get(eventType);
            if (eventListeners != null) {
                eventListeners.remove((EventListener<?>) listener);

                // 可选：如果列表为空，移除整个条目
                if (eventListeners.isEmpty()) {
                    listeners.remove(eventType);
                }
            }
        }

        /**
         * 发布事件
         */
        @SuppressWarnings("unchecked")
        public <T> void publish(T event) {
            Class<?> eventType = event.getClass();
            CopyOnWriteArrayList<EventListener<?>> eventListeners = listeners.get(eventType);

            if (eventListeners != null) {
                // 遍历监听器列表，调用每个监听器
                // CopyOnWriteArrayList保证迭代安全，不会抛ConcurrentModificationException
                for (EventListener<?> listener : eventListeners) {
                    try {
                        ((EventListener<T>) listener).onEvent(event);
                    } catch (Exception e) {
                        // 捕获监听器异常，避免影响其他监听器
                        System.err.println("监听器执行失败: " + e.getMessage());
                    }
                }
            }
        }

        /**
         * 异步发布事件
         * 使用线程池实现异步发布
         */
        public <T> void publishAsync(T event) {
            // 使用CompletableFuture异步执行（Java 17兼容）
            java.util.concurrent.CompletableFuture.runAsync(() -> publish(event));
        }

        /**
         * 获取订阅者数量
         */
        public int getSubscriberCount(Class<?> eventType) {
            CopyOnWriteArrayList<EventListener<?>> eventListeners = listeners.get(eventType);
            return eventListeners != null ? eventListeners.size() : 0;
        }

        /**
         * 清空所有订阅
         */
        public void clear() {
            listeners.clear();
        }
    }

    /**
     * 用户事件
     */
    static class UserEvent {
        final String username;
        final String action;

        UserEvent(String username, String action) {
            this.username = username;
            this.action = action;
        }

        @Override
        public String toString() {
            return "UserEvent{user='" + username + "', action='" + action + "'}";
        }
    }

    /**
     * 订单事件
     */
    static class OrderEvent {
        final String orderId;
        final double amount;

        OrderEvent(String orderId, double amount) {
            this.orderId = orderId;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "OrderEvent{orderId='" + orderId + "', amount=" + amount + "}";
        }
    }

    /**
     * 消息事件
     */
    static class MessageEvent {
        final String message;

        MessageEvent(String message) {
            this.message = message;
        }
    }
}

/**
 * 【知识点总结】
 *
 * 1. 数据结构选择:
 *    - ConcurrentHashMap: 事件类型到监听器列表的映射
 *    - CopyOnWriteArrayList: 监听器列表（读多写少）
 *
 * 2. 线程安全保证:
 *    - subscribe/unsubscribe: CopyOnWriteArrayList的add/remove是线程安全的
 *    - publish: 遍历CopyOnWriteArrayList不会抛ConcurrentModificationException
 *    - computeIfAbsent: 原子地创建监听器列表
 *
 * 3. 异常隔离:
 *    - 每个监听器用try-catch包裹
 *    - 一个监听器失败不影响其他监听器
 *
 * 4. 性能考虑:
 *    - 发布事件频繁（读操作）：CopyOnWriteArrayList无锁，性能好
 *    - 订阅/取消订阅少（写操作）：CopyOnWriteArrayList复制开销可接受
 *
 * 【扩展功能实现】
 *
 * 1. 支持事件继承:
 *    public <T> void publish(T event) {
 *        Class<?> eventType = event.getClass();
 *        // 发布给精确类型的监听器
 *        publishToListeners(eventType, event);
 *        // 发布给父类型的监听器
 *        for (Class<?> superType : getAllSuperTypes(eventType)) {
 *            publishToListeners(superType, event);
 *        }
 *    }
 *
 * 2. 支持优先级:
 *    class PriorityEventListener<T> implements Comparable<PriorityEventListener<T>> {
 *        int priority;
 *        EventListener<T> listener;
 *
 *        public int compareTo(PriorityEventListener<T> other) {
 *            return Integer.compare(this.priority, other.priority);
 *        }
 *    }
 *    // 使用CopyOnWriteArrayList时需要排序
 *
 * 3. 死事件处理:
 *    public <T> void publish(T event) {
 *        if (listeners.get(event.getClass()) == null) {
 *            // 没有监听器，发布DeadEvent
 *            publish(new DeadEvent(event));
 *        }
 *    }
 *
 * 4. 性能监控:
 *    class EventBusMetrics {
 *        AtomicLong publishCount = new AtomicLong();
 *        AtomicLong listenerCount = new AtomicLong();
 *        LongAdder totalLatency = new LongAdder();
 *    }
 *
 * 【实际框架对比】
 *
 * - Guava EventBus: 功能丰富，支持注解、异步等
 * - Spring ApplicationEvent: 与Spring集成，支持事务绑定
 * - Android EventBus: 专为Android优化，支持粘性事件
 * - RxJava: 响应式编程，支持复杂的事件流处理
 */
