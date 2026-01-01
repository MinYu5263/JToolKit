package com.minyu.jtoolkit.core.service;

import atlantafx.base.theme.*;
import com.jthemedetecor.OsThemeDetector;
import jakarta.annotation.PostConstruct;
import javafx.application.Application;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主题管理服务，封装AtlantaFX主题切换逻辑
 */
@Getter
@Service
public class ThemeManager {
    public static final String ID_SYSTEM = "SYSTEM";
    public static final int DEFAULT_FONT_SIZE = 14;

    private final Map<String, Theme> availableThemes = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化支持的主题列表
        availableThemes.put("Primer Light", new PrimerLight());
        availableThemes.put("Primer Dark", new PrimerDark());
        availableThemes.put("Nord Light", new NordLight());
        availableThemes.put("Nord Dark", new NordDark());
        availableThemes.put("Cupertino Light", new CupertinoLight());
        availableThemes.put("Cupertino Dark", new CupertinoDark());
        availableThemes.put("Dracula", new Dracula());
    }

    public void applyTheme(String themeName) {
        if (ID_SYSTEM.equals(themeName)) {
            applySystemTheme();
        } else {
            Theme theme = availableThemes.get(themeName);
            if (theme != null) {
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
        }
    }

    private void applySystemTheme() {
        boolean isSystemDark = OsThemeDetector.getDetector().isDark();
        Theme theme = isSystemDark ? new PrimerDark() : new PrimerLight();
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
    }
}
