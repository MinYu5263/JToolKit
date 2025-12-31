package com.minyu.jtoolkit.module.password;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

@Data
public class PasswordViewState implements ViewState {
    private int length = 16;
    private boolean useUpper = true;
    private boolean useLower = true;
    private boolean useDigits = true;
    private boolean useSpecial = true;
    private String excludeChars;
}