package com.minyu.jtoolkit.core.component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * EnhancedTextArea
 */
public class EnhancedTextArea extends EnhancedInputBase<TextArea> {

    private final StringProperty areaStyle = new SimpleStringProperty(this, "areaStyle", "");

    public EnhancedTextArea() {
        super();
        this.getStyleClass().add("enhanced-text-area");

        areaStyle.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (oldVal != null && !oldVal.isEmpty()) {
                    inputControl.getStyleClass().remove(oldVal);
                }
                inputControl.getStyleClass().add(newVal);
            }
        });
    }

    @Override
    protected TextArea createInputControl() {
        TextArea area = new TextArea();
        VBox.setVgrow(area, Priority.ALWAYS);
        return area;
    }

    public final StringProperty areaStyleProperty() {
        return areaStyle;
    }

    public final String getAreaStyle() {
        return areaStyle.get();
    }

    public final void setAreaStyle(String areaStyle) {
        this.areaStyle.set(areaStyle);
    }

    public final DoubleProperty scrollLeftProperty() {
        return inputControl.scrollLeftProperty();
    }

    public final DoubleProperty scrollTopProperty() {
        return inputControl.scrollTopProperty();
    }

    public final void bindBidirectionalScrollAll(EnhancedTextArea enhancedTextArea) {
        inputControl.scrollTopProperty().bindBidirectional(enhancedTextArea.scrollTopProperty());
        inputControl.scrollLeftProperty().bindBidirectional(enhancedTextArea.scrollLeftProperty());
    }
}