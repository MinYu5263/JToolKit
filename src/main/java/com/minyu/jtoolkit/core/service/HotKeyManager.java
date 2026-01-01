package com.minyu.jtoolkit.core.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.springframework.stereotype.Service;

/**
 * HotKeyManager
 */
@Service
public class HotKeyManager {
    private final ObservableMap<String, KeyCombination> keyMap = FXCollections.observableHashMap();

    public void updateShortcut(String actionKey, String keyCombinationStr) {
        if (keyCombinationStr == null || keyCombinationStr.isBlank()) {
            keyMap.remove(actionKey);
            return;
        }
        try {
            KeyCombination combo = KeyCombination.valueOf(keyCombinationStr);
            keyMap.put(actionKey, combo);
        } catch (IllegalArgumentException e) {
            // 记录日志，忽略无效的配置
        }
    }

    public ObservableMap<String, KeyCombination> getShortcuts() {
        return keyMap;
    }

    public boolean isMatch(String actionKey, KeyEvent event) {
        KeyCombination combo = keyMap.get(actionKey);
        return combo != null && combo.match(event);
    }

    public String getDisplayText(String actionKey) {
        KeyCombination combo = keyMap.get(actionKey);
        return combo != null ? combo.getDisplayText() : "";
    }
}
