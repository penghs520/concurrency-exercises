# Module 04: 原子类

## 学习目标

完成本模块后，你将掌握：
- ✅ CAS（Compare-And-Swap）原理与应用
- ✅ 原子类的使用（AtomicInteger、AtomicLong、AtomicBoolean）
- ✅ AtomicReference与对象引用的原子操作
- ✅ ABA问题及其解决方案
- ✅ LongAdder在高并发场景下的优势
- ✅ 原子字段更新器的使用

---

## 模块内容

### 📖 理论学习
阅读 [tutorial.md](tutorial.md) 了解原子类原理

### 💻 演示代码（Demo）
1. **D01_AtomicBasics** - 原子类基础操作
2. **D02_CASDemo** - CAS机制深入演示
3. **D03_LongAdder** - LongAdder高性能计数器

### ✏️ 练习题（Exercises）
1. **E01_ClickCounter** 🟢 - 线程安全的点击计数器
2. **E02_LockFreeStack** 🟡 - 无锁栈实现
3. **E03_ABAProblem** 🟡 - ABA问题演示与解决 ⭐

### ✅ 参考答案（Solutions）
每道练习题提供详细的参考实现和注释

---

## 快速开始

### 1. 运行Demo
```bash
# 编译
mvn compile

# 运行原子类基础示例
mvn exec:java -Dexec.mainClass="com.concurrency.atomic.demo.D01_AtomicBasics"

# 运行CAS机制示例
mvn exec:java -Dexec.mainClass="com.concurrency.atomic.demo.D02_CASDemo"

# 运行LongAdder示例
mvn exec:java -Dexec.mainClass="com.concurrency.atomic.demo.D03_LongAdder"
```

### 2. 完成练习
```bash
# 在 src/main/java/com/concurrency/atomic/exercises/ 目录下编写代码
# 查看 solutions/ 目录对比答案
```

### 3. 运行测试
```bash
mvn test -Dtest=AtomicTest
```

---

## 知识点清单

### 核心API

#### 基本原子类
- `AtomicInteger` / `AtomicLong` / `AtomicBoolean`
  - `get()` / `set()`
  - `getAndIncrement()` / `incrementAndGet()`
  - `getAndAdd(delta)` / `addAndGet(delta)`
  - `compareAndSet(expect, update)` - CAS操作
  - `updateAndGet(UnaryOperator)` - 函数式更新

#### 引用型原子类
- `AtomicReference<V>`
  - `compareAndSet(expect, update)`
  - `getAndSet(newValue)`
  - `updateAndGet(UnaryOperator)`

#### 解决ABA问题
- `AtomicStampedReference<V>` - 版本号戳
  - `compareAndSet(expectedRef, newRef, expectedStamp, newStamp)`
- `AtomicMarkableReference<V>` - 布尔标记
  - `compareAndSet(expectedRef, newRef, expectedMark, newMark)`

#### 高性能累加器
- `LongAdder` / `DoubleAdder`
  - `increment()` / `add(x)`
  - `sum()` - 获取总和
  - 适用于高并发累加场景

#### 字段更新器
- `AtomicIntegerFieldUpdater<T>`
- `AtomicLongFieldUpdater<T>`
- `AtomicReferenceFieldUpdater<T,V>`

### CAS原理
```
Compare-And-Swap (比较并交换):
1. 读取内存值 V
2. 比较 V == A (期望值)
3. 如果相等，将 V 更新为 B (新值)
4. 返回操作是否成功
```

---

## 常见问题

**Q: CAS相比synchronized有什么优势？**
A:
- CAS是乐观锁，synchronized是悲观锁
- CAS无阻塞，性能更好（无上下文切换）
- CAS适合读多写少的场景

**Q: 什么是ABA问题？**
A: 线程1读取值为A，线程2将A改为B再改回A，线程1的CAS仍会成功，但中间状态已改变。
解决方案：使用`AtomicStampedReference`增加版本号。

**Q: LongAdder为什么比AtomicLong快？**
A:
- AtomicLong：所有线程竞争同一个变量（热点）
- LongAdder：内部分段累加，降低竞争（类似ConcurrentHashMap思想）

**Q: 何时使用AtomicLong vs LongAdder？**
A:
- 低并发或需要精确值时：AtomicLong
- 高并发累加场景：LongAdder（读时需要sum()汇总）

---

## 学习建议

1. **理解CAS本质**：硬件级别的原子操作（CPU的CMPXCHG指令）
2. **对比synchronized**：什么时候用原子类，什么时候用锁
3. **注意ABA问题**：理解场景和解决方案
4. **性能测试**：实际对比AtomicLong vs LongAdder的性能差异
5. **无锁编程**：理解lock-free数据结构的设计思路

---

## 扩展阅读

- [Java Atomic Variables官方文档](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/atomic/package-summary.html)
- 《Java并发编程实战》第15章
- [Understanding CAS in Java](https://www.baeldung.com/java-compare-and-swap)
- 论文: [Treiber Stack](https://en.wikipedia.org/wiki/Treiber_stack) - 无锁栈算法

---

## 下一步

完成本模块后，继续学习：
👉 **[Module 05: 并发集合](../module-05-concurrent-collections/)**

学习`ConcurrentHashMap`、`CopyOnWriteArrayList`等线程安全集合
