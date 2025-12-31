package com.minyu.jtoolkit.module.settings;

import com.minyu.jtoolkit.core.service.ThemeManager;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SettingsController extends BaseController<SettingsViewState> {
    private static final String VIEW_KEY = "app_settings";

    @FXML
    private ComboBox<ThemeItem> themeCombo;

    private final ThemeManager themeManager;

    private record ThemeItem(String displayName, String themeId) {
    }

    public SettingsController(ThemeManager themeManager) {
        this.themeManager = themeManager;
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

        themeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                themeManager.applyTheme(newVal.themeId());
            }
        });
    }

    @Override
    protected String getViewKey() {
        return VIEW_KEY;
    }

    @Override
    protected Class<SettingsViewState> getStorageType() {
        return SettingsViewState.class;
    }

    @Override
    protected void initView() {
        initThemeCombo();
    }

    @Override
    protected void restoreValues(SettingsViewState state) {
        if (state.getThemeId() != null) {
            String savedId = state.getThemeId();
            for (ThemeItem item : themeCombo.getItems()) {
                if (Objects.equals(item.themeId(), savedId)) {
                    themeCombo.getSelectionModel().select(item);
                    return;
                }
            }
        }
    }

    @Override
    protected void initDefaultValues() {
        if (themeCombo.getSelectionModel().getSelectedItem() == null) {
            themeCombo.getSelectionModel().selectFirst();
        }
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(themeCombo.getSelectionModel().selectedItemProperty());
    }

    @Override
    protected SettingsViewState captureValues() {
        SettingsViewState state = new SettingsViewState();
        ThemeItem selected = themeCombo.getSelectionModel().getSelectedItem();
        if (selected != null) {
            state.setThemeId(selected.themeId());
        }
        return state;
    }
}