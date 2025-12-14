package com.minyu.jtoolkit;

import javafx.application.Application;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用物理入口 (Main Entry)
 * <p>
 * 职责：
 * 仅负责引导 JavaFX 运行时环境。
 * 实际的 Spring 容器初始化将在 {@link JfxRuntime#init()} 中触发。
 *
 * @see JfxRuntime
 */
@SpringBootApplication
@MapperScan("com.minyu.jtoolkit.system.mapper")
public class JToolKitApplication {
    public static void main(String[] args) {
        Application.launch(JfxRuntime.class, args);
    }
}
