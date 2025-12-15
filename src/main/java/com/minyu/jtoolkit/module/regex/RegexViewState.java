package com.minyu.jtoolkit.module.regex;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegexViewState {
    private String regexPattern;
    private String sourceText;
    private boolean ignoreCase;
    private boolean multiline;
    private boolean dotAll;
}