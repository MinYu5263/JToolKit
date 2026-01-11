package com.minyu.jtoolkit;

import com.minyu.jtoolkit.core.event.StageReadyEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;

/**
 * JavaFX运行时代理，管理Spring Boot上下文生命周期，发布StageReadyEvent
 */
public class JfxRuntime extends Application {
    private ConfigurableApplicationContext context;
    private Stage splashStage;

    @Override
    public void init() {
        /*context = new SpringApplicationBuilder()
                .sources(JToolKitApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));*/
    }

    @Override
    public void start(Stage stage) {
        showSplashScreen(stage);

        new Thread(() -> {
            try {
                // 启动 Spring Boot
                context = new SpringApplicationBuilder()
                        .sources(JToolKitApplication.class)
                        .headless(false)
                        .run(getParameters().getRaw().toArray(new String[0]));

                // 3. Spring 加载完成，切回 JavaFX 线程更新 UI
                Platform.runLater(() -> {
                    // 关闭 Splash
                    if (splashStage != null) {
                        splashStage.close();
                    }
                    context.publishEvent(new StageReadyEvent(stage));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        }, "Spring-Startup-Thread").start();
    }

    private void showSplashScreen(Stage parentStage) {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        VBox background = new VBox();
        background.setStyle("""
            -fx-background-color: #2b2b2b;
            -fx-background-radius: 15;
            -fx-border-color: #444444;
            -fx-border-radius: 15;
            -fx-border-width: 1;
        """);
        background.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.5)));

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(40));

        // Logo (如果有)
        try {
            ImageView logo = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png"))));
            logo.setFitWidth(64);
            logo.setFitHeight(64);
            content.getChildren().add(logo);
        } catch (Exception ignored) {}

        // 标题
        Label title = new Label("JToolKit");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        // 副标题/状态文字
        Label subTitle = new Label("正在初始化核心组件...");
        subTitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");

        // 进度条
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        // 使用 CSS 定制进度条颜色（比如改为绿色或紫色）
        progressBar.setStyle("-fx-accent: #6200ea;");

        content.getChildren().addAll(title, progressBar, subTitle);

        // 将背景和内容叠加
        root.getChildren().addAll(background, content);

        // 5. 创建 Scene，关键是要设置填充色为透明
        Scene scene = new Scene(root, 500, 300);
        scene.setFill(Color.TRANSPARENT); // 这一步至关重要，否则四个角是白色的

        splashStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_32.png"))));

        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}
