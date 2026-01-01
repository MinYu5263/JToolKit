package com.minyu.jtoolkit.core.service;

import com.minyu.jtoolkit.system.service.ViewDataService;
import jakarta.annotation.PostConstruct;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AppConfigManager
 */
@Slf4j
@Service
public class AppConfigManager {
    private static final String STORAGE_KEY = "app_config";

    @Value("${jtoolkit.theme:SYSTEM}") // 如果yml没配，默认SYSTEM
    private String defaultTheme;

    @Value("${jtoolkit.font-size:14}") // 如果yml没配，默认14
    private Integer defaultFontSize;

    private final ViewDataService viewDataService;
    private final ThemeManager themeManager;
    private final HotKeyManager hotKeyManager;
    private final FontManager fontManager;
    // private final ConfigRepository repository; // 假设你有存取文件的类

    @Getter
    private AppConfig currentConfig;

    public AppConfigManager(ViewDataService viewDataService,
                            ThemeManager themeManager,
                            HotKeyManager hotKeyManager,
                            FontManager fontManager) {
        this.viewDataService = viewDataService;
        this.themeManager = themeManager;
        this.hotKeyManager = hotKeyManager;
        this.fontManager = fontManager;
    }

    @PostConstruct
    public void initAppConfig() {
        log.info("Initializing application configuration...");

        currentConfig = viewDataService.loadState(STORAGE_KEY, AppConfig.class);
        // 没有数据则设置默认值
        if (currentConfig == null) {
            currentConfig = new AppConfig();
            currentConfig.setThemeId(defaultTheme);
            currentConfig.setFontSize(defaultFontSize);
            currentConfig.getShortcuts().put("search", "Shortcut+Shift+F");
        }

        currentConfig.getShortcuts().forEach(hotKeyManager::updateShortcut);
    }

    public void initSystemUI(Stage stage) {
        themeManager.applyTheme(currentConfig.getThemeId());
        if (currentConfig.getFontSize() != null) {
            fontManager.applyFontToStage(stage, currentConfig.getFontSize());
        }
    }

    public void updateTheme(String themeId) {
        currentConfig.setThemeId(themeId);
        themeManager.applyTheme(themeId);
        persistConfig();
    }

    public void updateFontSize(Integer fontSize) {
        currentConfig.setFontSize(fontSize);
        fontManager.applyFontToAll(fontSize);
        persistConfig();
    }

    public void updateShortcut(String actionKey, String keyCombination) {
        currentConfig.getShortcuts().put(actionKey, keyCombination);
        hotKeyManager.updateShortcut(actionKey, keyCombination);
        persistConfig();
    }

    private void persistConfig() {
        viewDataService.saveState(STORAGE_KEY, currentConfig);
    }
}
