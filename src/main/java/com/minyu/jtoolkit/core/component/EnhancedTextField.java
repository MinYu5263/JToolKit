package com.minyu.jtoolkit.core.component;

import javafx.scene.control.TextField;

/**
 * EnhancedTextField
 */
public class EnhancedTextField extends EnhancedInputBase<TextField> {

    public EnhancedTextField() {
        super();
        this.getStyleClass().add("enhanced-text-field");
    }

    @Override
    protected TextField createInputControl() {
        return new TextField();
    }
}