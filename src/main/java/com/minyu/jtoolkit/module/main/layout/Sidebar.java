package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.module.main.component.SearchDialog;
import javafx.application.Platform;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.Objects;

public final class Sidebar extends VBox {

    private final NavTree navTree;
    private final NavList navList;
    private final MainModel model;

    private int clickCount = 0;
    private long lastClickTime = 0;

    // 缓存 dialog 实例
    private SearchDialog searchDialog;

    public Sidebar(MainModel model) {
        super();
        this.model = model;
        this.navTree = new NavTree(model);
        this.navList = new NavList(model);

        this.navTree.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val.getValue() != null && !val.getValue().isGroup()) {
                this.model.navigate(val.getValue().fxmlPath());
                this.navList.getSelectionModel().clearSelection();
            }
        });

        this.navList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val.fxmlPath() != null) {
                this.model.navigate(val.fxmlPath());
                this.navTree.getSelectionModel().clearSelection();
            }
        });

        createView();
        initShortcuts();
    }

    private void createView() {
        var header = new Header();
        VBox.setVgrow(navTree, Priority.ALWAYS);
        this.navList.getStyleClass().add("footer-list");
        setId("sidebar");
        getChildren().addAll(header, navTree, navList);
    }

    private void initShortcuts() {
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> {
                    if (e.getCode().getName().equals("Slash") && !e.isShortcutDown()) {
                        openSearchDialog();
                    }
                });
            }
        });
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
        return new VBox();
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