# Module 04: 原子类教程

## 1. 为什么需要原子类？

### 1.1 并发问题回顾

在多线程环境下，简单的递增操作并非原子的：

```java
public class Counter {
    private int count = 0;

    public void increment() {
        count++;  // 非原子操作！
    }
}
```

**问题分析**：`count++` 包含三个步骤：
1. **读取**：从内存读取count的值
2. **修改**：将值加1
3. **写入**：将新值写回内存

在多线程环境下，这三步可能被打断，导致数据不一致。

### 1.2 传统解决方案：synchronized

```java
public synchronized void increment() {
    count++;  // 加锁保证原子性
}
```

**缺点**：
- 阻塞式操作，有上下文切换开销
- 所有线程串行化执行
- 性能较差

### 1.3 更好的方案：原子类

```java
private AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet();  // 原子操作，无锁
}
```

**优点**：
- 非阻塞（lock-free）
- 性能更好（CAS机制）
- API更丰富

---

## 2. CAS原理

### 2.1 什么是CAS？

**CAS (Compare-And-Swap)** 是一种乐观锁机制：

```
CAS(内存地址V, 期望值A, 新值B):
  if (内存中的值 == A) {
      将内存值更新为 B
      return true
  } else {
      return false
  }
```

### 2.2 CAS的硬件支持

CAS是CPU级别的原子指令：
- **x86**: `CMPXCHG` 指令
- **ARM**: `LDREX/STREX` 指令对

Java通过`Unsafe`类调用这些指令：

```java
// Unsafe类的CAS方法（底层调用）
public final native boolean compareAndSwapInt(
    Object o,        // 对象
    long offset,     // 字段偏移量
    int expected,    // 期望值
    int x            // 新值
);
```

### 2.3 CAS的使用示例

```java
AtomicInteger atomicInt = new AtomicInteger(10);

// CAS操作
boolean success = atomicInt.compareAndSet(10, 20);
// 如果当前值是10，更新为20，返回true
// 如果当前值不是10，不更新，返回false

System.out.println(success);           // true
System.out.println(atomicInt.get());   // 20
```

### 2.4 CAS的自旋重试

当CAS失败时，通常使用自旋重试：

```java
public void increment() {
    while (true) {
        int current = count.get();
        int next = current + 1;
        if (count.compareAndSet(current, next)) {
            break;  // 成功则退出
        }
        // 失败则重试
    }
}
```

**AtomicInteger内部就是这样实现的！**

### 2.5 CAS的优缺点

**优点**：
- ✅ 非阻塞，性能好
- ✅ 无死锁风险
- ✅ 适合读多写少场景

**缺点**：
- ❌ ABA问题（后面讲解）
- ❌ 自旋消耗CPU
- ❌ 只能保证单个变量的原子性

---

## 3. 基本原子类

### 3.1 AtomicInteger

**常用方法**：

```java
AtomicInteger ai = new AtomicInteger(0);

// 基本操作
ai.get();                    // 获取当前值
ai.set(10);                  // 设置值
ai.lazySet(10);             // 最终会设置成功（不保证立即可见）

// 自增/自减
ai.incrementAndGet();        // ++i (先增后取)
ai.getAndIncrement();        // i++ (先取后增)
ai.decrementAndGet();        // --i
ai.getAndDecrement();        // i--

// 加减
ai.addAndGet(5);            // i += 5
ai.getAndAdd(5);            // temp=i; i+=5; return temp

// CAS操作
ai.compareAndSet(10, 20);   // CAS核心方法

// 函数式更新（Java 8+）
ai.updateAndGet(x -> x * 2);        // 先更新后取值
ai.getAndUpdate(x -> x * 2);        // 先取值后更新
ai.accumulateAndGet(5, (x,y) -> x+y); // 累加
```

**典型应用**：

```java
// 1. 线程安全计数器
AtomicInteger requestCount = new AtomicInteger(0);

public void handleRequest() {
    requestCount.incrementAndGet();  // 每个请求计数+1
    // 处理请求...
}

// 2. 生成唯一ID
AtomicInteger idGenerator = new AtomicInteger(0);

public int nextId() {
    return idGenerator.incrementAndGet();
}
```

### 3.2 AtomicLong

与`AtomicInteger`类似，用于长整型：

```java
AtomicLong al = new AtomicLong(0L);
al.incrementAndGet();
al.addAndGet(100L);
```

### 3.3 AtomicBoolean

用于原子地操作布尔值：

```java
AtomicBoolean flag = new AtomicBoolean(false);

// CAS操作
if (flag.compareAndSet(false, true)) {
    // 只有一个线程能进入这里
    doExpensiveOperation();
}

// 获取并设置
boolean oldValue = flag.getAndSet(true);
```

**典型应用 - 单次初始化**：

```java
public class LazyInit {
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            // 只执行一次
            doActualInit();
        }
    }
}
```

---

## 4. 引用型原子类

### 4.1 AtomicReference

用于原子地更新对象引用：

```java
public class User {
    private String name;
    private int age;
    // 构造器、getter、setter...
}

AtomicReference<User> userRef = new AtomicReference<>();
userRef.set(new User("Alice", 25));

// CAS更新引用
User oldUser = new User("Alice", 25);
User newUser = new User("Bob", 30);
userRef.compareAndSet(oldUser, newUser);

// 函数式更新
userRef.updateAndGet(user -> {
    user.setAge(user.getAge() + 1);
    return user;
});
```

**典型应用 - 不可变对象更新**：

```java
public class ImmutableConfig {
    private final AtomicReference<Map<String, String>> configRef
        = new AtomicReference<>(new HashMap<>());

    public void updateConfig(String key, String value) {
        configRef.updateAndGet(oldMap -> {
            Map<String, String> newMap = new HashMap<>(oldMap);
            newMap.put(key, value);
            return newMap;  // 返回新的不可变副本
        });
    }

    public String getConfig(String key) {
        return configRef.get().get(key);
    }
}
```

---

## 5. ABA问题

### 5.1 什么是ABA问题？

**场景描述**：

1. 线程1读取值为A
2. 线程2将A改为B
3. 线程3将B改回A
4. 线程1执行CAS(A, C)，成功！但中间状态已经变化过

**代码示例**：

```java
AtomicInteger balance = new AtomicInteger(100);

// 线程1
int money = balance.get();  // 读取100
// ... 被挂起 ...

// 线程2：取出100，又存入100
balance.compareAndSet(100, 0);   // 100 -> 0
balance.compareAndSet(0, 100);   // 0 -> 100

// 线程1恢复
balance.compareAndSet(money, money - 50);  // 成功！但逻辑错误
```

### 5.2 ABA问题的危害

**栈的ABA问题**（经典案例）：

```
初始栈: A -> B -> C

线程1：准备弹出A
1. 读取 top = A
2. 准备设置 top = B

线程2（插队）：
1. 弹出 A (top = B)
2. 弹出 B (top = C)
3. 压入 D (top = D -> C)
4. 压入 A (top = A -> D -> C)

线程1恢复：
CAS(A, B) 成功！但B已经不在栈中了！
结果：top = B（野指针！）
```

### 5.3 解决方案1：AtomicStampedReference

**原理**：每次更新时增加版本号（邮戳）

```java
public class AtomicStampedReference<V> {
    private static class Pair<T> {
        final T reference;  // 引用
        final int stamp;    // 版本号
    }

    public boolean compareAndSet(
        V expectedReference,  // 期望引用
        V newReference,       // 新引用
        int expectedStamp,    // 期望版本号
        int newStamp          // 新版本号
    );
}
```

**使用示例**：

```java
AtomicStampedReference<Integer> asr =
    new AtomicStampedReference<>(100, 0);  // 初始值100，版本0

// 线程1
int[] stampHolder = new int[1];
Integer ref = asr.get(stampHolder);  // ref=100, stamp=0
int stamp = stampHolder[0];

// 线程2修改了值
asr.compareAndSet(100, 200, 0, 1);  // 100->200, 版本0->1
asr.compareAndSet(200, 100, 1, 2);  // 200->100, 版本1->2

// 线程1尝试CAS（会失败，因为版本号不匹配）
boolean success = asr.compareAndSet(
    100,    // 期望引用 ✓
    50,     // 新引用
    0,      // 期望版本 ✗ (实际是2)
    1       // 新版本
);
System.out.println(success);  // false
```

### 5.4 解决方案2：AtomicMarkableReference

**原理**：使用布尔标记而非版本号

```java
AtomicMarkableReference<Integer> amr =
    new AtomicMarkableReference<>(100, false);

boolean[] markHolder = new boolean[1];
Integer ref = amr.get(markHolder);

// CAS操作
amr.compareAndSet(
    100,    // 期望引用
    200,    // 新引用
    false,  // 期望标记
    true    // 新标记
);
```

**使用场景**：
- `AtomicStampedReference`：需要知道修改次数
- `AtomicMarkableReference`：只需知道是否被修改过

---

## 6. LongAdder vs AtomicLong

### 6.1 AtomicLong的性能瓶颈

高并发下，所有线程竞争同一个AtomicLong变量：

```
线程1 ---|
线程2 ---|--> CAS竞争 AtomicLong --> 热点
线程3 ---|
```

大量CAS失败导致自旋，CPU空转。

### 6.2 LongAdder的设计思想

**分段累加**（类似ConcurrentHashMap的分段锁思想）：

```
线程1 --> Cell[0] ---|
线程2 --> Cell[1] ---|--> 最终sum() = Σ Cell[i]
线程3 --> Cell[2] ---|
```

**核心思想**：
- 写操作：分散到多个Cell，降低竞争
- 读操作：sum()汇总所有Cell

### 6.3 LongAdder使用示例

```java
LongAdder adder = new LongAdder();

// 多线程累加
adder.increment();      // +1
adder.add(10);          // +10
adder.decrement();      // -1

// 获取总和（汇总所有Cell）
long sum = adder.sum();

// 重置
adder.reset();

// 累加并重置
long sumAndReset = adder.sumThenReset();
```

### 6.4 性能对比

```java
// 基准测试：10个线程，每个线程累加100万次
AtomicLong atomicLong = new AtomicLong();
LongAdder longAdder = new LongAdder();

// AtomicLong: 耗时约 500ms
// LongAdder:  耗时约 50ms（提升10倍！）
```

### 6.5 选择建议

| 场景 | 推荐 |
|-----|------|
| 低并发 | `AtomicLong` |
| 需要精确的中间值 | `AtomicLong` |
| 高并发累加 | `LongAdder` |
| 统计、计数器 | `LongAdder` |

**注意**：`LongAdder.sum()`不是强一致性的，高并发下可能有微小误差。

---

## 7. 数组型原子类

### 7.1 AtomicIntegerArray

```java
AtomicIntegerArray array = new AtomicIntegerArray(10);

// 原子地更新数组元素
array.set(0, 100);
array.getAndIncrement(0);           // array[0]++
array.compareAndSet(0, 100, 200);   // array[0]: 100->200

// 函数式更新
array.updateAndGet(0, x -> x * 2);
```

**使用场景**：多线程统计、计数数组

### 7.2 AtomicLongArray / AtomicReferenceArray

类似用法，分别用于long和对象引用。

---

## 8. 字段更新器

### 8.1 为什么需要字段更新器？

**场景**：已有类无法修改，但需要原子地更新其字段。

```java
public class User {
    volatile int age;  // 必须是volatile
    // 无法修改为AtomicInteger
}
```

### 8.2 AtomicIntegerFieldUpdater

```java
public class User {
    volatile int age;  // 必须是volatile + 非private

    private static final AtomicIntegerFieldUpdater<User> AGE_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(User.class, "age");

    public void increaseAge() {
        AGE_UPDATER.incrementAndGet(this);
    }

    public boolean compareAndSetAge(int expect, int update) {
        return AGE_UPDATER.compareAndSet(this, expect, update);
    }
}
```

**优点**：
- 无需修改原有类
- 节省内存（不用创建AtomicInteger对象）

**限制**：
- 字段必须是`volatile`
- 字段不能是`private`
- 只能用于实例字段（非static）

---

## 9. 原子类的底层原理

### 9.1 Unsafe类

Java的原子类底层依赖`sun.misc.Unsafe`：

```java
public final class Unsafe {
    // CAS操作
    public native boolean compareAndSwapInt(
        Object o, long offset, int expected, int x);

    public native boolean compareAndSwapLong(
        Object o, long offset, long expected, long x);

    public native boolean compareAndSwapObject(
        Object o, long offset, Object expected, Object x);

    // 内存屏障
    public native void loadFence();
    public native void storeFence();
    public native void fullFence();
}
```

### 9.2 AtomicInteger源码分析

```java
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;  // value字段的内存偏移量

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    private volatile int value;  // 实际存储的值

    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }

    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
}
```

### 9.3 volatile的作用

原子类的value字段都是`volatile`修饰：

```java
private volatile int value;
```

**作用**：
1. **可见性**：一个线程的修改立即对其他线程可见
2. **禁止重排序**：保证happens-before语义

---

## 10. 最佳实践

### 10.1 何时使用原子类？

**适用场景**：
- ✅ 单个变量的原子操作
- ✅ 读多写少
- ✅ 简单的累加、计数
- ✅ 无锁数据结构

**不适用场景**：
- ❌ 多个变量需要协调更新
- ❌ 复杂的业务逻辑
- ❌ 需要阻塞等待（用Lock）

### 10.2 避免ABA问题

```java
// ❌ 可能有ABA问题
AtomicReference<Node> head = new AtomicReference<>(node);

// ✅ 使用版本号
AtomicStampedReference<Node> head =
    new AtomicStampedReference<>(node, 0);
```

### 10.3 高并发计数器选择

```java
// 低并发（<10线程）
AtomicLong counter = new AtomicLong();

// 高并发（>10线程）
LongAdder counter = new LongAdder();
```

### 10.4 使用函数式API

```java
// ❌ 老式写法
while (true) {
    int current = atomicInt.get();
    int next = complexCalculation(current);
    if (atomicInt.compareAndSet(current, next)) {
        break;
    }
}

// ✅ 函数式写法（更简洁）
atomicInt.updateAndGet(this::complexCalculation);
```

### 10.5 字段更新器的使用时机

```java
// 当有大量实例时，使用字段更新器节省内存

// ❌ 每个对象都有一个AtomicInteger（浪费内存）
public class Task {
    private AtomicInteger status = new AtomicInteger(0);
}

// ✅ 共享一个Updater（节省内存）
public class Task {
    private volatile int status = 0;

    private static final AtomicIntegerFieldUpdater<Task> STATUS_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(Task.class, "status");
}
```

---

## 11. 无锁编程

### 11.1 无锁栈（Treiber Stack）

```java
public class LockFreeStack<E> {
    private final AtomicReference<Node<E>> top = new AtomicReference<>();

    private static class Node<E> {
        final E item;
        Node<E> next;
        Node(E item) { this.item = item; }
    }

    public void push(E item) {
        Node<E> newHead = new Node<>(item);
        while (true) {
            Node<E> oldHead = top.get();
            newHead.next = oldHead;
            if (top.compareAndSet(oldHead, newHead)) {
                return;  // 成功
            }
            // 失败则重试
        }
    }

    public E pop() {
        while (true) {
            Node<E> oldHead = top.get();
            if (oldHead == null) {
                return null;
            }
            Node<E> newHead = oldHead.next;
            if (top.compareAndSet(oldHead, newHead)) {
                return oldHead.item;
            }
        }
    }
}
```

### 11.2 无锁队列（Michael-Scott Queue）

基于AtomicReference实现的无锁队列（练习题中实现）。

---

## 12. 常见面试题

**Q1: CAS的ABA问题如何解决？**
A: 使用`AtomicStampedReference`（版本号）或`AtomicMarkableReference`（布尔标记）。

**Q2: AtomicInteger的底层实现？**
A: 基于`Unsafe.compareAndSwapInt()`，是CPU级别的CAS指令。

**Q3: LongAdder为什么比AtomicLong快？**
A: LongAdder采用分段累加，降低了CAS竞争的热点。

**Q4: volatile和AtomicInteger的区别？**
A:
- `volatile`：保证可见性，但不保证复合操作的原子性
- `AtomicInteger`：基于CAS，保证复合操作的原子性

**Q5: 什么时候用synchronized，什么时候用原子类？**
A:
- 简单的计数、标志位：原子类（性能好）
- 复杂的业务逻辑、多变量协调：synchronized

---

## 13. 性能对比

### 13.1 基准测试

```
场景：10个线程，每个线程累加100万次

synchronized:     ~1500ms
ReentrantLock:    ~1200ms
AtomicLong:       ~500ms
LongAdder:        ~50ms
```

### 13.2 性能分析

- **synchronized**：重量级锁，有上下文切换
- **ReentrantLock**：轻量级，但仍需加锁
- **AtomicLong**：CAS自旋，无锁
- **LongAdder**：分段累加，竞争最小

---

## 14. 总结

### 核心要点
1. **CAS**：乐观锁，硬件级原子指令
2. **AtomicInteger系列**：单变量原子操作
3. **ABA问题**：使用版本号解决
4. **LongAdder**：高并发场景的计数器首选
5. **无锁编程**：基于CAS实现lock-free数据结构

### 学习路径
- 理解CAS原理 → 掌握基本原子类 → 解决ABA问题 → 优化高并发计数器 → 无锁数据结构

---

## 15. 参考资料

- [Java Concurrency in Practice](https://jcip.net/) - Chapter 15
- [JDK Atomic Package](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/atomic/package-summary.html)
- [The Art of Multiprocessor Programming](https://www.elsevier.com/books/the-art-of-multiprocessor-programming/herlihy/978-0-12-415950-1)
- [Doug Lea's Homepage](http://gee.cs.oswego.edu/dl/) - J.U.C作者

完成本模块后，继续学习并发集合，了解如何构建线程安全的复杂数据结构。
