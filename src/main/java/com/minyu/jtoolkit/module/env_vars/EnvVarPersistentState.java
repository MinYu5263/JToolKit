package com.minyu.jtoolkit.module.env_vars;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * EnvVarPersistentState
 */
@Data
public class EnvVarPersistentState implements PersistentState {
    private List<EnvVarItemState> items = new ArrayList<>();

    private String lastSelectedFormat;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnvVarItemState {
        private String key;
        private String value;
    }
}
