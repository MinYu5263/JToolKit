package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.module.main.component.SearchDialog;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Sidebar extends VBox {

    private final NavTree navTree;
    private final MainModel model;

    private int clickCount = 0;
    private long lastClickTime = 0;

    // 缓存 dialog 实例
    private SearchDialog searchDialog;

    public Sidebar(MainModel model) {
        super();
        this.model = model;
        this.navTree = new NavTree(model);

        createView();

        // 绑定模型：当 Model 页面变化时，自动选中 NavTree 对应项
        // (这里需要 MainModel 提供 getTreeItemForPage 方法，或者简单的遍历查找，
        //  如果没有该方法，可以先注释掉这块联动逻辑)
        /* model.selectedPageProperty().addListener((obs, old, val) -> {
            if (val != null) {
                // navTree.getSelectionModel().select(...);
            }
        });
        */

        // 注册全局快捷键 (例如按 / 呼出搜索)
        // 简单实现：监听 Scene 的按键事件
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> {
                    // 如果是 / 键，且当前不是在输入框里输入文字，则打开搜索
                    if (e.getCode().getName().equals("Slash") && !e.isShortcutDown()) {
                        // 这里可以加更细致的判断，防止在输入框里打字时误触
                        // openSearchDialog();
                    }
                });
            }
        });
    }

    private void createView() {
        // 1. 创建头部 (Logo + Search Button)
        var header = new Header();

        // 2. 设置 NavTree 占据剩余空间
        VBox.setVgrow(navTree, Priority.ALWAYS);

        // 3. 底部 (版本号或其他)
        var footer = createFooter();

        setId("sidebar");

        getChildren().addAll(header, navTree, footer);
    }

    private void openSearchDialog() {
        if (searchDialog == null) {
            searchDialog = new SearchDialog(model);
            // 设置关闭时清空内容，可选
            // searchDialog.setClearOnClose(true);
        }
        // 调用 ModalBox 的 show 方法，它会自动寻找 ID 为 "main-modal-pane" 的 ModalPane
        searchDialog.show(getScene());

        // 延迟聚焦，体验更好
        Platform.runLater(searchDialog::begForFocus);
    }

    private VBox createFooter() {
        var footer = new VBox();
        footer.getStyleClass().add("footer");

        var settingsBtn = createFooterItem("设置", Material2MZ.SETTINGS);
        settingsBtn.setOnAction(e -> {
            try {
                new FXMLLoader(getClass().getResource("fxml/settings/SettingsView.fxml")).load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        footer.getChildren().addAll(settingsBtn);
        return footer;
    }

    private Button createFooterItem(String text, Ikon icon) {
        FontIcon iconView = new FontIcon(icon);
        Label label = new Label(text);
        label.getStyleClass().addAll(Styles.TEXT);
        var container = new HBox(10, iconView, label);
        container.setAlignment(Pos.CENTER_LEFT);

        var btn = new Button();
        btn.setGraphic(container);
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.getStyleClass().add("footer-button");

        return btn;
    }

    /// ////////////////////////////////////////////////////////////////////////
    // 内部类 Header：负责 Logo 和 搜索按钮

    /// ////////////////////////////////////////////////////////////////////////

    private class Header extends VBox {

        public Header() {
            super();
            getStyleClass().add("header");
            setPadding(new Insets(20, 10, 20, 10));
            setSpacing(20);

            getChildren().setAll(
                    createLogo(),
                    createSearchButton()
            );
        }

        private HBox createLogo() {
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_32.png")));
            ImageView appImage = new ImageView(appIcon);

            appImage.setFitHeight(32);
            appImage.setFitWidth(32);

            StackPane logoBox = new StackPane(appImage);

            var titleLbl = new Label("JToolKit");
            titleLbl.getStyleClass().addAll(Styles.TITLE_3);

            var root = new HBox(appImage, titleLbl);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("logo");

            // root.setCursor(javafx.scene.Cursor.HAND);
            appImage.setOnMouseClicked(e -> {
                long now = System.currentTimeMillis();
                if (now - lastClickTime > 500) {
                    clickCount = 0;
                }

                clickCount++;
                lastClickTime = now;

                // 连续点击 7 次触发
                if (clickCount >= 7) {
                    showHiddenCredits();
                    clickCount = 0; // 重置
                }
            });

            return root;
        }

        // 隐藏的版权声明
        private void showHiddenCredits() {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("关于作者");
            alert.setHeaderText("JToolKit Core");
            alert.setContentText("Original Author: " + decodeAuthor());
            alert.showAndWait();
        }

        // 简单的字符串混淆，防止全局搜索
        private String decodeAuthor() {
            return new String(java.util.Base64.getDecoder().decode("TWlueXU="));
        }

        // 这个按钮伪装成搜索框，是 AtlantaFX 的精髓
        private Button createSearchButton() {
            var searchIcon = new FontIcon(Material2MZ.SEARCH);
            var titleLbl = new Label("Search", searchIcon);

            var hintLbl = new Label("Press /");
            hintLbl.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

            var searchBox = new HBox(titleLbl, new Spacer(), hintLbl);
            searchBox.setAlignment(Pos.CENTER_LEFT);
            // 关键：让内部 HBox 撑满按钮宽度
            HBox.setHgrow(searchBox, Priority.ALWAYS);

            var root = new Button();
            root.getStyleClass().addAll("search-button"); // 需要 CSS 配合去边框
            root.setGraphic(searchBox);
            root.setMaxWidth(Double.MAX_VALUE); // 按钮撑满父容器

            // 样式微调：模拟输入框的外观
            root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 8px 12px;");

            root.setOnAction(e -> openSearchDialog());

            return root;
        }
    }
}