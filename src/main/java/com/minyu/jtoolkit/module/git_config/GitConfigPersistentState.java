package com.minyu.jtoolkit.module.git_config;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GitConfigPersistentState
 */
@Data
public class GitConfigPersistentState implements PersistentState {
    private List<String> localRepoPaths = new ArrayList<>();
}
