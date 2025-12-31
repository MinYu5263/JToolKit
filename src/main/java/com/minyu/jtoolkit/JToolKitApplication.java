package com.minyu.jtoolkit;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口类，启动JavaFX运行时，Spring容器初始化由{@link JfxRuntime}处理
 */
@SpringBootApplication
public class JToolKitApplication {
    public static void main(String[] args) {
        Application.launch(JfxRuntime.class, args);
    }
}
