package com.minyu.jtoolkit.module.text_formatter;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

/**
 * 文本格式化持久化状态 — JSON / XML 各自独立保存内容与设置
 */
@Data
public class TextFormatterPersistentState implements PersistentState {
    /** 当前选择的格式类型: "JSON" 或 "XML" */
    private String currentFormat = "JSON";

    // ── JSON 状态 ──
    private String jsonInputContent;
    private String jsonOutputContent;
    /** 缩进标签，如 "2个空格" */
    private String jsonIndentLabel = "2个空格";
    private boolean jsonCompactMode = false;

    // ── XML 状态 ──
    private String xmlInputContent;
    private String xmlOutputContent;
    /** 缩进标签，如 "2个空格" */
    private String xmlIndentLabel = "2个空格";
    private boolean xmlCompactMode = false;
}
