package com.minyu.jtoolkit.module.settings;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

/**
 * SettingsPersistentState
 */
@Data
public class SettingsPersistentState implements PersistentState {
    private String themeId;
    private Integer fontSize;
    private String searchShortcut;
}
