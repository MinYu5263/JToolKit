# JToolKit 开发手册

本文档面向希望维护或扩展 JToolKit 的开发者，说明项目架构、模块约定、运行打包流程和常见开发注意事项。

## 架构概览

JToolKit 是一个 JavaFX 桌面应用，同时使用 Spring Boot 管理依赖注入、配置和服务生命周期。

启动入口是 `com.minyu.jtoolkit.JToolKitApplication`：

```java
@SpringBootApplication
public class JToolKitApplication {
    public static void main(String[] args) {
        Application.launch(JfxRuntime.class, args);
    }
}
```

核心关系：

- `JToolKitApplication` 启动 JavaFX 运行时。
- `JfxRuntime` 负责衔接 JavaFX 生命周期和 Spring 容器。
- `ViewLoader` 使用 Spring `ApplicationContext` 创建 FXML Controller，因此 Controller 可以使用 Spring 注入。
- `MainModel` 定义侧边栏菜单、底部菜单和搜索索引。
- 每个功能模块通常由 FXML、Controller、PersistentState 三部分组成。

## 目录约定

```text
src/main/java/com/minyu/jtoolkit/
├── core/
│   ├── component/     # 通用 JavaFX 组件
│   ├── config/        # 核心配置
│   ├── event/         # 应用事件
│   ├── listener/      # 启动和窗口初始化监听
│   ├── model/         # 通用模型
│   ├── service/       # 主题、字体、快捷键、视图加载等核心服务
│   └── util/          # 工具类
├── module/
│   ├── main/          # 主界面、导航、搜索
│   ├── settings/      # 设置页
│   ├── about/         # 关于页
│   └── */             # 具体工具模块
└── system/
    └── service/       # Git、环境变量、本地存储等系统服务
```

资源目录：

```text
src/main/resources/
├── fxml/              # FXML 视图
├── styles/            # CSS 样式
├── images/            # 图标资源
├── fonts/             # 字体资源
└── application.yml    # 应用默认配置
```

## 模块开发流程

下面以新增 `demo_tool` 模块为例。

### 1. 创建状态模型

如果模块需要自动保存用户输入，创建一个实现 `PersistentState` 的状态类：

```java
package com.minyu.jtoolkit.module.demo_tool;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

@Data
public class DemoToolPersistentState implements PersistentState {
    private String input;
}
```

如果模块没有状态，也可以不创建状态类，并按实际情况选择普通 Controller。

### 2. 创建 Controller

有状态模块建议继承 `BaseController<T>`：

```java
package com.minyu.jtoolkit.module.demo_tool;

import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoToolController extends BaseController<DemoToolPersistentState> {
    @FXML
    private TextArea inputArea;

    @Override
    protected void restoreValues(DemoToolPersistentState state) {
        inputArea.setText(state.getInput());
    }

    @Override
    protected DemoToolPersistentState captureValues() {
        DemoToolPersistentState state = new DemoToolPersistentState();
        state.setInput(inputArea.getText());
        return state;
    }

    @Override
    protected String getViewKey() {
        return "demo_tool";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(inputArea.textProperty());
    }
}
```

`BaseController` 会在初始化时执行：

1. `initView()`
2. `loadStateOrDefaults()`
3. `registerAutoSave()`

当 `getObservables()` 中的属性变化时，Controller 会延迟约 1 秒自动保存状态。

### 3. 创建 FXML

FXML 文件放在：

```text
src/main/resources/fxml/demo_tool/DemoToolView.fxml
```

示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.TextArea?>
<?import javafx.scene.layout.VBox?>

<VBox xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.minyu.jtoolkit.module.demo_tool.DemoToolController"
      spacing="8">
    <TextArea fx:id="inputArea" promptText="输入内容" VBox.vgrow="ALWAYS"/>
</VBox>
```

Controller 由 Spring 创建，因此 Controller 类需要标记为 `@Component`。

### 4. 注册菜单

在 `MainModel#createSideTree()` 中添加导航项：

```java
item("Demo 工具", "fxml/demo_tool/DemoToolView.fxml")
```

如果是新的分类，使用：

```java
TreeItem<Nav> demo = group("示例工具", Material2AL.DEVELOPER_BOARD);
demo.getChildren().setAll(List.of(
        item("Demo 工具", "fxml/demo_tool/DemoToolView.fxml")
));
```

菜单注册后，搜索索引会通过 `refreshSearchIndex()` 自动更新。

### 5. 添加样式

模块样式建议放在：

```text
src/main/resources/styles/modules/demo-tool.css
```

如果样式需要全局加载，在公共样式入口中引入或参考现有模块样式的加载方式。

## 状态持久化

状态持久化由 `StorageService` 负责，模块只需要提供：

- `getViewKey()`：当前模块的唯一存储 key。
- `captureValues()`：从界面采集状态。
- `restoreValues()`：把状态恢复到界面。
- `getObservables()`：声明哪些属性变化后触发自动保存。

注意事项：

- `getViewKey()` 应保持稳定，修改后会导致旧配置无法自动读取。
- 状态类应尽量只保存可序列化的简单数据。
- 不要把密码、令牌等敏感信息默认持久化，除非明确经过加密或用户确认。

## FXML 与 Spring 注入

`ViewLoader` 通过下面的方式加载视图：

```java
FXMLLoader loader = new FXMLLoader(new ClassPathResource(fxmlPath).getURL());
loader.setControllerFactory(context::getBean);
return loader.load();
```

这意味着：

- FXML 中的 `fx:controller` 必须对应一个 Spring Bean。
- Controller 可以通过构造器或字段注入项目中的 Service。
- 如果 Controller 没有被 Spring 扫描到，FXML 加载会失败。

## 应用配置

默认配置位于 `src/main/resources/application.yml`：

```yaml
jtoolkit:
  title: "JToolKit"
  width: 1280
  height: 768
  theme: SYSTEM
  font-size: 14

spring:
  application:
    name: JToolKit
  main:
    lazy-initialization: true
```

`AppConfigManager` 会加载本地配置；不存在时使用 `application.yml` 的默认值，并注册默认搜索快捷键 `Shortcut+Shift+F`。

## 主题、字体和快捷键

相关核心服务：

- `ThemeManager`：应用主题。
- `FontManager`：应用字体大小。
- `HotKeyManager`：管理快捷键。
- `AppConfigManager`：统一读写应用配置。

设置页应优先通过 `AppConfigManager` 修改配置，不要绕过它直接操作主题、字体或快捷键服务。

## 构建和运行

常用命令：

```bash
mvn clean package
mvn -DskipTests package
java -jar target/JToolKit.jar
```

IDE 运行入口：

```text
com.minyu.jtoolkit.JToolKitApplication
```

## jpackage 打包

打包流程：

1. 执行 `mvn -DskipTests package`。
2. Maven 会生成 `target/JToolKit.jar`。
3. `maven-antrun-plugin` 会复制纯净输入到 `target/pure/JToolKit.jar`。
4. `jpackage` 读取 `package-app-image.txt`，从 `target/pure` 创建应用镜像。
5. 产物输出到 `dist/`。

公共参数：

```text
--name "JToolKit"
--type app-image
--input target/pure
--main-jar JToolKit.jar
--add-modules ALL-MODULE-PATH
--dest dist
```

Windows 使用 `build_win.bat`，macOS 使用 `build_mac.sh`。两者都依赖 `PATH_TO_FX_MODS` 指向 JavaFX JMODS 目录。

## 代码风格建议

- 使用 UTF-8 编码保存源码和资源文件。
- Controller 命名使用 `<ModuleName>Controller`。
- 状态类命名使用 `<ModuleName>PersistentState`。
- FXML 命名使用 `<ModuleName>View.fxml`。
- 模块目录使用小写下划线，例如 `text_formatter`。
- 新功能优先复用 `core/component` 中已有组件。
- 业务逻辑较多时，把系统交互或可复用逻辑放入 `system/service` 或合适的核心服务中。

## 提交 PR 前检查

建议在提交前完成：

```bash
mvn -DskipTests package
```

人工检查：

- 新模块能在侧边栏打开。
- FXML 没有加载异常。
- 状态恢复和自动保存符合预期。
- 设置项不会破坏主题、字体和快捷键。
- 涉及系统功能时，在对应平台上验证行为。

## 常见问题

### FXML 加载失败

检查：

- `fx:controller` 包名是否正确。
- Controller 是否添加了 `@Component`。
- FXML 路径是否与 `MainModel` 中注册的路径一致。
- FXML 中的 `fx:id` 是否能在 Controller 中找到对应字段。

### 模块状态没有保存

检查：

- Controller 是否继承 `BaseController<T>`。
- `getViewKey()` 是否返回非空且稳定的 key。
- `captureValues()` 是否返回状态对象。
- `getObservables()` 是否包含需要监听的 JavaFX 属性。

### jpackage 找不到 JavaFX 模块

检查：

- `JAVA_HOME` 是否指向 JDK 21。
- `PATH_TO_FX_MODS` 是否指向 JavaFX JMODS 目录。
- Windows 路径分隔符和 macOS 路径分隔符是否正确。

