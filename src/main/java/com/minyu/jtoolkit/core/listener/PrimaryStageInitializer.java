package com.minyu.jtoolkit.core.listener;

import com.minyu.jtoolkit.core.event.StageReadyEvent;
import com.minyu.jtoolkit.core.service.ThemeManager;
import com.minyu.jtoolkit.core.service.ViewLoader;
import com.minyu.jtoolkit.module.settings.SettingsViewState;
import com.minyu.jtoolkit.system.service.ViewDataService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 主Stage初始化器，监听StageReadyEvent，配置主窗口（主题、视图、样式、标题等）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ViewLoader viewLoader;
    private final ThemeManager themeManager;
    private final ViewDataService viewDataService;

    @Value("${jtoolkit.title:JToolKit App}")
    private String appTitle;

    @Value("${jtoolkit.width:1280}")
    private int width;

    @Value("${jtoolkit.height:768}")
    private int height;

    @Value("${jtoolkit.theme:light}")
    private String defaultTheme;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();

        Parent root = viewLoader.load("fxml/main/MainView.fxml");
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/index.css")).toExternalForm());
        stage.setScene(scene);

        stage.setTitle(appTitle);
        Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_32.png")));
        stage.getIcons().add(appIcon);

        // 设置主题
        SettingsViewState settingsViewState = viewDataService.loadState("app_settings", SettingsViewState.class);
        if (settingsViewState != null) {
            themeManager.applyTheme(settingsViewState.getThemeId());
        } else {
            themeManager.applyTheme(defaultTheme);
        }

        stage.show();
    }
}