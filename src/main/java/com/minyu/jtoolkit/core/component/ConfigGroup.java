package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.Tile;
import javafx.animation.RotateTransition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
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

    public ConfigGroup() {
        getStyleClass().add(STYLE_CLASS);

        FontIcon arrowIcon = new FontIcon(Material2AL.KEYBOARD_ARROW_DOWN);
        header.setAction(arrowIcon);
        header.setCursor(javafx.scene.Cursor.HAND);

        container.setFillWidth(true);
        container.getStyleClass().add("content-container");
        this.getChildren().addAll(header, container);

        rotateTransition = new RotateTransition(Duration.millis(200), arrowIcon);

        header.setOnMouseClicked(e -> setExpanded(!isExpanded()));

        expanded.addListener((obs, oldVal, newVal) -> updateState(newVal));

        updateState(true);
    }

    public ObservableList<Node> getItems() {
        return container.getChildren();
    }

    private void updateState(boolean isExpanded) {
        container.setVisible(isExpanded);
        container.setManaged(isExpanded);

        rotateTransition.setToAngle(isExpanded ? 180 : 0);
        rotateTransition.play();

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
}
