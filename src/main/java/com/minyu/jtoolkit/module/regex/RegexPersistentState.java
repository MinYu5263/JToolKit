package com.minyu.jtoolkit.module.regex;


import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegexPersistentState implements PersistentState {
    private String regexPattern;
    private String sourceText;

    // 选项
    private boolean isGlobal = true;
    private boolean isIgnoreCase;
    private boolean isMultiline;
    private boolean isDotAll;
    private boolean isComments;
    private boolean isUnicode;
    private boolean isCanonEq;
}