package com.concurrency.project.flashsale;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀系统性能基准测试
 *
 * 使用 JMH (Java Microbenchmark Harness) 进行微基准测试
 *
 * 测试场景：
 * 1. 单线程性能（基准）
 * 2. 多线程性能（1/10/100/1000 线程）
 * 3. 三个版本对比（V1/V2/V3）
 *
 * 测试指标：
 * - Throughput: 吞吐量（操作/秒）
 * - AverageTime: 平均延迟
 * - SampleTime: 采样延迟（P50/P95/P99）
 *
 * 运行方式：
 * mvn clean package
 * java -jar target/benchmarks.jar
 *
 * 快速运行（减少迭代）：
 * java -jar target/benchmarks.jar -f 1 -wi 3 -i 5
 *
 * 指定测试：
 * java -jar target/benchmarks.jar V1Benchmark
 */
@BenchmarkMode(Mode.Throughput)  // 测试吞吐量
@OutputTimeUnit(TimeUnit.SECONDS)  // 输出单位：操作/秒
@State(Scope.Benchmark)  // 所有线程共享状态
@Warmup(iterations = 5, time = 1)  // 预热 5 次，每次 1 秒
@Measurement(iterations = 10, time = 1)  // 测试 10 次，每次 1 秒
@Fork(1)  // 1 个进程
public class PerformanceBenchmark {

    /**
     * 测试状态：Version 1 (Synchronized)
     */
    @State(Scope.Benchmark)
    public static class V1State {
        FlashSaleService service;
        AtomicLong userIdCounter;

        @Setup(Level.Trial)
        public void setup() {
            service = new FlashSaleServiceV1();
            // 初始化大量库存，避免快速耗尽
            Product product = new Product(1001L, "Test Product", 100.0, 1000000);
            service.addProduct(product);
            userIdCounter = new AtomicLong(10000);
        }
    }

    /**
     * 测试状态：Version 2 (ReentrantLock)
     */
    @State(Scope.Benchmark)
    public static class V2State {
        FlashSaleServiceV2 service;
        AtomicLong userIdCounter;

        @Setup(Level.Trial)
        public void setup() {
            service = new FlashSaleServiceV2();
            Product product = new Product(1001L, "Test Product", 100.0, 1000000);
            service.addProduct(product);
            userIdCounter = new AtomicLong(10000);
        }
    }

    /**
     * 测试状态：Version 3 (Optimized)
     */
    @State(Scope.Benchmark)
    public static class V3State {
        FlashSaleServiceV3 service;
        AtomicLong userIdCounter;

        @Setup(Level.Trial)
        public void setup() {
            service = new FlashSaleServiceV3();
            Product product = new Product(1001L, "Test Product", 100.0, 1000000);
            service.addProduct(product);
            userIdCounter = new AtomicLong(10000);
        }
    }

    /**
     * Benchmark: V1 - Synchronized 版本
     */
    @Benchmark
    @Threads(1)  // 单线程
    public String testV1_1Thread(V1State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    @Benchmark
    @Threads(10)
    public String testV1_10Threads(V1State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    @Benchmark
    @Threads(100)
    public String testV1_100Threads(V1State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    /**
     * Benchmark: V2 - ReentrantLock 版本
     */
    @Benchmark
    @Threads(1)
    public String testV2_1Thread(V2State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    @Benchmark
    @Threads(10)
    public String testV2_10Threads(V2State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    @Benchmark
    @Threads(100)
    public String testV2_100Threads(V2State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    /**
     * Benchmark: V3 - Optimized 版本
     */
    @Benchmark
    @Threads(1)
    public String testV3_1Thread(V3State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    @Benchmark
    @Threads(10)
    public String testV3_10Threads(V3State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    @Benchmark
    @Threads(100)
    public String testV3_100Threads(V3State state) {
        return state.service.buy(state.userIdCounter.incrementAndGet(), 1001L);
    }

    /**
     * 主函数：运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PerformanceBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)  // 快速运行：减少预热
                .measurementIterations(5)  // 快速运行：减少测试
                .build();

        new Runner(opt).run();
    }
}
