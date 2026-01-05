package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.CustomTextField;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

/**
 * EnhancedCustomInputBase
 */
public abstract class EnhancedCustomInputBase<T extends CustomTextField> extends EnhancedInputBase<T> {

    public EnhancedCustomInputBase() {
        super();
    }

    public final ObjectProperty<Node> leftProperty() { return inputControl.leftProperty(); }
    public final Node getLeft() { return inputControl.getLeft(); }
    public final void setLeft(Node node) { inputControl.setLeft(node); }

    public final ObjectProperty<Node> rightProperty() { return inputControl.rightProperty(); }
    public final Node getRight() { return inputControl.getRight(); }
    public final void setRight(Node node) { inputControl.setRight(node); }
}