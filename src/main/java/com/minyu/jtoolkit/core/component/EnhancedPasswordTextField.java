package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.PasswordTextField;
import javafx.beans.property.BooleanProperty;

/**
 * EnhancedPasswordTextField
 */
public class EnhancedPasswordTextField extends EnhancedCustomInputBase<PasswordTextField> {

    public EnhancedPasswordTextField() {
        super();
        this.getStyleClass().add("enhanced-password-field");
    }

    @Override
    protected PasswordTextField createInputControl() {
        return new PasswordTextField();
    }

    public final BooleanProperty revealPasswordProperty() {
        return inputControl.revealPasswordProperty();
    }

    public final boolean getRevealPassword() {
        return inputControl.getRevealPassword();
    }

    public final void setRevealPassword(boolean value) {
        inputControl.setRevealPassword(value);
    }
}
