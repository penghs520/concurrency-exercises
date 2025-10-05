# 线程基础教程

## 一、什么是线程？

### 1.1 进程 vs 线程

**进程（Process）**
- 操作系统资源分配的基本单位
- 拥有独立的内存空间
- 进程间通信复杂（IPC）

**线程（Thread）**
- CPU调度的基本单位
- 共享进程的内存空间
- 线程间通信简单（共享内存）
- 轻量级，创建/销毁成本低

### 1.2 为什么使用多线程？

1. **提高CPU利用率**：I/O阻塞时其他线程继续执行
2. **提升响应速度**：UI线程不阻塞
3. **充分利用多核**：并行计算提高性能
4. **模拟真实世界**：同时处理多个任务

---

## 二、线程的创建

### 方式1：继承Thread类

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
}

// 使用
MyThread thread = new MyThread();
thread.start(); // 启动线程
```

**优点**：简单直观
**缺点**：Java单继承限制

### 方式2：实现Runnable接口（推荐）

```java
public class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
}

// 使用
Thread thread = new Thread(new MyRunnable());
thread.start();
```

**优点**：避免单继承限制，更灵活
**推荐**：优先使用此方式

### 方式3：Lambda表达式（Java 8+）

```java
Thread thread = new Thread(() -> {
    System.out.println("Lambda线程：" + Thread.currentThread().getName());
});
thread.start();
```

**优点**：代码简洁
**适用**：简单的线程任务

### 方式4：实现Callable接口（有返回值）

```java
Callable<Integer> task = () -> {
    return 42; // 返回计算结果
};

FutureTask<Integer> futureTask = new FutureTask<>(task);
Thread thread = new Thread(futureTask);
thread.start();

Integer result = futureTask.get(); // 获取结果（阻塞）
```

**优点**：可以返回结果，抛出异常
**适用**：需要获取线程执行结果

---

## 三、线程的生命周期

### 3.1 线程状态

Java线程有6种状态（`Thread.State`枚举）：

```
NEW (新建)
  ↓ start()
RUNNABLE (就绪/运行)
  ↓ 等待锁
BLOCKED (阻塞)
  ↓ 获取锁/wait()/join()
WAITING (等待)
  ↓ sleep()/wait(timeout)/join(timeout)
TIMED_WAITING (限时等待)
  ↓ 执行完成
TERMINATED (终止)
```

### 3.2 状态详解

#### 1. NEW（新建）
```java
Thread t = new Thread(() -> {});
// 此时 t.getState() == Thread.State.NEW
```
线程对象创建，但未调用`start()`

#### 2. RUNNABLE（可运行）
```java
t.start();
// 此时 t.getState() == Thread.State.RUNNABLE
```
- 包含两个子状态：
  - **Ready**：就绪，等待CPU调度
  - **Running**：运行中

#### 3. BLOCKED（阻塞）
```java
synchronized (lock) {
    // 其他线程持有锁时，当前线程进入BLOCKED
}
```
等待获取监视器锁（synchronized）

#### 4. WAITING（等待）
```java
// 方式1: Object.wait()
synchronized (lock) {
    lock.wait(); // 进入WAITING
}

// 方式2: Thread.join()
otherThread.join(); // 等待otherThread结束

// 方式3: LockSupport.park()
LockSupport.park(); // 进入WAITING
```
无限期等待其他线程的特定操作

#### 5. TIMED_WAITING（限时等待）
```java
// 方式1: Thread.sleep()
Thread.sleep(1000); // 休眠1秒

// 方式2: Object.wait(timeout)
synchronized (lock) {
    lock.wait(1000);
}

// 方式3: Thread.join(timeout)
otherThread.join(1000);
```
有时间限制的等待

#### 6. TERMINATED（终止）
```java
// run()方法执行完成
// 或抛出未捕获异常
```
线程执行结束

### 3.3 状态转换图

```
          start()
NEW -----------------> RUNNABLE
                          |  ↑
                          |  | 获取锁/notify()/时间到
                          ↓  |
          等待锁      BLOCKED

          wait()/join()
RUNNABLE -------------> WAITING
          <-------------
           notify()/中断

          sleep()/wait(timeout)
RUNNABLE -------------> TIMED_WAITING
          <-------------
              时间到/中断

          run()完成
RUNNABLE -------------> TERMINATED
```

---

## 四、线程的核心方法

### 4.1 启动与运行

#### start() vs run()

```java
Thread t = new Thread(() -> {
    System.out.println("Hello from: " + Thread.currentThread().getName());
});

t.start(); // ✓ 启动新线程执行run()
// 输出: Hello from: Thread-0

t.run();   // ✗ 只是普通方法调用，不会创建新线程
// 输出: Hello from: main
```

**关键区别**：
- `start()`: 启动新线程，JVM调用`run()`
- `run()`: 当前线程直接执行，不创建新线程

### 4.2 线程休眠

```java
// 休眠1秒（1000毫秒）
Thread.sleep(1000);

// 或使用TimeUnit（更易读）
TimeUnit.SECONDS.sleep(1);
TimeUnit.MILLISECONDS.sleep(500);
```

**特点**：
- 不释放锁
- 可被中断（抛出`InterruptedException`）
- 时间到后自动唤醒

### 4.3 线程让步

```java
Thread.yield(); // 提示CPU可以切换到其他线程
```

**特点**：
- 只是"建议"，CPU可以忽略
- 不释放锁
- 让出CPU给**同优先级或更高优先级**的线程

### 4.4 线程等待

```java
Thread t = new Thread(() -> {
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

t.start();
t.join(); // 等待t线程执行完成
System.out.println("t线程已结束");

// 或设置超时
t.join(1000); // 最多等待1秒
```

**使用场景**：主线程等待子线程完成

---

## 五、线程中断机制

### 5.1 什么是中断？

中断是一种**协作机制**，用于优雅地停止线程。

```java
Thread t = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
    System.out.println("线程被中断，准备退出");
});

t.start();
Thread.sleep(100);
t.interrupt(); // 请求中断
```

### 5.2 中断相关方法

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `interrupt()` | 请求中断线程 | void |
| `isInterrupted()` | 检查是否被中断 | boolean（不清除中断标志） |
| `interrupted()` | 检查并清除中断标志 | boolean（静态方法） |

### 5.3 响应中断的两种方式

#### 方式1：检查中断标志
```java
public void run() {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
    // 清理资源
}
```

#### 方式2：捕获InterruptedException
```java
public void run() {
    try {
        while (true) {
            // 执行任务
            Thread.sleep(100); // 阻塞方法会抛出异常
        }
    } catch (InterruptedException e) {
        // 收到中断请求
        Thread.currentThread().interrupt(); // 重新设置中断标志
    }
    // 清理资源
}
```

### 5.4 中断的最佳实践

**✓ 好的做法**：
```java
public void run() {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            doWork();
        }
    } catch (InterruptedException e) {
        // 恢复中断状态
        Thread.currentThread().interrupt();
    } finally {
        // 清理资源
        cleanup();
    }
}
```

**✗ 不好的做法**：
```java
// 1. 吞掉异常
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // 什么都不做 [错误！]
}

// 2. 使用stop()（已废弃）
thread.stop(); // 危险！可能导致数据不一致
```

---

## 六、守护线程

### 6.1 概念

**用户线程（User Thread）**：
- JVM会等待所有用户线程结束才退出

**守护线程（Daemon Thread）**：
- 后台服务线程
- 所有用户线程结束时，JVM自动终止守护线程

### 6.2 使用示例

```java
Thread daemon = new Thread(() -> {
    while (true) {
        System.out.println("守护线程运行中...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            break;
        }
    }
});

daemon.setDaemon(true); // 必须在start()之前设置
daemon.start();

// 主线程结束后，守护线程自动终止
Thread.sleep(3000);
System.out.println("主线程结束");
```

### 6.3 典型应用

- GC线程（垃圾回收）
- 监控线程
- 日志记录线程

**注意**：守护线程不能用于执行重要的业务逻辑（可能被突然终止）

---

## 七、线程优先级

### 7.1 设置优先级

```java
Thread t = new Thread(() -> {});

t.setPriority(Thread.MIN_PRIORITY);  // 1（最低）
t.setPriority(Thread.NORM_PRIORITY); // 5（默认）
t.setPriority(Thread.MAX_PRIORITY);  // 10（最高）
```

### 7.2 注意事项

- 优先级只是"建议"，不保证执行顺序
- 依赖操作系统的线程调度
- **不要依赖优先级来保证程序正确性**

---

## 八、线程异常处理

### 8.1 未捕获异常处理器

```java
Thread t = new Thread(() -> {
    throw new RuntimeException("线程异常");
});

// 设置异常处理器
t.setUncaughtExceptionHandler((thread, throwable) -> {
    System.err.println("线程 " + thread.getName() + " 抛出异常: " + throwable.getMessage());
});

t.start();
```

### 8.2 全局默认处理器

```java
Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
    System.err.println("全局处理器捕获: " + throwable.getMessage());
});
```

---

## 九、最佳实践

### 1. 优先使用Runnable
```java
// ✓ 好
Thread t = new Thread(() -> { /* 任务 */ });

// ✗ 不推荐（除非需要重写Thread方法）
class MyThread extends Thread { /* ... */ }
```

### 2. 给线程命名
```java
Thread t = new Thread(() -> { /* ... */ }, "MyWorkerThread");
// 便于调试和日志分析
```

### 3. 优雅关闭线程
```java
// ✓ 使用中断
thread.interrupt();

// ✗ 强制停止（已废弃）
thread.stop();
```

### 4. 处理InterruptedException
```java
// ✓ 恢复中断状态
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    // 清理资源
}

// ✗ 吞掉异常
catch (InterruptedException e) {
    // 空catch块
}
```

### 5. 避免共享可变状态
```java
// ✓ 使用ThreadLocal或不可变对象
ThreadLocal<SimpleDateFormat> dateFormat =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// ✗ 共享可变对象
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 非线程安全
```

---

## 十、常见面试题

**Q1: start()可以调用多次吗？**
A: 不可以，会抛出`IllegalThreadStateException`。线程只能启动一次。

**Q2: 如何让线程按顺序执行？**
A: 使用`join()`方法：
```java
t1.start();
t1.join();
t2.start();
t2.join();
// t1执行完 → t2执行完
```

**Q3: sleep()和wait()的区别？**
A:
- `sleep()`: 不释放锁，Thread类方法，自动唤醒
- `wait()`: 释放锁，Object类方法，需要notify()唤醒

**Q4: 为什么不建议使用stop()？**
A: `stop()`会立即终止线程，可能导致：
- 资源未释放（锁未释放）
- 数据不一致（事务未完成）
- 对象状态破坏

**Q5: 如何实现线程安全的单例？**
A: 使用静态内部类或双重检查锁定（后续模块学习）

---

## 十一、实战技巧

### 1. 线程调试

```java
// 打印当前线程信息
System.out.println("线程: " + Thread.currentThread().getName());
System.out.println("状态: " + Thread.currentThread().getState());
System.out.println("优先级: " + Thread.currentThread().getPriority());

// 查看所有活动线程
Thread.getAllStackTraces().keySet().forEach(t -> {
    System.out.println(t.getName() + " - " + t.getState());
});
```

### 2. 使用线程池（预习）

```java
// 不要直接创建大量线程
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> { /* 任务 */ });
executor.shutdown();
```

### 3. 监控线程性能

```bash
# jstack查看线程堆栈
jstack <pid>

# jconsole可视化监控
jconsole
```

---

## 总结

本模块学习了：
- ✅ 线程的4种创建方式
- ✅ 线程的6种状态及转换
- ✅ 核心方法：start/sleep/join/interrupt
- ✅ 中断机制的正确使用
- ✅ 守护线程的应用场景

**下一步**：学习线程同步机制（synchronized、wait/notify）

---

## 参考资料

- [Java Thread官方文档](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Thread.html)
- 《Java并发编程实战》第5章
- [Java线程状态详解](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Thread.State.html)
