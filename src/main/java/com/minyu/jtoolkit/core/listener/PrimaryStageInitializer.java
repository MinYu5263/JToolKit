package com.minyu.jtoolkit.core.listener;

import com.minyu.jtoolkit.core.event.StageReadyEvent;
import com.minyu.jtoolkit.core.service.AppConfigManager;
import com.minyu.jtoolkit.core.service.ViewLoader;
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
    private final AppConfigManager appConfigManager; // 注入 Manager

    @Value("${jtoolkit.title:JToolKit}")
    private String appTitle;

    @Value("${jtoolkit.width:1280}")
    private int width;

    @Value("${jtoolkit.height:768}")
    private int height;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();

        Parent root = viewLoader.load("fxml/main/MainView.fxml");
        Scene scene = new Scene(root, width, height);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/index.css")).toExternalForm());

        stage.setScene(scene);
        stage.setTitle(appTitle);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_32.png"))));
        // 初始化系统页面
        appConfigManager.initSystemUI(stage);
        stage.show();
    }
}