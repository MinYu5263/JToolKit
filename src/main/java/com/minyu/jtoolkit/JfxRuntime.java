package com.minyu.jtoolkit;

import com.minyu.jtoolkit.core.event.StageReadyEvent;
import com.minyu.jtoolkit.core.util.AppIconUtils;
import com.minyu.jtoolkit.core.util.ApplicationShutdown;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;

public class JfxRuntime extends Application {
    private ConfigurableApplicationContext context;
    private Stage splashStage;
    private Animation splashProgressAnimation;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(true);
        AppIconUtils.configureApplicationIcons(stage);
        showSplashScreen();

        Thread startupThread = new Thread(() -> {
            try {
                context = new SpringApplicationBuilder()
                        .sources(JToolKitApplication.class)
                        .headless(false)
                        .run(getParameters().getRaw().toArray(new String[0]));

                Platform.runLater(() -> {
                    closeSplashScreen(() -> context.publishEvent(new StageReadyEvent(stage)));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        }, "Spring-Startup-Thread");
        startupThread.setDaemon(true);
        startupThread.start();
    }

    private void showSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");
        root.setOpacity(0);

        StackPane card = new StackPane();
        card.setMaxSize(520, 320);
        card.setStyle("""
                    -fx-background-color: #1f232a;
                    -fx-background-radius: 18;
                    -fx-border-color: rgba(255,255,255,0.08);
                    -fx-border-radius: 18;
                    -fx-border-width: 1;
                """);
        card.setEffect(new DropShadow(28, Color.rgb(0, 0, 0, 0.42)));

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(70, 52, 38, 52));
        content.setMaxWidth(520);
        StackPane.setAlignment(content, Pos.TOP_LEFT);

        HBox logoRow = new HBox(14);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        try {
            ImageView logo = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png"))));
            logo.setFitWidth(58);
            logo.setFitHeight(58);
            logo.setPreserveRatio(true);
            logoRow.getChildren().add(logo);
        } catch (Exception ignored) {
        }

        Label title = new Label("JToolKit");
        title.setStyle("""
                    -fx-text-fill: #f4f7fb;
                    -fx-font-size: 30px;
                    -fx-font-weight: 700;
                    -fx-font-family: 'Inter', 'Segoe UI', sans-serif;
                """);
        logoRow.getChildren().add(title);

        Label subTitle = new Label("正在初始化核心组件...");
        subTitle.setStyle("-fx-text-fill: #aab4c3; -fx-font-size: 12px;");

        Region spacer = new Region();
        spacer.setMinHeight(118);

        StackPane progress = createSplashProgress(360);

        content.getChildren().addAll(logoRow, spacer, subTitle, progress);
        card.getChildren().add(content);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 560, 360);
        scene.setFill(Color.TRANSPARENT);

        AppIconUtils.configureApplicationIcons(splashStage);

        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);
        fadeIn.play();
    }

    private StackPane createSplashProgress(double width) {
        StackPane track = new StackPane();
        track.setPrefSize(width, 3);
        track.setMaxSize(width, 3);
        track.setStyle("-fx-background-color: rgba(255,255,255,0.08);");

        Rectangle bar = new Rectangle(150, 3);
        bar.setFill(Color.web("#58a6ff"));
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);

        Rectangle clip = new Rectangle(width, 3);
        track.setClip(clip);
        track.getChildren().add(bar);

        splashProgressAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.translateXProperty(), -170, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1250), new KeyValue(bar.translateXProperty(), width + 20, Interpolator.EASE_BOTH))
        );
        splashProgressAnimation.setCycleCount(Animation.INDEFINITE);
        splashProgressAnimation.play();

        return track;
    }

    private void closeSplashScreen(Runnable afterClose) {
        if (splashStage == null || splashStage.getScene() == null) {
            afterClose.run();
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), splashStage.getScene().getRoot());
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);
        fadeOut.setOnFinished(e -> {
            if (splashProgressAnimation != null) {
                splashProgressAnimation.stop();
                splashProgressAnimation = null;
            }
            splashStage.close();
            splashStage = null;
            afterClose.run();
        });
        fadeOut.play();
    }

    @Override
    public void stop() {
        ApplicationShutdown.exit(context);
    }
}
