package com.minyu.jtoolkit.module.json;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

/**
 * JsonPersistentState
 */
@Data
public class JsonPersistentState implements PersistentState {
    /**
     * 输入区域的文本
     */
    private String inputContent;

    /**
     * 输出区域的文本
     */
    private String outputContent;

    /**
     * 缩进模式(默认2)
     */
    private String indentKey = "2space";

    /**
     * 是否为压缩模式
     */
    private boolean compactMode = false;
}
