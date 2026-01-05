package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.CustomTextField;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

/**
 * EnhancedTextField
 */
public class EnhancedTextField extends EnhancedInputBase<CustomTextField> {

    public EnhancedTextField() {
        super();
        this.getStyleClass().add("enhanced-text-field");
    }

    @Override
    protected CustomTextField createInputControl() {
        return new CustomTextField();
    }

    // 代理方法
    public final ObjectProperty<Node> leftProperty() {
        return inputControl.leftProperty();
    }

    public final Node getLeft() {
        return inputControl.getLeft();
    }

    public final void setLeft(Node node) {
        inputControl.setLeft(node);
    }

    public final ObjectProperty<Node> rightProperty() {
        return inputControl.rightProperty();
    }

    public final Node getRight() {
        return inputControl.getRight();
    }

    public final void setRight(Node node) {
        inputControl.setRight(node);
    }
}