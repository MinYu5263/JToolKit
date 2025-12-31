package com.minyu.jtoolkit.module.yaml_props;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

@Data
public class YamlPropsViewState implements ViewState {
    private String sourceText;
    private String targetText;
    private boolean propToYaml = true;
}