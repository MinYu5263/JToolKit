package com.minyu.jtoolkit.module.radix;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

@Data
public class RadixPersistentState implements PersistentState {
    private String decimalValue;
    private boolean formatEnabled;
}