# Java内存模型（JMM）

## 什么是Java内存模型？

**Java Memory Model (JMM)** 是Java虚拟机规范定义的一种抽象概念，用于屏蔽各种硬件和操作系统的内存访问差异，让Java程序在各种平台下都能达到一致的内存访问效果。

### 为什么需要JMM？

1. **硬件层面的问题**
   - CPU缓存（L1/L2/L3 Cache）
   - 编译器优化重排序
   - CPU指令重排序

2. **需要统一的规范**
   - 定义多线程程序的语义
   - 规定什么操作是线程安全的
   - 保证跨平台的一致性

---

## JMM的核心概念

### 1. 主内存与工作内存

```
    CPU1          CPU2          CPU3
     |             |             |
  ┌──┴──┐       ┌──┴──┐       ┌──┴──┐
  │工作 │       │工作 │        │ 工作 │
  │内存 │       │内存 │        │ 内存 │
  └──┬──┘       └──┬──┘       └──┬──┘
     │             │             │
     └─────────┬───┴─────────────┘
               │
          ┌────┴────┐
          │  主内存  │
          │ (共享)   │
          └─────────┘
```

#### 主内存（Main Memory）
- 所有线程共享
- 存储所有变量的主副本
- 对应Java堆中的对象实例数据

#### 工作内存（Working Memory）
- 每个线程私有
- 存储主内存变量的副本
- 对应CPU寄存器和高速缓存

### 2. 内存交互操作

JMM定义了8种原子操作：

| 操作 | 作用域 | 说明 |
|------|--------|------|
| **lock** | 主内存 | 锁定变量，标识为线程独占 |
| **unlock** | 主内存 | 解锁变量 |
| **read** | 主内存 | 读取变量值到工作内存 |
| **load** | 工作内存 | 把read的值放入工作内存副本 |
| **use** | 工作内存 | 传递给执行引擎 |
| **assign** | 工作内存 | 执行引擎赋值给工作内存 |
| **store** | 工作内存 | 传输到主内存准备写入 |
| **write** | 主内存 | 把store的值写入主内存 |

#### 完整的读写流程

**读操作**：
```
主内存 --[read]--> 工作内存 --[load]--> 变量副本 --[use]--> 执行引擎
```

**写操作**：
```
执行引擎 --[assign]--> 变量副本 --[store]--> 工作内存 --[write]--> 主内存
```

---

## Happens-Before原则

> JMM通过happens-before规则来保证多线程程序的有序性

### 定义
如果操作A happens-before 操作B，那么A的结果对B可见，且A的执行顺序在B之前。

### 8条规则

#### 1. 程序次序规则（Program Order Rule）
```java
int a = 1;  // [1]
int b = 2;  // [2]
int c = a + b; // [3]

// [1] happens-before [2]
// [2] happens-before [3]
```
**单线程内，代码按照书写顺序执行**

#### 2. 锁定规则（Monitor Lock Rule）
```java
synchronized (lock) {
    // [1] unlock happens-before 下一次 lock
}
```
**一个unlock操作 happens-before 后续对同一个锁的lock操作**

#### 3. volatile变量规则（Volatile Variable Rule）
```java
volatile boolean flag = false;

// 线程A
flag = true; // [1] 写volatile

// 线程B
if (flag) { // [2] 读volatile
    // [1] happens-before [2]
}
```
**对volatile变量的写 happens-before 后续对这个变量的读**

#### 4. 线程启动规则（Thread Start Rule）
```java
Thread t = new Thread(() -> {
    // [2] 线程内的操作
});
t.start(); // [1] start操作

// [1] happens-before [2]
```
**Thread.start() happens-before 线程内的每个操作**

#### 5. 线程终止规则（Thread Termination Rule）
```java
Thread t = new Thread(() -> {
    // [1] 线程内的操作
});
t.start();
t.join(); // [2] join返回

// [1] happens-before [2]
```
**线程内的所有操作 happens-before 其他线程检测到该线程终止**

#### 6. 线程中断规则（Thread Interruption Rule）
```java
thread.interrupt(); // [1] 中断操作
// happens-before
thread.isInterrupted(); // [2] 检测到中断
```

#### 7. 对象终结规则（Finalizer Rule）
```java
// 对象的构造函数 happens-before finalize()方法
```

#### 8. 传递性（Transitivity）
```java
// 如果 A happens-before B，B happens-before C
// 那么 A happens-before C
```

### Happens-Before应用示例

#### 示例1：volatile保证可见性
```java
public class VolatileExample {
    private int a = 0;
    private volatile boolean flag = false;

    // 线程A
    public void writer() {
        a = 1;          // [1]
        flag = true;    // [2] volatile写
    }

    // 线程B
    public void reader() {
        if (flag) {     // [3] volatile读
            int i = a;  // [4] 一定能看到a=1
        }
    }
}

// [1] happens-before [2] (程序次序规则)
// [2] happens-before [3] (volatile规则)
// [3] happens-before [4] (程序次序规则)
// 传递性：[1] happens-before [4]
```

#### 示例2：synchronized保证可见性
```java
public class SynchronizedExample {
    private int a = 0;

    public synchronized void writer() {
        a = 1; // [1]
    } // [2] unlock

    public synchronized void reader() {
        // [3] lock
        int i = a; // [4] 一定能看到a=1
    }
}

// [1] happens-before [2] (程序次序规则)
// [2] happens-before [3] (锁定规则)
// [3] happens-before [4] (程序次序规则)
```

---

## 三大特性的保证机制

### 1. 原子性（Atomicity）

#### 由JMM保证的原子性操作
- 基本类型的读写（long/double除外）
- reference类型的读写

#### 由锁保证的原子性
```java
// synchronized
synchronized (lock) {
    count++; // 保证原子性
}

// Lock
lock.lock();
try {
    count++; // 保证原子性
} finally {
    lock.unlock();
}
```

### 2. 可见性（Visibility）

#### volatile的实现原理
```java
volatile int value = 0;

// 汇编层面（x86）
value = 1; // 编译为：mov [addr], 1; lock addl $0, 0(%%esp)
```

**lock前缀指令的作用**：
1. 将当前处理器缓存行的数据写回主内存
2. 使其他处理器的缓存失效（MESI协议）
3. 提供内存屏障，防止指令重排序

#### 内存屏障（Memory Barrier）
```
LoadLoad屏障   : Load1; LoadLoad; Load2
StoreStore屏障 : Store1; StoreStore; Store2
LoadStore屏障  : Load1; LoadStore; Store2
StoreLoad屏障  : Store1; StoreLoad; Load2
```

**volatile的内存屏障插入策略**：
```java
// volatile写
StoreStore屏障
volatile写操作
StoreLoad屏障

// volatile读
volatile读操作
LoadLoad屏障
LoadStore屏障
```

### 3. 有序性（Ordering）

#### as-if-serial语义
> 不管怎么重排序，单线程程序的执行结果不能改变

```java
int a = 1; // [1]
int b = 2; // [2]
int c = a + b; // [3]

// [1]和[2]可以重排序，但[3]不能排到[1][2]之前
```

#### 数据依赖性
```java
a = 1; // [1]
b = a; // [2] 依赖[1]，不能重排序
```

---

## 经典案例分析

### 1. 双重检查锁定（DCL）

#### 错误版本
```java
public class Singleton {
    private static Singleton instance;

    public static Singleton getInstance() {
        if (instance == null) {           // [1] 第一次检查
            synchronized (Singleton.class) {
                if (instance == null) {   // [2] 第二次检查
                    instance = new Singleton(); // [3] 问题所在！
                }
            }
        }
        return instance;
    }
}
```

**问题分析**：
```
new Singleton() 的字节码：
1. memory = allocate();   // 分配内存
2. ctorInstance(memory);  // 初始化对象
3. instance = memory;     // 设置引用

可能被重排序为：
1. memory = allocate();
3. instance = memory;     // 提前赋值！
2. ctorInstance(memory);

线程A: 执行到步骤3 [切换]
线程B: [1]处检查instance != null，返回未初始化的对象 [✗]
```

#### 正确版本
```java
private static volatile Singleton instance; // 加volatile

// volatile禁止[2][3]重排序
```

### 2. 安全发布

#### 不安全的发布
```java
public class UnsafePublication {
    private Resource resource;

    public void init() {
        resource = new Resource(); // [1]
        // [1]和[2]可能重排序！
    }

    public Resource getResource() {
        return resource; // [2] 可能返回未初始化的对象
    }
}
```

#### 安全的发布方式

**方式1: final字段**
```java
public class SafePublication {
    private final Resource resource;

    public SafePublication() {
        resource = new Resource(); // final保证初始化完成
    }
}
```

**方式2: volatile**
```java
private volatile Resource resource;
```

**方式3: synchronized**
```java
private Resource resource;

public synchronized Resource getResource() {
    return resource;
}
```

**方式4: 静态初始化**
```java
private static final Resource resource = new Resource();
```

---

## 实战技巧

### 1. 何时使用volatile？

#### ✓ 适用场景
```java
// 1. 状态标志
volatile boolean shutdown = false;

// 2. 单次发布（安全初始化）
volatile Resource resource;

// 3. 独立观察（读多写少）
volatile long lastUpdate;
```

#### ✗ 不适用场景
```java
// 1. 复合操作
volatile int count = 0;
count++; // 非原子！需要用AtomicInteger

// 2. 不变约束
volatile int lower = 0;
volatile int upper = 10;
// lower < upper 约束无法保证
```

### 2. synchronized的内存语义

```java
synchronized (lock) {
    // 进入时：清空工作内存，从主内存加载最新值
    // 退出时：刷新工作内存到主内存
}
```

**等价于**：
```
lock.lock();    // volatile read
// 临界区
lock.unlock();  // volatile write
```

### 3. final的内存语义

```java
public class FinalExample {
    final int x;
    int y;

    public FinalExample() {
        x = 1; // [1] final写
        y = 2; // [2] 普通写
    }

    // JMM保证：[1]一定在构造函数返回前完成
    // [2]可能被重排序到构造函数外
}
```

---

## 常见误区

### 误区1：volatile保证原子性
```java
volatile int count = 0;
count++; // ✗ 非原子！分为读-改-写三步

// 正确做法
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();
```

### 误区2：synchronized只保证互斥
```java
int a = 0;
synchronized (lock) {
    a = 1; // synchronized还保证可见性和有序性
}
```

### 误区3：final变量一定不可变
```java
final List<String> list = new ArrayList<>();
list.add("item"); // ✓ final保证引用不变，但对象可变

// 真正不可变
final ImmutableList<String> list = ImmutableList.of("item");
```

---

## 性能优化

### 1. 避免伪共享（False Sharing）

#### 问题
```java
class Counter {
    volatile long count1; // 缓存行1
    volatile long count2; // 可能在同一缓存行！
}

// 线程1修改count1，导致count2的缓存行失效
// 线程2修改count2，导致count1的缓存行失效
```

#### 解决
```java
class Counter {
    volatile long count1;
    long p1, p2, p3, p4, p5, p6, p7; // 填充
    volatile long count2;
}

// 或使用@Contended注解（JDK 8+）
@sun.misc.Contended
volatile long count;
```

### 2. 减少volatile写

```java
// ✗ 不好：每次都写volatile
for (int i = 0; i < 1000; i++) {
    volatileVar = i;
}

// ✓ 好：批量处理后写一次
int temp = 0;
for (int i = 0; i < 1000; i++) {
    temp += i;
}
volatileVar = temp;
```

---

## 总结

### JMM核心要点
1. **抽象模型**：主内存 + 工作内存
2. **核心规则**：Happens-Before 8条规则
3. **三大特性**：原子性、可见性、有序性
4. **实现机制**：内存屏障 + 缓存一致性协议

### 实践建议
- 优先使用并发工具类（Atomic*/Concurrent*）
- 理解happens-before，而非死记硬背
- volatile适用于简单状态，复杂场景用锁
- 性能调优时关注缓存行和内存屏障

**JMM是并发编程的理论基础，理解它能让你写出正确且高效的并发代码！**
