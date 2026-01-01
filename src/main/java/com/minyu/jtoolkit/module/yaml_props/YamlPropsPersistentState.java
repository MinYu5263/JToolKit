package com.minyu.jtoolkit.module.yaml_props;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

@Data
public class YamlPropsPersistentState implements PersistentState {
    private String sourceText;
    private String targetText;
    private boolean propToYaml = true;
}