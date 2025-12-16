package com.minyu.jtoolkit.module.password;

import lombok.Data;

@Data
public class PasswordViewState {
    private int length = 16;
    private boolean useUpper = true;
    private boolean useLower = true;
    private boolean useDigits = true;
    private boolean useSpecial = true;
    private String excludeChars;
}