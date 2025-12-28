package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.theme.Tweaks;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

public class NavTree extends TreeView<Nav> {

    public NavTree(MainModel model) {
        super();
        getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        setShowRoot(false);
        rootProperty().bind(model.navTreeProperty());
        setCellFactory(p -> new NavTreeCell());
        setFocusTraversable(false);

        getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val.getValue() != null && !val.getValue().isGroup()) {
                model.navigate(val.getValue().fxmlPath());
            }
        });
    }

    static class NavTreeCell extends TreeCell<Nav> {
        private final HBox root;
        private final Label titleLabel;
        private final FontIcon arrowIcon;
        private final FontIcon leftIcon;
        private final StackPane leftIconContainer;

        public NavTreeCell() {
            // 1. 图标容器
            leftIcon = new FontIcon();
            leftIcon.getStyleClass().add("left-icon");

            leftIconContainer = new StackPane(leftIcon);
            // 核心：固定宽度占位
            leftIconContainer.setMinWidth(30);
            leftIconContainer.setPrefWidth(30);
            leftIconContainer.setMaxWidth(30);
            leftIconContainer.setAlignment(Pos.CENTER);
            leftIconContainer.getStyleClass().add("icon-container");

            // 2. 标题
            titleLabel = new Label();
            titleLabel.getStyleClass().add("title");

            // 3. 右侧箭头
            arrowIcon = new FontIcon(Material2AL.KEYBOARD_ARROW_LEFT);
            arrowIcon.getStyleClass().add("arrow");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            root = new HBox(leftIconContainer, titleLabel, spacer, arrowIcon);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("nav-tree-cell");

            // 确保没有额外的间距
            root.setSpacing(0);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            root.setOnMouseClicked(e -> {
                if (getItem() != null && getItem().isGroup() && e.getButton() == MouseButton.PRIMARY) {
                    getTreeItem().setExpanded(!getTreeItem().isExpanded());
                    e.consume();
                }
            });
        }

        @Override
        protected void updateItem(Nav item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setGraphic(null);
                setText(null);
            } else {
                titleLabel.setText(item.title());

                // 父级给 30px，子级给 12px
                double containerWidth = item.isGroup() ? 30 : 12;

                leftIconContainer.setMinWidth(containerWidth);
                leftIconContainer.setPrefWidth(containerWidth);
                leftIconContainer.setMaxWidth(containerWidth);

                leftIconContainer.setVisible(true);
                leftIconContainer.setManaged(true);

                if (item.isGroup() && item.icon() != null) {
                    leftIcon.setIconCode(item.icon());
                    leftIcon.setVisible(true);
                } else {
                    leftIcon.setVisible(false);
                }

                arrowIcon.setVisible(item.isGroup());
                if (item.isGroup() && getTreeItem().isExpanded()) {
                    arrowIcon.setIconCode(Material2AL.KEYBOARD_ARROW_DOWN);
                } else {
                    arrowIcon.setIconCode(Material2AL.KEYBOARD_ARROW_LEFT);
                }

                setGraphic(root);
            }
        }
    }
}