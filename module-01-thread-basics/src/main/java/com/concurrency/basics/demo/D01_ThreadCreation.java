package com.concurrency.basics.demo;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Demo 01: 线程创建的4种方式
 *
 * 本示例演示：
 * 1. 继承Thread类
 * 2. 实现Runnable接口
 * 3. 使用Lambda表达式
 * 4. 实现Callable接口（有返回值）
 */
public class D01_ThreadCreation {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 线程创建演示 ===\n");

        // 方式1: 继承Thread类
        method1_ExtendThread();
        Thread.sleep(100);

        // 方式2: 实现Runnable接口（推荐）
        method2_ImplementRunnable();
        Thread.sleep(100);

        // 方式3: 使用Lambda表达式（Java 8+，推荐）
        method3_UseLambda();
        Thread.sleep(100);

        // 方式4: 实现Callable接口（有返回值）
        method4_UseCallable();

        System.out.println("\n主线程执行完毕");
    }

    /**
     * 方式1: 继承Thread类
     * 优点: 简单直观
     * 缺点: Java单继承限制，不够灵活
     */
    private static void method1_ExtendThread() {
        System.out.println("--- 方式1: 继承Thread类 ---");

        MyThread thread = new MyThread("Worker-1");
        thread.start(); // 启动线程

        // 注意: 不能多次调用start()
        // thread.start(); // 会抛出 IllegalThreadStateException
    }

    /**
     * 方式2: 实现Runnable接口（推荐）
     * 优点: 避免单继承限制，更灵活
     * 推荐: 优先使用此方式
     */
    private static void method2_ImplementRunnable() {
        System.out.println("\n--- 方式2: 实现Runnable接口 ---");

        MyRunnable runnable = new MyRunnable("任务A");
        Thread thread = new Thread(runnable, "Worker-2");
        thread.start();

        // 同一个Runnable可以被多个线程共享
        Thread thread2 = new Thread(runnable, "Worker-3");
        thread2.start();
    }

    /**
     * 方式3: 使用Lambda表达式（Java 8+）
     * 优点: 代码简洁
     * 适用: 简单的线程任务
     */
    private static void method3_UseLambda() {
        System.out.println("\n--- 方式3: Lambda表达式 ---");

        // Lambda实现Runnable
        Thread thread = new Thread(() -> {
            System.out.println("Lambda线程: " + Thread.currentThread().getName() + " 正在执行");
        }, "Lambda-Worker");

        thread.start();

        // 更复杂的Lambda示例
        new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                System.out.println("Lambda计数: " + i);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Lambda-Counter").start();
    }

    /**
     * 方式4: 实现Callable接口（有返回值）
     * 优点: 可以返回结果，抛出异常
     * 适用: 需要获取线程执行结果的场景
     */
    private static void method4_UseCallable() throws Exception {
        System.out.println("\n--- 方式4: Callable接口（有返回值） ---");

        // 定义Callable任务
        Callable<Integer> task = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                System.out.println("Callable任务开始计算...");
                Thread.sleep(100);
                int result = 42 * 2;
                System.out.println("Callable任务计算完成");
                return result;
            }
        };

        // 使用FutureTask包装Callable
        FutureTask<Integer> futureTask = new FutureTask<>(task);
        Thread thread = new Thread(futureTask, "Callable-Worker");
        thread.start();

        // 获取结果（会阻塞直到任务完成）
        Integer result = futureTask.get();
        System.out.println("获取到Callable结果: " + result);

        // Lambda版本的Callable
        FutureTask<String> futureTask2 = new FutureTask<>(() -> {
            return "Hello from Callable Lambda";
        });
        new Thread(futureTask2).start();
        System.out.println("Lambda Callable结果: " + futureTask2.get());
    }

    /**
     * 自定义Thread类
     */
    static class MyThread extends Thread {
        public MyThread(String name) {
            super(name); // 设置线程名
        }

        @Override
        public void run() {
            System.out.println("MyThread执行: " + getName());
            System.out.println("  线程ID: " + getId());
            System.out.println("  是否存活: " + isAlive());
        }
    }

    /**
     * 自定义Runnable实现
     */
    static class MyRunnable implements Runnable {
        private final String taskName;

        public MyRunnable(String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            System.out.println("MyRunnable执行: " + threadName + " - 任务: " + taskName);
        }
    }
}
