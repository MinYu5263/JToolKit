package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.Tile;
import javafx.beans.NamedArg;

/**
 * 基础配置卡片 (ConfigCard)
 * 用于展示单个配置项，包含标题、描述和右侧操作区。
 */
public class ConfigCard extends Tile {
    public static final String STYLE_CLASS = "config-card";

    public ConfigCard() {
        super();
        init();
    }

    public ConfigCard(@NamedArg("title") String title, @NamedArg("description") String description) {
        super(title, description);
        init();
    }

    private void init() {
        getStyleClass().add(STYLE_CLASS);
    }
}