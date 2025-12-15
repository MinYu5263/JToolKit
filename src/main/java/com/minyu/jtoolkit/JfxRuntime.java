package com.minyu.jtoolkit;

import com.minyu.jtoolkit.core.event.StageReadyEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX运行时代理，管理Spring Boot上下文生命周期，发布StageReadyEvent
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
