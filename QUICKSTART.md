# 快速开始指南

## 项目结构

```
concurrency-exercises/
├── pom.xml                     # Maven配置文件
├── README.md                   # 项目总览
├── QUICKSTART.md              # 快速开始指南（本文件）
├── .gitignore                 # Git忽略配置
│
├── docs/                      # 📚 学习文档
│   ├── Java并发学习路线图.md
│   ├── 并发基础理论.md
│   ├── Java内存模型.md
│   └── 常见并发问题诊断.md
│
├── module-01-thread-basics/   # 📦 Module 01: 线程基础
│   ├── README.md              # 模块说明
│   └── tutorial.md            # 教程文档
│
└── src/                       # 💻 源代码
    ├── main/java/com/concurrency/basics/
    │   ├── demo/              # 演示代码
    │   │   ├── D01_ThreadCreation.java
    │   │   ├── D02_ThreadLifecycle.java
    │   │   └── D03_ThreadInterrupt.java
    │   ├── exercises/         # 练习题
    │   │   ├── E01_MultiThreadDownloader.java
    │   │   └── E02_ThreadCoordination.java
    │   └── solutions/         # 参考答案
    │       ├── S01_MultiThreadDownloader.java
    │       └── S02_ThreadCoordination.java
    └── test/java/com/concurrency/basics/
        └── ThreadBasicsTest.java
```

---

## 环境准备

### 1. 检查JDK版本
```bash
java -version
# 需要 JDK 17 或更高版本
```

### 2. 检查Maven
```bash
mvn -version
# 需要 Maven 3.6+
```

---

## 编译与运行

### 1. 编译项目
```bash
mvn clean compile
```

### 2. 运行测试
```bash
# 运行所有测试
mvn test

# 运行指定测试
mvn test -Dtest=ThreadBasicsTest
```

### 3. 运行Demo示例

#### Demo 01: 线程创建的4种方式
```bash
mvn exec:java -Dexec.mainClass="com.concurrency.basics.demo.D01_ThreadCreation"
```

**输出示例**：
```
=== 线程创建演示 ===

--- 方式1: 继承Thread类 ---
MyThread执行: Worker-1
  线程ID: 26
  是否存活: true

--- 方式2: 实现Runnable接口 ---
MyRunnable执行: Worker-2 - 任务: 任务A
...
```

#### Demo 02: 线程生命周期与状态
```bash
mvn exec:java -Dexec.mainClass="com.concurrency.basics.demo.D02_ThreadLifecycle"
```

#### Demo 03: 线程中断机制
```bash
mvn exec:java -Dexec.mainClass="com.concurrency.basics.demo.D03_ThreadInterrupt"
```

---

## 学习路径

### Step 1: 阅读理论文档（30分钟）
1. 📖 [并发基础理论](docs/并发基础理论.md) - 理解什么是并发
2. 📖 [Java并发学习路线图](docs/Java并发学习路线图.md) - 规划学习路径

### Step 2: 学习Module 01（2-3小时）
1. 📄 阅读 [tutorial.md](module-01-thread-basics/tutorial.md)
2. 💻 运行3个Demo（D01, D02, D03）
3. ✏️ 完成2个练习（E01, E02）
4. ✅ 对比参考答案（S01, S02）

### Step 3: 运行测试验证
```bash
mvn test -Dtest=ThreadBasicsTest
```

### Step 4: 深入学习（可选）
1. 📖 [Java内存模型](docs/Java内存模型.md)
2. 📖 [常见并发问题诊断](docs/常见并发问题诊断.md)

---

## 练习题指南

### 练习1: 多线程文件下载器 🟢
**文件**: `E01_MultiThreadDownloader.java`

**目标**:
- 实现多线程并发下载
- 使用join()等待所有线程完成
- 合并下载的文件块

**提示**:
1. 计算每个线程的下载范围
2. 创建并启动所有线程
3. 使用join()等待
4. 合并结果

**参考答案**: `S01_MultiThreadDownloader.java`

### 练习2: 线程协调与顺序执行 🟡
**文件**: `E02_ThreadCoordination.java`

**目标**:
- 控制3个线程按顺序执行
- 输出: First → Second → Third

**方法**:
- 方式1: 使用join()
- 方式2: 使用共享标志位

**参考答案**: `S02_ThreadCoordination.java`（提供4种实现方式）

---

## 常用命令

```bash
# 清理编译
mvn clean

# 编译
mvn compile

# 编译 + 测试
mvn test

# 运行Demo（替换mainClass）
mvn exec:java -Dexec.mainClass="完整类名"

# 只显示输出（安静模式）
mvn exec:java -Dexec.mainClass="类名" -q

# 运行指定测试
mvn test -Dtest=测试类名
```

---

## IDE配置

### IntelliJ IDEA
1. **导入项目**: File → Open → 选择pom.xml
2. **运行Demo**: 右键Java文件 → Run
3. **运行测试**: 右键测试类 → Run
4. **调试**: 设置断点 → Debug

### Eclipse
1. **导入项目**: File → Import → Maven → Existing Maven Projects
2. **运行**: 右键 → Run As → Java Application
3. **测试**: 右键 → Run As → JUnit Test

### VS Code
1. **安装插件**:
   - Java Extension Pack
   - Maven for Java
2. **运行**: 点击类上方的"Run"按钮

---

## 验证学习成果

### ✅ 检查清单

**理论知识**:
- [ ] 理解线程与进程的区别
- [ ] 掌握线程的6种状态
- [ ] 理解happens-before规则
- [ ] 知道线程安全的三大特性

**实践能力**:
- [ ] 能创建并启动线程
- [ ] 理解join()的作用
- [ ] 会使用中断机制
- [ ] 能控制线程执行顺序

**测试通过**:
- [ ] ThreadBasicsTest 全部通过（9个测试）
- [ ] 成功运行3个Demo
- [ ] 完成2个练习题

---

## 常见问题

**Q: Maven编译报错？**
```bash
# 1. 检查JDK版本
java -version

# 2. 清理重新编译
mvn clean compile

# 3. 更新依赖
mvn dependency:resolve
```

**Q: Demo运行没有输出？**
```bash
# 使用安静模式
mvn exec:java -Dexec.mainClass="类名" -q
```

**Q: 如何在IDE中运行？**
- 找到Demo类（如D01_ThreadCreation.java）
- 右键 → Run 'D01_ThreadCreation.main()'

**Q: 练习题不知道怎么做？**
1. 先阅读tutorial.md
2. 运行对应的Demo
3. 查看题目中的TODO和提示
4. 参考solutions目录的答案

---

## 下一步

完成Module 01后，继续学习：

👉 **Module 02: 同步机制** (开发中)
- synchronized关键字
- wait/notify机制
- 死锁分析与预防

---

## 获取帮助

- 📧 提交Issue反馈问题
- 💬 查看FAQ文档
- 📚 阅读推荐书籍：《Java并发编程实战》

**开始你的并发学习之旅！** 🚀
