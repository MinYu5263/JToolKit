package com.minyu.jtoolkit.module.yaml_props;

import lombok.Data;

@Data
public class YamlPropsViewState {
    private String sourceText;
    private String targetText;
    private boolean propToYaml = true;
}