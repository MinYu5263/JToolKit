package com.minyu.jtoolkit.module.text_analyzer;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

@Data
public class TextAnalyzerPersistentState implements PersistentState {
    private String text;
    private String originalText;
}