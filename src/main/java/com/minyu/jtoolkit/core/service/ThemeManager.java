package com.minyu.jtoolkit.core.service;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 主题管理服务，封装AtlantaFX主题切换逻辑
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
