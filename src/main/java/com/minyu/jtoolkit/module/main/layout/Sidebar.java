package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
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

    private SearchDialog searchDialog;

    public Sidebar(MainModel model) {
        super();
        this.model = model;
        this.navTree = new NavTree();
        this.navList = new NavList();

        this.navTree.rootProperty().bind(model.navTreeProperty());
        this.navList.itemsProperty().bind(model.footerListProperty());

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

        this.model.selectedPageProperty().addListener((obs, old, newPath) -> {
            if (newPath != null) {
                this.navTree.selectPath(newPath);
                this.navList.selectPath(newPath);
            }
        });
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
                    if (e.getCode() == KeyCode.F && e.isShortcutDown() && e.isShiftDown()) {
                        e.consume();
                        openSearchDialog();
                    }
                });
            }
        });
    }

    public void openSearchDialog() {
        if (searchDialog == null) {
            searchDialog = new SearchDialog(model);
        }
        searchDialog.show(getScene());

        Platform.runLater(searchDialog::begForFocus);
    }

    private VBox createFooter() {
        return new VBox();
    }

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

            appImage.setOnMouseClicked(e -> {
                long now = System.currentTimeMillis();
                if (now - lastClickTime > 500) {
                    clickCount = 0;
                }

                clickCount++;
                lastClickTime = now;

                if (clickCount >= 7) {
                    showHiddenCredits();
                    clickCount = 0;
                }
            });

            return root;
        }

        private void showHiddenCredits() {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("关于作者");
            alert.setHeaderText("JToolKit Core");
            alert.setContentText("Original Author: " + decodeAuthor());
            alert.showAndWait();
        }

        private String decodeAuthor() {
            return new String(java.util.Base64.getDecoder().decode("TWlueXU="));
        }

        private Button createSearchButton() {
            var searchIcon = new FontIcon(Material2MZ.SEARCH);
            var titleLbl = new Label("搜索", searchIcon);

            var hintLbl = new Label();
            hintLbl.textProperty().bind(model.searchShortcutTextProperty());
            hintLbl.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

            var searchBox = new HBox(titleLbl, new Spacer(), hintLbl);
            searchBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(searchBox, Priority.ALWAYS);

            var root = new Button();
            root.getStyleClass().addAll("search-button");
            root.setGraphic(searchBox);
            root.setMaxWidth(Double.MAX_VALUE);

            root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 8px 12px;");

            root.setOnAction(e -> openSearchDialog());

            return root;
        }
    }
}
