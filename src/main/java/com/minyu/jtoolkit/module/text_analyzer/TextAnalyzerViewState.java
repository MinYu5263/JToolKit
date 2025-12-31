package com.minyu.jtoolkit.module.text_analyzer;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

@Data
public class TextAnalyzerViewState implements ViewState {
    private String text;
    private String originalText;
}