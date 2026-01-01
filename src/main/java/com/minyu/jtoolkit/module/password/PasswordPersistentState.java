package com.minyu.jtoolkit.module.password;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

@Data
public class PasswordPersistentState implements PersistentState {
    private int length = 16;
    private boolean useUpper = true;
    private boolean useLower = true;
    private boolean useDigits = true;
    private boolean useSpecial = true;
    private String excludeChars;
}