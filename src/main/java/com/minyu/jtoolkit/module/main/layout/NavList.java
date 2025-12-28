package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.theme.Tweaks;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

public class NavList extends ListView<Nav> {

    // 预设行高，用于计算总高度 (36px是通常的舒适高度)
    private static final int CELL_HEIGHT = 36;

    public NavList(MainModel model) {
        super();

        // 1. 基础样式
        getStyleClass().add(Tweaks.EDGE_TO_EDGE); // 去边框
        getStyleClass().add("nav-list");          // 方便 CSS 覆盖
        setFocusTraversable(false);               // 避免 Tab 键选中整个列表

        // 2. 数据源
        var items = model.createFooter();
        setItems(FXCollections.observableArrayList(items));

        // 3. 关键：根据数量自动计算高度，避免出现滚动条
        setPrefHeight(items.size() * CELL_HEIGHT);
        setFixedCellSize(CELL_HEIGHT); // 固定每一行的高度，性能更好

        // 4. 设置渲染器
        setCellFactory(lv -> new NavListCell(model));
    }

    // --- 内部类：简单的单元格渲染 ---
    static class NavListCell extends ListCell<Nav> {
        private final HBox root;
        private final Label titleLabel;
        private final FontIcon iconNode;
        private final StackPane iconContainer;

        public NavListCell(MainModel model) {
            // 左侧图标容器 (保持与 NavTree 30px 对齐)
            iconNode = new FontIcon();
            iconContainer = new StackPane(iconNode);
            iconContainer.setMinWidth(30);
            iconContainer.setMaxWidth(30);
            iconContainer.setAlignment(Pos.CENTER);

            // 标题
            titleLabel = new Label();
            titleLabel.getStyleClass().add("title");

            // 布局
            root = new HBox(iconContainer, titleLabel);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("nav-list-cell");

            // 交互：直接监听点击，比 SelectionModel 更直接
            setOnMouseClicked(e -> {
                Nav item = getItem();
                if (item != null && item.fxmlPath() != null) {
                    model.navigate(item.fxmlPath());
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

                if (item.icon() != null) {
                    iconNode.setIconCode(item.icon());
                    iconContainer.setVisible(true);
                } else {
                    iconContainer.setVisible(false);
                }

                setGraphic(root);
            }
        }
    }
}