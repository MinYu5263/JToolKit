package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.CustomTextField;

/**
 * EnhancedTextField
 */
public class EnhancedTextField extends EnhancedCustomInputBase<CustomTextField> {
    public EnhancedTextField() {
        super();
        this.getStyleClass().add("enhanced-text-field");
    }

    @Override
    protected CustomTextField createInputControl() {
        return new CustomTextField();
    }
}