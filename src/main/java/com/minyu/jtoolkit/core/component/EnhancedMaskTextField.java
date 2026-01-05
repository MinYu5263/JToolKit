package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.MaskTextField;
import javafx.beans.property.StringProperty;

/**
 * EnhancedMaskTextField
 */
public class EnhancedMaskTextField extends EnhancedCustomInputBase<MaskTextField> {
    public EnhancedMaskTextField() {
        super();
        this.getStyleClass().add("enhanced-mask-field");
    }

    @Override
    protected MaskTextField createInputControl() {
        return new MaskTextField();
    }

    public final StringProperty maskProperty() {
        return inputControl.maskProperty();
    }

    public final String getMask() {
        return inputControl.getMask();
    }

    public final void setMask(String mask) {
        inputControl.setMask(mask);
    }
}
