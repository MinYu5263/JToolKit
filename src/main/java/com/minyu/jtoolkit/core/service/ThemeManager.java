package com.minyu.jtoolkit.core.service;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 主题管理服务
 * <p>
 * 负责封装 AtlantaFX 主题库的调用逻辑，提供统一的主题切换接口。
 * 支持通过配置文件或运行时动态切换亮色/暗色模式。
 */
@Service
public class ThemeManager {
    public void applyTheme(String themeName) {
        if (Objects.equals(themeName, "light")) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }
    }
}
