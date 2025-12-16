package com.minyu.jtoolkit.module.settings;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.PrimerLight;
import com.minyu.jtoolkit.system.service.AppSettingService;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsController {

    private final AppSettingService settingService;
    private final ApplicationContext applicationContext; // 用于获取 HostServices 打开网页

    // === Keys ===
    private static final String KEY_THEME = "app.theme";
    private static final String KEY_CLOSE_TRAY = "app.general.close_to_tray";
    private static final String KEY_CHECK_UPDATE = "app.general.check_update";

    // === UI ===
    @FXML private ToggleGroup themeGroup;
    @FXML private RadioButton rbLight;
    @FXML private RadioButton rbDark;
    @FXML private RadioButton rbDracula;

    @FXML private ToggleSwitch tsCloseToTray;
    @FXML private ToggleSwitch tsCheckUpdate;

    @FXML
    public void initialize() {
        loadSettings();
        bindListeners();
    }

    private void loadSettings() {
        // 1. 恢复主题设置
        /*String currentTheme = settingService.get(KEY_THEME, "LIGHT");
        switch (currentTheme) {
            case "DARK" -> rbDark.setSelected(true);
            case "DRACULA" -> rbDracula.setSelected(true);
            default -> rbLight.setSelected(true);
        }*/

        // 2. 恢复通用设置
        // tsCloseToTray.setSelected(settingService.getBoolean(KEY_CLOSE_TRAY, false));
        // tsCheckUpdate.setSelected(settingService.getBoolean(KEY_CHECK_UPDATE, true));
    }

    private void bindListeners() {
        // 1. 监听主题切换
        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String themeCode = newVal.getUserData().toString();
                applyTheme(themeCode);
                // settingService.set(KEY_THEME, themeCode, "应用主题");
            }
        });

        // 2. 监听开关
        tsCloseToTray.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // settingService.set(KEY_CLOSE_TRAY, String.valueOf(newVal), "是否最小化到托盘");
            // 这里可以触发 MainController 的某种状态更新，或者 MainController 直接读取这个配置
        });

        tsCheckUpdate.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // settingService.set(KEY_CHECK_UPDATE, String.valueOf(newVal), "启动时检查更新");
        });
    }

    /**
     * 切换 AtlantaFX 主题
     */
    private void applyTheme(String themeCode) {
        switch (themeCode) {
            case "DARK" -> Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            case "DRACULA" -> Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
            default -> Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()); // 或 CupertinoLight
        }
    }

    @FXML
    public void onOpenGithub() {
        // 使用 Spring 获取 HostServices 打开浏览器
        try {
            // 注意：HostServices 通常由 Application 类提供，需要通过 Bean 获取或静态方式
            // 这里假设你能获取到，或者使用 java.awt.Desktop
            java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://github.com/your-repo"));
        } catch (Exception e) {
            log.error("无法打开浏览器", e);
        }
    }
}