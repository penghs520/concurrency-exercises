# 并发集合教程

## 一、为什么需要并发集合？

### 1.1 普通集合的线程安全问题

```java
// 线程不安全
Map<String, Integer> map = new HashMap<>();
// 多线程同时put可能导致：
// 1. 数据丢失
// 2. 死循环（JDK 7）
// 3. 数据覆盖

List<String> list = new ArrayList<>();
// 多线程同时add可能导致：
// 1. ArrayIndexOutOfBoundsException
// 2. 数据丢失
```

### 1.2 同步包装器的问题

```java
// 低效的解决方案
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
// 问题：
// 1. 全表锁，并发性能差
// 2. 迭代时需要手动加锁
// 3. 复合操作不是原子的

synchronized (syncMap) {
    if (!syncMap.containsKey(key)) {
        syncMap.put(key, value); // 仍需手动同步
    }
}
```

### 1.3 并发集合的优势

```java
// 高效的并发集合
ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
concurrentMap.putIfAbsent(key, value); // 原子操作，无需手动同步

// 优势：
// 1. 更细粒度的锁（分段锁/CAS）
// 2. 更好的并发性能
// 3. 提供原子复合操作
// 4. 线程安全的迭代器
```

---

## 二、ConcurrentHashMap详解

### 2.1 演进历史

**JDK 7 - 分段锁（Segment）**
```
ConcurrentHashMap (默认16个段)
├── Segment 0 (独立锁)
│   └── HashEntry[]
├── Segment 1 (独立锁)
│   └── HashEntry[]
...
└── Segment 15 (独立锁)
    └── HashEntry[]

- 最多支持16个线程并发写
- 锁粒度：Segment级别
```

**JDK 8+ - CAS + synchronized**
```
ConcurrentHashMap
└── Node[] table
    ├── Node (链表)
    └── TreeNode (红黑树，链表长度>8时转换)

- 锁粒度：单个桶（Bucket）
- 无写操作时完全无锁（CAS）
- 并发度更高
```

### 2.2 核心操作原理

#### put操作
```java
public V put(K key, V value) {
    // 1. 计算hash
    int hash = spread(key.hashCode());

    // 2. 定位桶位置
    Node<K,V>[] tab = table;
    int i = (n - 1) & hash;

    // 3. 如果桶为空，CAS插入（无锁）
    if (tabAt(tab, i) == null) {
        if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
            break; // 成功
    }
    // 4. 如果桶不为空，synchronized锁住桶
    else {
        synchronized (f = tabAt(tab, i)) {
            // 插入链表或红黑树
        }
    }
}
```

**关键点**：
- 首次插入：CAS无锁
- 冲突插入：只锁当前桶，不影响其他桶
- 读操作：完全无锁（volatile保证可见性）

#### get操作
```java
public V get(Object key) {
    Node<K,V>[] tab = table;
    int hash = spread(key.hashCode());

    // 无需加锁，直接读取
    Node<K,V> e = tabAt(tab, (n - 1) & hash);

    // 遍历链表/红黑树
    while (e != null) {
        if (e.hash == hash && key.equals(e.key))
            return e.val; // 找到
        e = e.next;
    }
    return null;
}
```

**关键点**：
- 完全无锁
- volatile读保证可见性
- 可能读到旧值（弱一致性）

### 2.3 原子复合操作

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// 1. putIfAbsent - 不存在则插入
Integer oldValue = map.putIfAbsent("key", 1);
// 等价于（但是原子的）：
// if (!map.containsKey("key")) {
//     map.put("key", 1);
// }

// 2. compute - 原子计算
map.compute("counter", (k, v) -> v == null ? 1 : v + 1);
// 原子地增加计数器

// 3. merge - 原子合并
map.merge("counter", 1, Integer::sum);
// 不存在则插入1，存在则加1

// 4. computeIfAbsent - 延迟初始化
map.computeIfAbsent("key", k -> expensiveComputation());
// 只有不存在时才调用计算函数

// 5. computeIfPresent - 存在则更新
map.computeIfPresent("key", (k, v) -> v * 2);
// 存在则翻倍
```

### 2.4 size()方法的特殊性

```java
// size()是弱一致的
int size = map.size();
// 注意：
// 1. 不是精确值（在并发环境下）
// 2. 不会阻塞其他操作
// 3. 适合作为估算值
// 4. 如需精确值，需要外部同步
```

### 2.5 迭代器的弱一致性

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);

// 迭代过程中的并发修改
for (String key : map.keySet()) {
    map.put("c", 3); // 不会抛出ConcurrentModificationException
    // 但可能看不到新插入的"c"
}

// 特点：
// 1. 不会fail-fast（不抛异常）
// 2. 可能看到部分新元素
// 3. 保证不会死循环
```

---

## 三、BlockingQueue家族

### 3.1 BlockingQueue接口

```java
public interface BlockingQueue<E> extends Queue<E> {
    // 插入操作
    boolean add(E e);        // 满则抛异常
    boolean offer(E e);      // 满则返回false
    void put(E e);          // 满则阻塞 ⭐
    boolean offer(E e, long timeout, TimeUnit unit); // 满则等待

    // 移除操作
    E remove();             // 空则抛异常
    E poll();               // 空则返回null
    E take();               // 空则阻塞 ⭐
    E poll(long timeout, TimeUnit unit); // 空则等待

    // 检查操作
    E element();            // 空则抛异常
    E peek();               // 空则返回null
}
```

**操作对比表**：
```
操作 | 抛异常    | 返回特殊值 | 阻塞    | 超时
-----|----------|-----------|---------|--------
插入 | add(e)   | offer(e)  | put(e)  | offer(e, time, unit)
移除 | remove() | poll()    | take()  | poll(time, unit)
检查 | element()| peek()    | -       | -
```

### 3.2 ArrayBlockingQueue

**特点**：
- 数组实现
- 有界队列（创建时指定容量）
- 单锁实现（putLock = takeLock）
- FIFO顺序
- 公平/非公平模式

**使用示例**：
```java
// 创建容量为10的队列
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

// 生产者
new Thread(() -> {
    try {
        for (int i = 0; i < 20; i++) {
            queue.put("Item-" + i); // 队列满时阻塞
            System.out.println("生产: Item-" + i);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

// 消费者
new Thread(() -> {
    try {
        while (true) {
            String item = queue.take(); // 队列空时阻塞
            System.out.println("消费: " + item);
            Thread.sleep(100); // 模拟处理
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
```

**内部实现**：
```java
public class ArrayBlockingQueue<E> {
    final Object[] items;      // 存储元素
    int takeIndex;             // 取出位置
    int putIndex;              // 放入位置
    int count;                 // 元素个数

    final ReentrantLock lock;  // 单锁
    final Condition notEmpty;  // 非空条件
    final Condition notFull;   // 非满条件

    public void put(E e) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)
                notFull.await(); // 队列满，等待

            enqueue(e);
            notEmpty.signal(); // 通知消费者
        } finally {
            lock.unlock();
        }
    }
}
```

### 3.3 LinkedBlockingQueue

**特点**：
- 链表实现
- 可选有界（默认Integer.MAX_VALUE，几乎无界）
- 双锁实现（putLock和takeLock分离）
- FIFO顺序
- 吞吐量高于ArrayBlockingQueue

**使用示例**：
```java
// 无界队列（小心OOM）
BlockingQueue<Task> queue1 = new LinkedBlockingQueue<>();

// 有界队列（推荐）
BlockingQueue<Task> queue2 = new LinkedBlockingQueue<>(1000);
```

**双锁优势**：
```java
public class LinkedBlockingQueue<E> {
    private final ReentrantLock putLock = new ReentrantLock();
    private final ReentrantLock takeLock = new ReentrantLock();

    // 生产者和消费者可以并发执行
    public void put(E e) {
        putLock.lock(); // 只锁put操作
        try {
            // 插入元素
        } finally {
            putLock.unlock();
        }
    }

    public E take() {
        takeLock.lock(); // 只锁take操作
        try {
            // 取出元素
        } finally {
            takeLock.unlock();
        }
    }
}
```

### 3.4 PriorityBlockingQueue

**特点**：
- 无界队列
- 基于堆（Heap）实现
- 元素需实现Comparable或提供Comparator
- 自动扩容

**使用示例**：
```java
// 任务优先级队列
class Task implements Comparable<Task> {
    String name;
    int priority; // 优先级（数字越小越高）

    @Override
    public int compareTo(Task other) {
        return Integer.compare(this.priority, other.priority);
    }
}

BlockingQueue<Task> queue = new PriorityBlockingQueue<>();
queue.put(new Task("普通任务", 5));
queue.put(new Task("紧急任务", 1));
queue.put(new Task("低优先级", 10));

Task task = queue.take(); // 总是取出优先级最高的
```

### 3.5 DelayQueue

**特点**：
- 无界队列
- 元素需实现Delayed接口
- 只有到期的元素才能被取出
- 用于定时任务

**使用示例**：
```java
class DelayedTask implements Delayed {
    private final String name;
    private final long delayTime; // 延迟时间（毫秒）
    private final long expire;    // 过期时间戳

    public DelayedTask(String name, long delayTime) {
        this.name = name;
        this.delayTime = delayTime;
        this.expire = System.currentTimeMillis() + delayTime;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(
            expire - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.expire, ((DelayedTask) o).expire);
    }
}

// 使用
DelayQueue<DelayedTask> queue = new DelayQueue<>();
queue.put(new DelayedTask("Task1", 3000)); // 3秒后执行
queue.put(new DelayedTask("Task2", 1000)); // 1秒后执行

DelayedTask task = queue.take(); // 阻塞直到有任务到期
System.out.println(task.name); // 输出: Task2（先到期）
```

### 3.6 SynchronousQueue

**特点**：
- 零容量队列
- 每个put必须等待一个take
- 直接交换（handoff）
- 适合传递性设计

**使用示例**：
```java
SynchronousQueue<String> queue = new SynchronousQueue<>();

// 生产者
new Thread(() -> {
    try {
        System.out.println("准备放入数据...");
        queue.put("data"); // 阻塞，直到有消费者取走
        System.out.println("数据已被取走");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

Thread.sleep(2000); // 模拟延迟

// 消费者
new Thread(() -> {
    try {
        String data = queue.take(); // 取出数据
        System.out.println("取到数据: " + data);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

// 输出：
// 准备放入数据...
// (2秒延迟)
// 取到数据: data
// 数据已被取走
```

**应用场景**：
- `Executors.newCachedThreadPool()` 内部使用
- 需要确保任务被立即处理

---

## 四、CopyOnWrite集合

### 4.1 CopyOnWriteArrayList

**核心思想**：
- 读操作不加锁
- 写操作复制整个数组
- 最终一致性

**实现原理**：
```java
public class CopyOnWriteArrayList<E> {
    private transient volatile Object[] array;
    final transient ReentrantLock lock = new ReentrantLock();

    // 读操作 - 无锁
    public E get(int index) {
        return (E) array[index]; // 直接读取，无锁
    }

    // 写操作 - 复制整个数组
    public boolean add(E e) {
        lock.lock();
        try {
            Object[] elements = array;
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len + 1); // 复制
            newElements[len] = e;
            array = newElements; // 原子替换
            return true;
        } finally {
            lock.unlock();
        }
    }
}
```

**使用示例**：
```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// 100个读线程
for (int i = 0; i < 100; i++) {
    new Thread(() -> {
        for (String item : list) { // 快照迭代，不会抛异常
            System.out.println(item);
        }
    }).start();
}

// 1个写线程
new Thread(() -> {
    list.add("new item"); // 写时复制
}).start();
```

**适用场景**：
- ✅ 读操作远多于写操作（如监听器列表）
- ✅ 集合较小
- ✅ 可以容忍短暂的数据不一致
- ❌ 写操作频繁
- ❌ 集合很大（复制开销大）
- ❌ 需要实时一致性

**典型应用**：
```java
// 事件监听器列表
public class EventSource {
    private final CopyOnWriteArrayList<EventListener> listeners
        = new CopyOnWriteArrayList<>();

    public void addListener(EventListener listener) {
        listeners.add(listener); // 写操作少
    }

    public void fireEvent(Event event) {
        for (EventListener listener : listeners) { // 读操作多
            listener.onEvent(event);
        }
    }
}
```

### 4.2 CopyOnWriteArraySet

```java
// 基于CopyOnWriteArrayList实现
public class CopyOnWriteArraySet<E> extends AbstractSet<E> {
    private final CopyOnWriteArrayList<E> al;

    public boolean add(E e) {
        return al.addIfAbsent(e); // 保证唯一性
    }
}

// 使用
Set<String> set = new CopyOnWriteArraySet<>();
set.add("A");
set.add("B");
set.add("A"); // 不会重复添加
```

---

## 五、ConcurrentSkipListMap/Set

### 5.1 跳表（Skip List）数据结构

```
Level 3:  1 -----------------> 9 ----------> null
Level 2:  1 -------> 4 ------> 9 ----------> null
Level 1:  1 --> 3 -> 4 --> 7 > 9 -> 11 ----> null
Level 0:  1 -> 2 -> 3 -> 4 -> 5 -> 7 -> 9 -> 11 -> null

- 底层是有序链表
- 上层是索引层（加速查找）
- 查找时间复杂度：O(log n)
- 插入/删除时间复杂度：O(log n)
```

**特点**：
- 有序Map/Set
- 无锁实现（CAS）
- 可排序
- 支持范围查询

### 5.2 使用示例

```java
// 1. ConcurrentSkipListMap
ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
map.put(3, "C");
map.put(1, "A");
map.put(2, "B");

// 按键有序
for (Integer key : map.keySet()) {
    System.out.println(key); // 输出: 1, 2, 3
}

// 范围查询
SortedMap<Integer, String> subMap = map.subMap(1, 3); // [1, 3)

// 2. ConcurrentSkipListSet
ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();
set.add(5);
set.add(2);
set.add(8);

System.out.println(set); // 输出: [2, 5, 8] (有序)

// 范围操作
NavigableSet<Integer> subset = set.subSet(2, true, 8, false); // [2, 8)
```

### 5.3 与TreeMap的对比

```
特性              | ConcurrentSkipListMap | TreeMap
-----------------|----------------------|----------
线程安全          | 是                    | 否
底层结构          | 跳表                  | 红黑树
并发性能          | 高（无锁CAS）          | 需要外部同步
时间复杂度        | O(log n)             | O(log n)
迭代器            | 弱一致性              | fail-fast
```

---

## 六、并发集合选择指南

### 6.1 Map选择

```
场景                           | 推荐集合
------------------------------|------------------------
通用并发Map                    | ConcurrentHashMap
需要排序                       | ConcurrentSkipListMap
读多写少                       | CopyOnWriteArrayList转Map
需要精确一致性                  | Collections.synchronizedMap
```

### 6.2 List选择

```
场景                           | 推荐集合
------------------------------|------------------------
读多写少                       | CopyOnWriteArrayList
需要实时一致性                  | Collections.synchronizedList
需要阻塞操作                    | LinkedBlockingQueue
```

### 6.3 Set选择

```
场景                           | 推荐集合
------------------------------|------------------------
通用并发Set                    | ConcurrentHashMap.newKeySet()
需要排序                       | ConcurrentSkipListSet
读多写少                       | CopyOnWriteArraySet
```

### 6.4 Queue选择

```
场景                           | 推荐集合
------------------------------|------------------------
生产者-消费者（有界）           | ArrayBlockingQueue
生产者-消费者（高吞吐）         | LinkedBlockingQueue
优先级队列                     | PriorityBlockingQueue
延迟任务                       | DelayQueue
直接交换                       | SynchronousQueue
非阻塞队列                     | ConcurrentLinkedQueue
```

---

## 七、性能优化建议

### 7.1 ConcurrentHashMap优化

```java
// 1. 初始容量设置
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>(1000);
// 避免扩容开销

// 2. 批量操作
// ✗ 不好
for (String key : keys) {
    map.put(key, 1);
}

// ✓ 更好（如果可能）
Map<String, Integer> batch = new HashMap<>();
for (String key : keys) {
    batch.put(key, 1);
}
map.putAll(batch); // 批量插入

// 3. 使用compute系列方法
// ✗ 不好（多次查找）
if (map.containsKey(key)) {
    map.put(key, map.get(key) + 1);
} else {
    map.put(key, 1);
}

// ✓ 更好（一次操作）
map.merge(key, 1, Integer::sum);
```

### 7.2 BlockingQueue优化

```java
// 1. 选择合适的容量
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(1000);
// 太小：生产者频繁阻塞
// 太大：内存占用高

// 2. 批量操作
List<Task> batch = new ArrayList<>(100);
queue.drainTo(batch, 100); // 批量取出，减少锁竞争

// 3. 避免无界队列
// ✗ 危险
BlockingQueue<Task> queue1 = new LinkedBlockingQueue<>(); // 无界，可能OOM

// ✓ 安全
BlockingQueue<Task> queue2 = new LinkedBlockingQueue<>(10000); // 有界
```

### 7.3 CopyOnWrite优化

```java
// 1. 批量写操作
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// ✗ 不好（多次复制）
for (String item : items) {
    list.add(item); // 每次add都复制整个数组
}

// ✓ 更好
list.addAll(items); // 只复制一次

// 2. 避免在写密集场景使用
// 如果写操作频繁，改用Collections.synchronizedList
```

---

## 八、常见陷阱与注意事项

### 8.1 ConcurrentHashMap陷阱

```java
// 1. 复合操作不是原子的
// ✗ 错误
if (!map.containsKey(key)) {
    map.put(key, value); // 竞态条件！
}

// ✓ 正确
map.putIfAbsent(key, value);

// 2. size()是估算值
int size = map.size(); // 不精确

// 3. 不允许null
map.put(null, value);  // NullPointerException
map.put(key, null);    // NullPointerException
```

### 8.2 BlockingQueue陷阱

```java
// 1. 无界队列OOM
LinkedBlockingQueue<Task> queue = new LinkedBlockingQueue<>();
// 如果生产速度 > 消费速度，会OOM

// 2. 中断处理
try {
    queue.take();
} catch (InterruptedException e) {
    // ✗ 吞掉异常
    e.printStackTrace();
}

// ✓ 正确处理
try {
    queue.take();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // 恢复中断状态
    return;
}
```

### 8.3 CopyOnWrite陷阱

```java
// 1. 内存占用翻倍
CopyOnWriteArrayList<byte[]> list = new CopyOnWriteArrayList<>();
list.add(new byte[1000000]); // 写操作会复制整个数组

// 2. 迭代器是快照
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("A");

Iterator<String> it = list.iterator(); // 快照
list.add("B"); // 修改

while (it.hasNext()) {
    System.out.println(it.next()); // 只输出A，看不到B
}
```

---

## 九、实战案例

### 案例1: 并发计数器

```java
public class ConcurrentCounter {
    private final ConcurrentHashMap<String, AtomicLong> counters
        = new ConcurrentHashMap<>();

    public void increment(String key) {
        counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public long get(String key) {
        AtomicLong counter = counters.get(key);
        return counter != null ? counter.get() : 0;
    }
}
```

### 案例2: 生产者-消费者

```java
public class ProducerConsumer {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);

    // 生产者
    class Producer implements Runnable {
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Task task = generateTask();
                    queue.put(task); // 队列满时阻塞
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 消费者
    class Consumer implements Runnable {
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Task task = queue.take(); // 队列空时阻塞
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 案例3: 事件监听器管理

```java
public class EventManager {
    private final CopyOnWriteArrayList<EventListener> listeners
        = new CopyOnWriteArrayList<>();

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void fireEvent(Event event) {
        // 迭代时不会抛ConcurrentModificationException
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
```

---

## 十、总结

### 核心要点

1. **选择合适的集合**
   - Map: ConcurrentHashMap（通用）、ConcurrentSkipListMap（有序）
   - Queue: LinkedBlockingQueue（高吞吐）、ArrayBlockingQueue（有界）
   - List: CopyOnWriteArrayList（读多写少）

2. **理解权衡**
   - 性能 vs 一致性
   - 内存 vs 吞吐量
   - 复杂度 vs 功能

3. **避免常见错误**
   - ConcurrentHashMap的复合操作
   - BlockingQueue的无界陷阱
   - CopyOnWrite的内存开销

4. **性能优化**
   - 设置合理的初始容量
   - 批量操作
   - 选择正确的队列实现

### 学习路径

1. ✅ 理解每种集合的特点
2. ✅ 动手实践Demo
3. ✅ 完成练习题
4. ✅ 对比性能差异
5. ✅ 在项目中应用

---

## 参考资料

- [Java并发编程实战](https://jcip.net/) - Brian Goetz
- [Doug Lea的并发编程](http://gee.cs.oswego.edu/dl/cpj/index.html)
- [Java并发包源码](https://github.com/openjdk/jdk/tree/master/src/java.base/share/classes/java/util/concurrent)
- [ConcurrentHashMap 1.8源码分析](https://www.ibm.com/developerworks/cn/java/java-lo-concurrenthashmap/)
