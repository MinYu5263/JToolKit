package com.minyu.jtoolkit.core.service;

import atlantafx.base.theme.*;
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
    private final Map<String, Theme> availableThemes = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        availableThemes.put("Primer Light", new PrimerLight()); // 浅色
        availableThemes.put("Primer Dark", new PrimerDark()); // 深色
        availableThemes.put("Nord Light", new NordLight()); // 北欧浅色
        availableThemes.put("Nord Dark", new NordDark()); // 北欧深色
        availableThemes.put("Cupertino Light", new CupertinoLight());// 苹果浅色
        availableThemes.put("Cupertino Dark", new CupertinoDark()); // 苹果深色
        availableThemes.put("Dracula", new Dracula()); // 吸血鬼
    }

    public void applyTheme(String themeName) {
        Theme theme = availableThemes.get(themeName);
        if (theme != null) {
            Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
        }
    }

    public void applyTheme(Theme theme) {
        if (theme != null) {
            Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
        }
    }
}
