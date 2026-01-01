package com.minyu.jtoolkit.module.settings;

import com.minyu.jtoolkit.core.service.AppConfig;
import com.minyu.jtoolkit.core.service.AppConfigManager;
import com.minyu.jtoolkit.core.service.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

@Slf4j
@Component
public class SettingsController implements Initializable {
    @FXML
    private ComboBox<ThemeItem> themeCombo;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private TextField shortcutField;

    private KeyCombination currentKeyCombination;

    private final AppConfigManager appConfigManager;

    private record ThemeItem(String displayName, String themeId) {
    }

    public SettingsController(AppConfigManager appConfigManager) {
        this.appConfigManager = appConfigManager;
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
        });
    }
}