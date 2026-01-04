package com.minyu.jtoolkit.module.xml;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

/**
 * XmlPersistentState
 */
@Data
public class XmlPersistentState implements PersistentState {
    private String inputContent;
    private String outputContent;

    // 默认使用 2 个空格
    private String indentKey = "2space";
    private boolean compactMode = false;
}
