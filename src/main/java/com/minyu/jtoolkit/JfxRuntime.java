package com.minyu.jtoolkit;

import com.minyu.jtoolkit.core.event.StageReadyEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX 运行时代理
 * <p>
 * 职责：
 * 1. 管理 Spring Boot 上下文的生命周期（启动/关闭）。
 * 2. 将 JavaFX 的 {@link Stage} 通过事件机制发布给 Spring 容器。
 * <p>
 * 注意：此类不应包含任何业务逻辑。
 */
public class JfxRuntime extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder()
                .sources(JToolKitApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }
}
