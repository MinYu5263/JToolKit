package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.theme.Tweaks;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

public class NavTree extends TreeView<Nav> {

    public NavTree() {
        super();
        getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        setShowRoot(false);
        setCellFactory(p -> new NavTreeCell());
        setFocusTraversable(false);
    }

    public boolean selectPath(String path) {
        if (path == null) return false;

        TreeItem<Nav> target = findItemByPath(getRoot(), path);
        if (target != null) {
            getSelectionModel().select(target);
            return true;
        } else {
            getSelectionModel().clearSelection();
            return false;
        }
    }

    private TreeItem<Nav> findItemByPath(TreeItem<Nav> root, String path) {
        if (root == null) return null;
        if (root.getValue() != null && path.equals(root.getValue().fxmlPath())) {
            return root;
        }
        for (TreeItem<Nav> child : root.getChildren()) {
            var found = findItemByPath(child, path);
            if (found != null) return found;
        }
        return null;
    }

    static class NavTreeCell extends TreeCell<Nav> {
        private final HBox root;
        private final Label titleLabel;
        private final FontIcon arrowIcon;
        private final FontIcon leftIcon;
        private final StackPane leftIconContainer;

        public NavTreeCell() {
            leftIcon = new FontIcon();
            leftIcon.getStyleClass().add("left-icon");

            leftIconContainer = new StackPane(leftIcon);
            leftIconContainer.setMinWidth(30);
            leftIconContainer.setPrefWidth(30);
            leftIconContainer.setMaxWidth(30);
            leftIconContainer.setAlignment(Pos.CENTER);
            leftIconContainer.getStyleClass().add("icon-container");

            titleLabel = new Label();
            titleLabel.getStyleClass().add("title");

            arrowIcon = new FontIcon(Material2AL.KEYBOARD_ARROW_LEFT);
            arrowIcon.getStyleClass().add("arrow");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            root = new HBox(leftIconContainer, titleLabel, spacer, arrowIcon);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("nav-tree-cell");

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