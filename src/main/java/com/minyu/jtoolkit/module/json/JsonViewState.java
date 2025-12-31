package com.minyu.jtoolkit.module.json;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

/**
 * JsonViewState
 */
@Data
public class JsonViewState implements ViewState {
    /**
     * 输入区域的文本
     */
    private String inputContent;

    /**
     * 输出区域的文本
     */
    private String outputContent;
}
