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
    private final ListChangeListener<Nav> resizeListener = c -> resize();

    public NavList() {
        super();
        getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        setFocusTraversable(false);
        setFixedCellSize(CELL_HEIGHT);
        setCellFactory(lv -> new NavListCell());
        itemsProperty().addListener((obs, oldList, newList) -> {
            if (oldList != null) {
                oldList.removeListener(resizeListener);
            }
            if (newList != null) {
                newList.addListener(resizeListener);
                resize();
            }
        });
        resize();
    }

    public boolean selectPath(String path) {
        if (path == null) return false;

        Nav target = getItems().stream()
                .filter(n -> path.equals(n.fxmlPath()))
                .findFirst()
                .orElse(null);

        if (target != null) {
            getSelectionModel().select(target);
            return true;
        } else {
            getSelectionModel().clearSelection();
            return false;
        }
    }

    private void resize() {
        int count = getItems().size();
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
            leftIcon = new FontIcon();
            leftIcon.getStyleClass().add("left-icon");

            StackPane leftIconContainer = new StackPane(leftIcon);
            leftIconContainer.setAlignment(Pos.CENTER);
            leftIconContainer.getStyleClass().add("icon-container");

            leftIconContainer.setMinWidth(30);
            leftIconContainer.setMaxWidth(30);

            titleLabel = new Label();
            titleLabel.getStyleClass().add("title");

            root = new HBox(leftIconContainer, titleLabel);
            root.setAlignment(Pos.CENTER_LEFT);
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