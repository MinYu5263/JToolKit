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
    private boolean ignoreCase;
    private boolean multiline;
    private boolean dotAll;
}