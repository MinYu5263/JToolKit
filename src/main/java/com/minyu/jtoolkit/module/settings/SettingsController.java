package com.minyu.jtoolkit.module.settings;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.*;
import com.minyu.jtoolkit.system.service.AppSettingService;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsController {

    private final AppSettingService settingService;

    @FXML
    private ComboBox<ThemeItem> themeCombo;

    private record ThemeItem(String displayName, Theme theme) {
    }

    @FXML
    public void initialize() {
        initThemeCombo();
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
                new ThemeItem("浅色 (Primer Light)", new PrimerLight()),
                new ThemeItem("深色 (Primer Dark)", new PrimerDark()),
                new ThemeItem("北欧浅色 (Nord Light)", new NordLight()),
                new ThemeItem("北欧深色 (Nord Dark)", new NordDark()),
                new ThemeItem("苹果浅色 (Cupertino Light)", new CupertinoLight()),
                new ThemeItem("苹果深色 (Cupertino Dark)", new CupertinoDark()),
                new ThemeItem("吸血鬼 (Dracula)", new Dracula())
        );

        themeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applyTheme(newVal.theme());
            }
        });

        themeCombo.getSelectionModel().selectFirst();
    }

    private void applyTheme(Theme theme) {
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
    }

    /**
     * 切换 AtlantaFX 主题
     */
    /*private void applyTheme(String themeCode) {
        switch (themeCode) {
            case "DARK" -> Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            case "DRACULA" -> Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
            default -> Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()); // 或 CupertinoLight
        }
    }*/
}