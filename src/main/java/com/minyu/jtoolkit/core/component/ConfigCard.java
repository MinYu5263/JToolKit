package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.Tile;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

/**
 * 基础配置卡片 (ConfigCard)
 * 用于展示单个配置项，包含标题、描述和右侧操作区。
 */
@DefaultProperty("action")
public class ConfigCard extends Tile {
    public static final String STYLE_CLASS = "config-card";

    public ConfigCard() {
        super();
        getStyleClass().add(STYLE_CLASS);
    }

    @Override
    public ObjectProperty<Node> actionProperty() {
        return super.actionProperty();
    }

    @Override
    public Node getAction() {
        return super.getAction();
    }

    @Override
    public void setAction(Node action) {
        super.setAction(action);
    }
}