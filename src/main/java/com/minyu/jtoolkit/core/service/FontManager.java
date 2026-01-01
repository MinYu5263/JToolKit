package com.minyu.jtoolkit.core.service;

import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Service;

/**
 * 字体管理
 */
@Service
public class FontManager {

    /**
     * 将字体大小应用到指定的 Stage
     */
    public void applyFontToStage(Stage stage, int fontSize) {
        if (stage != null && stage.getScene() != null) {
            applyFontToRoot(stage.getScene().getRoot(), fontSize);
        }
    }

    /**
     * 将字体大小应用到所有已打开的窗口
     */
    public void applyFontToAll(int fontSize) {
        for (Window window : Window.getWindows()) {
            if (window.getScene() != null) {
                applyFontToRoot(window.getScene().getRoot(), fontSize);
            }
        }
    }

    private void applyFontToRoot(Parent root, int fontSize) {
        if (root != null) {
            root.setStyle("-fx-font-size: " + fontSize + "px;");
        }
    }
}
