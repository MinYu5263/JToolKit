package com.minyu.jtoolkit.core.component;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Global lightweight toast notifications.
 */
public final class Toast {

    private static final int MAX_VISIBLE_TOASTS = 5;
    private static final Duration DEFAULT_DURATION = Duration.seconds(2.4);
    private static final Duration ANIMATION_DURATION = Duration.millis(180);

    private static StackPane rootPane;
    private static VBox toastLayer;

    private Toast() {
    }

    public static void register(StackPane root) {
        if (root == null) {
            return;
        }

        rootPane = root;
        if (toastLayer != null && toastLayer.getParent() == rootPane) {
            return;
        }

        toastLayer = new VBox(8);
        toastLayer.getStyleClass().add("toast-layer");
        toastLayer.setAlignment(Pos.TOP_CENTER);
        toastLayer.setPickOnBounds(false);
        StackPane.setAlignment(toastLayer, Pos.TOP_CENTER);
        StackPane.setMargin(toastLayer, new Insets(18, 16, 0, 16));

        rootPane.getChildren().add(toastLayer);
    }

    public static void info(String message) {
        show(Type.INFO, message, DEFAULT_DURATION);
    }

    public static void success(String message) {
        show(Type.SUCCESS, message, DEFAULT_DURATION);
    }

    public static void warning(String message) {
        show(Type.WARNING, message, DEFAULT_DURATION);
    }

    public static void error(String message) {
        show(Type.ERROR, message, DEFAULT_DURATION);
    }

    public static void loading(String message) {
        show(Type.LOADING, message, DEFAULT_DURATION);
    }

    private static void show(Type type, String message, Duration duration) {
        Runnable task = () -> {
            if (toastLayer == null || message == null || message.isBlank()) {
                return;
            }

            Node toast = createToast(type, message);
            addToast(toastLayer.getChildren(), toast);
            trimVisibleToasts();
            playEnterAnimation(toast);
            scheduleDismiss(toast, duration);
        };

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    private static HBox createToast(Type type, String message) {
        FontIcon icon = new FontIcon(type.icon);
        icon.setIconSize(16);
        icon.getStyleClass().add("toast-icon");

        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);

        HBox toast = new HBox(8, icon, text);
        toast.getStyleClass().addAll("toast", type.styleClass);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(520);
        toast.setOpacity(0);
        toast.setTranslateY(-12);
        toast.setPickOnBounds(false);

        return toast;
    }

    static <T> void addToast(List<T> toasts, T toast) {
        toasts.add(toast);
    }

    private static void playEnterAnimation(Node toast) {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, toast);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(ANIMATION_DURATION, toast);
        slide.setFromY(-12);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        fade.play();
        slide.play();
    }

    private static void scheduleDismiss(Node toast, Duration duration) {
        PauseTransition pause = new PauseTransition(duration);
        pause.setOnFinished(e -> dismiss(toast));
        pause.play();
    }

    private static void dismiss(Node toast) {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, toast);
        fade.setFromValue(toast.getOpacity());
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(e -> toastLayer.getChildren().remove(toast));
        fade.play();
    }

    private static void trimVisibleToasts() {
        while (toastLayer.getChildren().size() > MAX_VISIBLE_TOASTS) {
            toastLayer.getChildren().remove(0);
        }
    }

    private enum Type {
        INFO(Feather.INFO, "toast-info"),
        SUCCESS(Feather.CHECK_CIRCLE, "toast-success"),
        WARNING(Feather.ALERT_CIRCLE, "toast-warning"),
        ERROR(Feather.X_CIRCLE, "toast-error"),
        LOADING(Feather.LOADER, "toast-loading");

        private final Ikon icon;
        private final String styleClass;

        Type(Ikon icon, String styleClass) {
            this.icon = icon;
            this.styleClass = styleClass;
        }
    }
}
