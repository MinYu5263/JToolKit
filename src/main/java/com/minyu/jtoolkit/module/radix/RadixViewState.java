package com.minyu.jtoolkit.module.radix;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

@Data
public class RadixViewState implements ViewState {
    private String decimalValue;
    private boolean formatEnabled;
}