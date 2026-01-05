package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.Tile;
import javafx.animation.RotateTransition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;


@DefaultProperty("items")
public class ConfigGroup extends VBox {
    private static final String STYLE_CLASS = "config-group";

    private final Tile header = new Tile();
    private final VBox container = new VBox();
    private final RotateTransition rotateTransition;
    private final BooleanProperty expanded = new SimpleBooleanProperty(true);
    private final FontIcon arrowIcon;

    public ConfigGroup() {
        getStyleClass().add(STYLE_CLASS);

        arrowIcon = new FontIcon(Material2AL.KEYBOARD_ARROW_DOWN);
        header.setAction(arrowIcon);
        header.setCursor(javafx.scene.Cursor.HAND);

        container.setFillWidth(true);
        container.getStyleClass().add("content-container");
        this.getChildren().addAll(header, container);

        rotateTransition = new RotateTransition(Duration.millis(200), arrowIcon);

        header.setOnMouseClicked(e -> setExpanded(!isExpanded()));

        expanded.addListener((obs, oldVal, newVal) -> updateState(newVal));

        updateState(isExpanded());
    }

    public ObservableList<Node> getItems() {
        return container.getChildren();
    }

    private void updateState(boolean isExpanded) {
        container.setVisible(isExpanded);
        container.setManaged(isExpanded);

        int targetAngle = isExpanded ? 0 : 90;

        if (header.getScene() == null) {
            arrowIcon.setRotate(targetAngle);
            // 确保动画对象的状态也同步，防止后续播放时跳变
            rotateTransition.jumpTo(Duration.ZERO);
        } else {
            rotateTransition.setToAngle(targetAngle);
            rotateTransition.play();
        }

        if (isExpanded) {
            getStyleClass().add("expanded");
            header.getStyleClass().add("expanded");
        } else {
            getStyleClass().remove("expanded");
            header.getStyleClass().remove("expanded");
        }
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public void setExpanded(boolean value) {
        expanded.set(value);
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public StringProperty titleProperty() {
        return header.titleProperty();
    }

    public String getTitle() {
        return titleProperty().get();
    }

    public void setTitle(String title) {
        titleProperty().set(title);
    }

    public StringProperty descriptionProperty() {
        return header.descriptionProperty();
    }

    public String getDescription() {
        return descriptionProperty().get();
    }

    public void setDescription(String description) {
        descriptionProperty().set(description);
    }

    public void setGraphic(Node node) {
        header.setGraphic(node);
    }

    public ObjectProperty<Node> actionProperty() {
        return header.actionProperty();
    }

    public Node getAction() {
        return (Node) header.actionProperty().get();
    }

    public void setAction(Node action) {
        header.actionProperty().set(action);
    }
}
