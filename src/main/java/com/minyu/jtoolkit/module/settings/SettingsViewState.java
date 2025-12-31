package com.minyu.jtoolkit.module.settings;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

/**
 * SettingsViewState
 */
@Data
public class SettingsViewState implements ViewState {
    private String themeId;
    private Integer fontSize;
}
