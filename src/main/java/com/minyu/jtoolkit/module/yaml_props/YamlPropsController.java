package com.minyu.jtoolkit.module.yaml_props;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class YamlPropsController extends BaseController<YamlPropsPersistentState> {

    // 当前转换方向状态
    private final BooleanProperty propToYaml = new SimpleBooleanProperty(true);
    @FXML
    private EnhancedTextArea sourceInput;

    // Jackson Mappers
    private final JavaPropsMapper propsMapper;
    private final YAMLMapper yamlMapper;
    @FXML
    private EnhancedTextArea targetOutput;
    // 防止在恢复数据(restoreValues)时触发监听器里的“内容交换”逻辑
    private boolean isRestoring = false;

    public YamlPropsController() {
        this.propsMapper = new JavaPropsMapper();
        this.yamlMapper = new YAMLMapper();
        this.yamlMapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
        this.yamlMapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
    }

    @FXML
    public void initView() {
        propToYaml.addListener((obs, oldVal, isPropToYaml) -> {
            updateUiText(isPropToYaml);
            if (StringUtils.isNotBlank(sourceInput.getText()) && !isRestoring) {
                sourceInput.setText(targetOutput.getText());
            }
        });
        sourceInput.textProperty().addListener((obs, old, newVal) -> tryConvert());
    }

    // ================== 核心转换逻辑 ==================

    private void tryConvert() {
        String input = sourceInput.getText();

        // 空值处理
        if (input == null || input.isBlank()) {
            targetOutput.setText("");
            return;
        }

        try {
            String result;
            if (propToYaml.get()) {
                JsonNode node = propsMapper.readTree(input);
                result = yamlMapper.writeValueAsString(node);
            } else {
                JsonNode node = yamlMapper.readTree(input);
                result = propsMapper.writeValueAsString(node);
            }
            targetOutput.setText(result);

        } catch (Exception e) {
            log.debug("Convert failed: {}", e.getMessage());
        }
    }

    // ================== 交互事件 ==================

    /**
     * 切换转换方向
     */
    @FXML
    public void onToggleDirection() {
        propToYaml.set(!propToYaml.get());
    }

    private void updateUiText(boolean isPropToYaml) {
        if (isPropToYaml) {
            sourceInput.setTitle("Properties");
            targetOutput.setTitle("YAML");
        } else {
            sourceInput.setTitle("YAML");
            targetOutput.setTitle("Properties");
        }
    }

    // ================== 持久化状态 ==================

    @Override
    protected String getViewKey() {
        return "yaml_props";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(sourceInput.textProperty(), propToYaml);
    }

    @Override
    protected void restoreValues(YamlPropsPersistentState state) {
        if (state == null) return;
        isRestoring = true;
        try {
            propToYaml.set(state.isPropToYaml());
            if (state.getSourceText() != null) {
                sourceInput.setText(state.getSourceText());
            }
        } finally {
            isRestoring = false;
        }
    }

    @Override
    protected void initDefaultValues() {
        sourceInput.setTitle("Properties");
        targetOutput.setTitle("YAML");

        isRestoring = true;
        try {
            propToYaml.set(true);
        } finally {
            isRestoring = false;
        }
    }

    @Override
    protected YamlPropsPersistentState captureValues() {
        YamlPropsPersistentState state = new YamlPropsPersistentState();
        state.setSourceText(sourceInput.getText());
        state.setTargetText(targetOutput.getText());
        state.setPropToYaml(propToYaml.get());
        return state;
    }
}