# JToolKit

JToolKit 是一个基于 JavaFX 和 Spring Boot 构建的桌面开发工具箱，面向日常开发、配置处理、文本处理和系统辅助场景。项目把常用的小工具集中在一个本地应用里，尽量减少在网页工具、脚本和临时命令之间来回切换。

> 当前项目仍在持续迭代中，欢迎提交 Issue、建议和 Pull Request。

## 功能特性

### 生成器

- Cron 生成器：辅助编写和查看 Cron 表达式。
- 文件树生成：根据目录生成文件树文本。
- 密码生成器：生成开发和日常使用的随机密码。

### 转换工具

- Excel > SQL：将 Excel 数据转换为 SQL。
- SQL > Excel：连接数据库并导出查询结果。
- Properties <> YAML：在 Properties 和 YAML 配置格式之间转换。
- 数字进制：常用数字进制转换。

### 文本工具

- 文本格式化：整理、格式化常见文本内容。
- 正则表达式测试：验证正则表达式匹配结果。
- 文本分析与实用工具：提供文本统计、处理等辅助能力。

### 系统工具

- JAR 安装：辅助安装本地 JAR 到 Maven 仓库。
- Git 配置助手：查看和维护 Git 配置。
- 环境变量助手：查看和维护系统环境变量。

### 应用能力

- JavaFX 桌面界面。
- Spring Boot 负责依赖注入和应用配置。
- Atlantafx 主题样式和 Ikonli 图标。
- 支持主题、字体大小、快捷键等本地配置。
- 模块状态自动保存，减少重复输入。

## 技术栈

- Java 21
- Maven
- JavaFX 21
- Spring Boot 3.5
- Atlantafx
- Ikonli
- RichTextFX
- Fastjson2
- Jackson YAML / Properties
- Fastexcel
- MySQL Connector/J
- PostgreSQL JDBC Driver
- JNA

## 环境要求

开发环境建议：

- JDK 21
- Maven 3.9 或更新版本
- JavaFX JMODS 21，打包桌面应用时需要

确认环境：

```bash
java -version
mvn -version
```

## 快速开始

获取源码后进入项目目录：

```bash
cd JToolKit
```

构建项目：

```bash
mvn clean package
```

跳过测试构建：

```bash
mvn -DskipTests package
```

运行生成的 Spring Boot 可执行 JAR：

```bash
java -jar target/JToolKit.jar
```

也可以直接在 IDE 中运行：

```text
com.minyu.jtoolkit.JToolKitApplication
```

## 打包桌面应用

项目使用 `jpackage` 生成桌面应用镜像，公共参数位于 `package-app-image.txt`。

打包前先构建 JAR：

```bash
mvn -DskipTests package
```

### Windows

准备 JavaFX JMODS，并设置环境变量：

```bat
set PATH_TO_FX_MODS=D:\DevTools\JavaFX\javafx-jmods-21
```

执行：

```bat
build_win.bat
```

### macOS

准备 JavaFX JMODS，并设置环境变量：

```bash
export PATH_TO_FX_MODS=/path/to/javafx-jmods-21
```

执行：

```bash
chmod +x build_mac.sh
./build_mac.sh
```

打包产物默认输出到 `dist/`。

## 项目结构

```text
.
├── pom.xml
├── package-app-image.txt
├── build_win.bat
├── build_mac.sh
├── docs/
│   └── development.md
└── src/
    └── main/
        ├── java/com/minyu/jtoolkit/
        │   ├── core/          # 通用组件、服务、事件和工具类
        │   ├── module/        # 各功能模块 Controller 和状态模型
        │   ├── system/        # 系统能力封装
        │   ├── JfxRuntime.java
        │   └── JToolKitApplication.java
        └── resources/
            ├── fxml/          # JavaFX FXML 视图
            ├── styles/        # CSS 样式
            ├── images/        # 应用图标
            ├── fonts/         # 字体资源
            └── application.yml
```

## 开发文档

如果你想了解架构、模块开发流程、状态持久化和打包细节，请阅读：

- [开发手册](docs/development.md)

## 贡献指南

欢迎通过 Issue 或 Pull Request 参与项目。

建议流程：

1. Fork 本仓库。
2. 创建功能分支，例如：`git checkout -b feature/add-demo-tool`。
3. 提交修改，并尽量保持单次提交聚焦。
4. 执行构建确认没有破坏主流程：`mvn -DskipTests package`。
5. 发起 Pull Request，并说明改动内容、验证方式和相关截图。

提交代码时请尽量遵循现有项目风格：

- Java 源码使用 UTF-8 编码。
- 新模块放在 `src/main/java/com/minyu/jtoolkit/module/<module_name>/`。
- 新视图放在 `src/main/resources/fxml/<module_name>/`。
- 样式优先复用 `src/main/resources/styles/` 下的公共样式。
- 有本地状态的模块继承 `BaseController<T extends PersistentState>`。

## 许可证

当前仓库尚未声明开源许可证。正式开源前建议添加 `LICENSE` 文件，例如 MIT、Apache-2.0 或 GPL-3.0，并在本节补充对应说明。
