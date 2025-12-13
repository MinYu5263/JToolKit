package com.minyu.jtoolkit.module.theme;

import com.minyu.jtoolkit.core.service.ThemeManager;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

/**
 * ThemeController
 */
@Component
public class ThemeController {
    private final ThemeManager themeManager;

    // 构造器注入主题服务
    public ThemeController(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    @FXML
    public void onDarkMode() {
        themeManager.applyTheme("dark");
    }

    @FXML
    public void onLightMode() {
        themeManager.applyTheme("light");
    }
}
