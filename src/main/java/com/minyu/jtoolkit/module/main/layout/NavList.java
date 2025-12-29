package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.theme.Tweaks;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

public class NavList extends ListView<Nav> {

    private static final int CELL_HEIGHT = 30;
    private static final int TOP_PADDING = 5;
    private static final int BORDER_WIDTH = 1;

    public NavList(MainModel model) {
        super();
        getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        setFocusTraversable(false);

        setFixedCellSize(CELL_HEIGHT);

        itemsProperty().bind(model.footerListProperty());
        setCellFactory(lv -> new NavListCell());

        getItems().addListener((ListChangeListener<Nav>) c -> resize());
        resize();
    }

    private void resize() {
        int count = getItems().size();
        // 高度 = 行数 * 行高 + 顶部内边距 + 边框修正
        double height = count * CELL_HEIGHT + TOP_PADDING + BORDER_WIDTH;

        setPrefHeight(height);
        setMinHeight(height);
        setMaxHeight(height);
    }

    static class NavListCell extends ListCell<Nav> {
        private final HBox root;
        private final Label titleLabel;
        private final FontIcon leftIcon;

        public NavListCell() {
            // 图标容器
            leftIcon = new FontIcon();
            // 这里可以复用 left-icon 样式，或者单独定义
            leftIcon.getStyleClass().add("left-icon");

            StackPane leftIconContainer = new StackPane(leftIcon);
            leftIconContainer.setAlignment(Pos.CENTER);
            leftIconContainer.getStyleClass().add("icon-container");

            // 核心：强制 30px，与 NavTree 一级菜单对齐
            leftIconContainer.setMinWidth(30);
            leftIconContainer.setMaxWidth(30);

            titleLabel = new Label();
            titleLabel.getStyleClass().add("title");

            root = new HBox(leftIconContainer, titleLabel);
            root.setAlignment(Pos.CENTER_LEFT);
            // 复用 nav-tree-cell 的样式，保证高度和边框一致
            root.getStyleClass().add("nav-tree-cell");
            root.setSpacing(0);

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Nav item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setGraphic(null);
                setText(null);
            } else {
                titleLabel.setText(item.title());

                if (item.icon() != null) {
                    leftIcon.setIconCode(item.icon());
                    leftIcon.setVisible(true);
                }

                setGraphic(root);
            }
        }
    }
}