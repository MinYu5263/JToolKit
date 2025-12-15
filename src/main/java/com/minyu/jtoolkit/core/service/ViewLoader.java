package com.minyu.jtoolkit.core.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * FXML视图加载器，桥接Spring容器与JavaFX控制器，支持控制器注入Spring Bean
 */
@Component
public class ViewLoader {
    private final ApplicationContext context;

    public ViewLoader(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 加载指定的 FXML 文件 并自动注入 Spring Bean 到 Controller
     */
    public Parent load(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(new ClassPathResource(fxmlPath).getURL());
            // 使用 Spring 上下文来创建控制器
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("无法加载视图: " + fxmlPath, e);
        }
    }
}
