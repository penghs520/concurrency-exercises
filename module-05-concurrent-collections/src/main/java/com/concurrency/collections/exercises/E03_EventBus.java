package com.concurrency.collections.exercises;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * 练习3: 简易事件总线 🟡
 *
 * 【题目描述】
 * 实现一个简单的事件总线（Event Bus），支持事件的发布和订阅。
 *
 * 【要求】
 * 1. 支持不同类型的事件
 * 2. 支持多个监听器订阅同一事件
 * 3. 线程安全的发布和订阅
 * 4. 支持取消订阅
 * 5. 异步事件分发（可选）
 *
 * 【学习目标】
 * - CopyOnWriteArrayList的实际应用
 * - ConcurrentHashMap的使用
 * - 观察者模式
 * - 事件驱动架构
 *
 * 【难度】: 🟡 中等
 */
public class E03_EventBus {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 事件总线系统 ===\n");

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
     * 事件总线
     * TODO: 完成实现
     */
    static class EventBus {
        // 存储事件类型到监听器列表的映射
        // TODO: 使用ConcurrentHashMap + CopyOnWriteArrayList
        private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventListener<?>>> listeners;

        public EventBus() {
            // TODO: 初始化
            this.listeners = null; // TODO: 创建ConcurrentHashMap
        }

        /**
         * 订阅事件
         * TODO: 实现订阅逻辑
         *
         * @param eventType 事件类型
         * @param listener  监听器
         */
        public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
            // TODO: 实现
            // 提示：
            // 1. 使用computeIfAbsent获取或创建监听器列表
            // 2. 将listener添加到列表中
            // 3. CopyOnWriteArrayList保证线程安全

            // 参考代码：
            // listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            //          .add((EventListener<?>) listener);
        }

        /**
         * 取消订阅
         * TODO: 实现取消订阅
         *
         * @param eventType 事件类型
         * @param listener  监听器
         */
        public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
            // TODO: 实现
            // 提示：
            // 1. 获取监听器列表
            // 2. 从列表中移除listener
            // 3. 如果列表为空，可选择移除整个条目
        }

        /**
         * 发布事件
         * TODO: 实现事件发布
         *
         * @param event 事件对象
         */
        public <T> void publish(T event) {
            // TODO: 实现
            // 提示：
            // 1. 获取事件的Class类型
            // 2. 查找该类型的监听器列表
            // 3. 遍历列表，调用每个监听器的onEvent方法
            // 4. 注意类型转换

            // 参考代码：
            // Class<?> eventType = event.getClass();
            // CopyOnWriteArrayList<EventListener<?>> eventListeners = listeners.get(eventType);
            // if (eventListeners != null) {
            //     for (EventListener listener : eventListeners) {
            //         listener.onEvent(event);
            //     }
            // }
        }

        /**
         * 异步发布事件（可选）
         * TODO: 实现异步发布
         *
         * 提示：
         * 1. 使用线程池执行监听器回调
         * 2. 避免阻塞发布者线程
         */
        public <T> void publishAsync(T event) {
            // TODO: 可选实现
            // 提示：可以使用CompletableFuture.runAsync或线程池
        }

        /**
         * 获取订阅者数量
         */
        public int getSubscriberCount(Class<?> eventType) {
            CopyOnWriteArrayList<EventListener<?>> eventListeners = listeners.get(eventType);
            return eventListeners != null ? eventListeners.size() : 0;
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
 * 【参考输出】
 * === 事件总线系统 ===
 *
 * --- 基本功能测试 ---
 *
 * 发布UserEvent:
 * [监听器1] 收到用户事件: UserEvent{user='Alice', action='login'}
 * [监听器2] 处理用户: Alice
 *
 * 发布OrderEvent:
 * [监听器3] 收到订单事件: OrderEvent{orderId='ORD-001', amount=299.0}
 *
 * 发布另一个UserEvent:
 * [监听器1] 收到用户事件: UserEvent{user='Bob', action='logout'}
 * [监听器2] 处理用户: Bob
 *
 * --- 取消订阅测试 ---
 *
 * 添加临时监听器后，发布事件:
 * [监听器1] 收到用户事件: UserEvent{user='Charlie', action='register'}
 * [监听器2] 处理用户: Charlie
 * [临时监听器] UserEvent{user='Charlie', action='register'}
 *
 * 取消临时监听器后，发布事件:
 * [监听器1] 收到用户事件: UserEvent{user='David', action='login'}
 * [监听器2] 处理用户: David
 *
 * ==================================================
 *
 * --- 并发测试 ---
 *
 * [监听器0] 收到消息: Publisher-0 Message-0
 * [监听器1] 收到消息: Publisher-0 Message-0
 * [监听器2] 收到消息: Publisher-1 Message-0
 * ...
 *
 * ✓ 并发测试完成，无异常
 */

/**
 * 【实现提示】
 *
 * 1. 数据结构选择:
 *    - ConcurrentHashMap: 存储事件类型到监听器列表的映射
 *    - CopyOnWriteArrayList: 存储监听器列表（读多写少）
 *
 * 2. 线程安全保证:
 *    - ConcurrentHashMap保证并发put/get安全
 *    - CopyOnWriteArrayList保证并发add/remove安全
 *    - 发布事件时遍历列表不会抛ConcurrentModificationException
 *
 * 3. 注意事项:
 *    - 监听器回调可能抛异常，需要捕获
 *    - 避免监听器中执行耗时操作（考虑异步）
 *    - 注意内存泄漏（记得取消订阅）
 *
 * 【扩展功能】
 *
 * 1. 事件继承:
 *    - 支持监听父类事件
 *    - 子类事件也会触发父类监听器
 *
 * 2. 优先级:
 *    - 监听器支持优先级
 *    - 使用PriorityQueue管理监听器
 *
 * 3. 异步分发:
 *    - 使用线程池异步调用监听器
 *    - 避免阻塞发布者
 *
 * 4. 错误处理:
 *    - 监听器异常隔离
 *    - 异常回调通知
 *
 * 5. 死事件处理:
 *    - 没有监听器的事件
 *    - 记录或特殊处理
 *
 * 【实际应用】
 *
 * - Guava EventBus
 * - Spring ApplicationEvent
 * - Android EventBus
 * - 微服务事件驱动架构
 */
