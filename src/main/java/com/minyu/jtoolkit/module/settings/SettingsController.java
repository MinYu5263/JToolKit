package com.minyu.jtoolkit.module.settings;

import com.minyu.jtoolkit.core.service.AppConfig;
import com.minyu.jtoolkit.core.service.AppConfigManager;
import com.minyu.jtoolkit.core.service.ThemeManager;
import com.minyu.jtoolkit.core.util.AppLifecycleUtils;
import com.minyu.jtoolkit.system.service.StorageService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
@Component
public class SettingsController implements Initializable {
    @FXML
    private VBox rootBox;
    @FXML
    private ComboBox<ThemeItem> themeCombo;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private TextField shortcutField;
    @FXML
    private Button btnClearPageData;
    @FXML
    private Button btnClearAllData;

    private KeyCombination currentKeyCombination;

    private final AppConfigManager appConfigManager;
    private final StorageService storageService;

    private record ThemeItem(String displayName, String themeId) {
    }

    public SettingsController(AppConfigManager appConfigManager, StorageService storageService) {
        this.appConfigManager = appConfigManager;
        this.storageService = storageService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. 初始化下拉框数据
        initThemeCombo();

        // 2. 初始化字体微调器
        initFontSizeSpinner();

        // 3. 初始化快捷键录制逻辑
        initShortcutRecorder();

        // 4. 回显当前的配置
        loadFromAppConfig();

        // 5. 配置监听器
        bindConfigListeners();
    }

    private void initThemeCombo() {
        themeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeItem item) {
                return item == null ? "" : item.displayName();
            }

            @Override
            public ThemeItem fromString(String string) {
                return null;
            }
        });

        themeCombo.getItems().addAll(
                new ThemeItem("跟随系统 (SYSTEM)", ThemeManager.ID_SYSTEM),
                new ThemeItem("浅色 (Primer Light)", "Primer Light"),
                new ThemeItem("深色 (Primer Dark)", "Primer Dark"),
                new ThemeItem("北欧浅色 (Nord Light)", "Nord Light"),
                new ThemeItem("北欧深色 (Nord Dark)", "Nord Dark"),
                new ThemeItem("苹果浅色 (Cupertino Light)", "Cupertino Light"),
                new ThemeItem("苹果深色 (Cupertino Dark)", "Cupertino Dark"),
                new ThemeItem("吸血鬼 (Dracula)", "Dracula")
        );
    }

    private void initFontSizeSpinner() {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 36, ThemeManager.DEFAULT_FONT_SIZE);
        fontSizeSpinner.setValueFactory(valueFactory);
    }

    private void loadFromAppConfig() {
        AppConfig config = appConfigManager.getCurrentConfig();

        // 回显主题
        String currentThemeId = config.getThemeId();
        themeCombo.getItems().stream()
                .filter(item -> Objects.equals(item.themeId(), currentThemeId))
                .findFirst()
                .ifPresent(item -> themeCombo.setValue(item));

        // 回显字体
        if (config.getFontSize() != null) {
            fontSizeSpinner.getValueFactory().setValue(config.getFontSize());
        }

        // 回显快捷键
        String searchShortcut = config.getShortcuts().get("search");
        if (searchShortcut != null && !searchShortcut.isBlank()) {
            try {
                currentKeyCombination = KeyCombination.valueOf(searchShortcut);
                shortcutField.setText(currentKeyCombination.getDisplayText());
            } catch (Exception e) {
                log.error("Error parsing shortcut", e);
            }
        }
    }

    private void bindConfigListeners() {
        // 主题变更
        themeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                appConfigManager.updateTheme(newVal.themeId());
            }
        });

        // 字体变更
        fontSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                appConfigManager.updateFontSize(newVal);
            }
        });
    }

    private void initShortcutRecorder() {
        shortcutField.setOnKeyPressed(event -> {
            event.consume();
            KeyCode code = event.getCode();

            // 删除快捷键
            if (code == KeyCode.BACK_SPACE || code == KeyCode.DELETE) {
                shortcutField.setText("");
                currentKeyCombination = null;
                // 更新配置：移除快捷键
                appConfigManager.updateShortcut("search", null);
                rootBox.requestFocus();
                return;
            }

            if (code.isModifierKey()) return;

            KeyCombination.ModifierValue shift = event.isShiftDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
            KeyCombination.ModifierValue ctrl = event.isControlDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
            KeyCombination.ModifierValue alt = event.isAltDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
            KeyCombination.ModifierValue meta = event.isMetaDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;

            KeyCodeCombination combination = new KeyCodeCombination(code, shift, ctrl, alt, meta, KeyCombination.ModifierValue.UP);

            currentKeyCombination = combination;
            shortcutField.setText(combination.getDisplayText());

            appConfigManager.updateShortcut("search", combination.getName());
            rootBox.requestFocus();
        });
    }

    @FXML
    private void onClearPageData() {
        if (showConfirmDialog("确定要清除业务数据吗？", "这将删除所有工具产生的记录（如JSON工具的历史、最近打开的文件等），但会保留主题、字体和快捷键设置。")) {
            storageService.clearExclude("app_config");
            showInfoDialog("清理完成", "业务数据已重置。");
        }
    }

    @FXML
    private void onClearAllData() {
        if (showConfirmDialog("警告：确定要清空所有数据吗？", "此操作不可恢复！\n应用将重置为初始状态（包括主题和设置），建议重启应用以避免显示异常。")) {
            // 清空所有
            storageService.clearAll();
            showInfoDialog("重置完成", "所有数据已清空，请重启应用。");
        }
    }

    /**
     * 打开数据目录
     */
    @FXML
    private void onOpenDataDir() {
        try {
            Path dir = storageService.getBaseDirectory();

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            File file = dir.toFile();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                showInfoDialog("无法打开", "当前系统不支持自动打开文件夹，路径为：\n" + file.getAbsolutePath());
            }

        } catch (IOException e) {
            log.error("Failed to open data directory", e);
            showInfoDialog("打开失败", "无法打开文件夹：" + e.getMessage());
        }
    }

    @FXML
    private void onRestartApp() {
        if (showConfirmDialog("确认重启", "确定要立即重启应用吗？\n未保存的内容可能会丢失。")) {
            try {
                AppLifecycleUtils.restart();
            } catch (Exception e) {
                showInfoDialog("重启失败", "无法自动重启，请手动操作。\n错误：" + e.getMessage());
            }
        }
    }

    @FXML
    private void onRestartAsAdmin() {
        if (AppLifecycleUtils.isAdmin()) {
            showInfoDialog("提示", "当前已经是管理员身份运行。");
            return;
        }

        if (showConfirmDialog("提升权限", "确定要以管理员身份重启应用吗？\n系统将弹出 UAC 提示框。")) {
            try {
                AppLifecycleUtils.restartAsAdmin();
            } catch (Exception e) {
                showInfoDialog("重启失败", "操作被取消或发生错误。\n" + e.getMessage());
            }
        }
    }

    // 通用确认弹窗
    private boolean showConfirmDialog(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认操作");
        alert.setHeaderText(header);
        alert.setContentText(content);

        // 适配当前窗口 (如果有 owner 可以设置 initOwner)
        alert.initOwner(rootBox.getScene().getWindow());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // 通用提示弹窗
    private void showInfoDialog(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initOwner(rootBox.getScene().getWindow());
        alert.show();
    }
}