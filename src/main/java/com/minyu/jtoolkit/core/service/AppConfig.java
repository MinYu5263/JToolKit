package com.minyu.jtoolkit.core.service;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AppConfig
 */
@Data
public class AppConfig implements PersistentState {
    private String themeId;
    private Integer fontSize;
    private Map<String, String> shortcuts = new HashMap<>();
}
