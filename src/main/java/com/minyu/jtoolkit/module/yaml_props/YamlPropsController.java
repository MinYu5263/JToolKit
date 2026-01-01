package com.minyu.jtoolkit.module.yaml_props;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.minyu.jtoolkit.module.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Component
public class YamlPropsController extends BaseController<YamlPropsPersistentState> {

    @FXML
    private ToggleButton directionToggle;
    @FXML
    private Label leftLabel;
    @FXML
    private Label rightLabel;
    @FXML
    private TextArea sourceArea;
    @FXML
    private TextArea targetArea;
    @FXML
    private Label statusLabel;

    // Jackson Mappers
    private final JavaPropsMapper propsMapper;
    private final YAMLMapper yamlMapper;

    public YamlPropsController() {
        // 初始化 Mapper
        this.propsMapper = new JavaPropsMapper();

        // YAML 配置：去除文档开头的 ---，尽量不使用引号
        this.yamlMapper = new YAMLMapper();
        this.yamlMapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
        this.yamlMapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
    }

    @FXML
    public void initView() {
        

        // 监听输入，实时转换
        sourceArea.textProperty().addListener((obs, old, newVal) -> tryConvert());

        // 自动保存
        super.observeChanges(sourceArea.textProperty(), directionToggle.selectedProperty());
    }

    // ================== 核心转换逻辑 ==================

    private void tryConvert() {
        String input = sourceArea.getText();
        if (input == null || input.isBlank()) {
            targetArea.clear();
            statusLabel.setText("就绪");
            return;
        }

        boolean isPropToYaml = !directionToggle.isSelected(); // Toggle 未选中状态默认为 Prop -> Yaml

        try {
            String result;
            if (isPropToYaml) {
                // Properties -> YAML
                // 1. 读取 Props 为通用 JsonNode 树
                JsonNode node = propsMapper.readTree(input);
                // 2. 将树写入为 YAML
                result = yamlMapper.writeValueAsString(node);
            } else {
                // YAML -> Properties
                // 1. 读取 YAML 为通用 JsonNode 树
                JsonNode node = yamlMapper.readTree(input);
                // 2. 将树写入为 Properties (Jackson 会自动处理层级 . 连接)
                result = propsMapper.writeValueAsString(node);
            }

            targetArea.setText(result);
            statusLabel.setText("转换成功");
            statusLabel.setStyle("-fx-text-fill: green;");

        } catch (Exception e) {
            // 转换失败不清空，但在状态栏提示
            statusLabel.setText("格式错误: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ================== 交互事件 ==================

    @FXML
    public void onToggleDirection() {
        boolean isPropToYaml = !directionToggle.isSelected();
        updateLabels(isPropToYaml);

        // 交换内容（如果用户想反着转回去）
        String currentSource = sourceArea.getText();
        String currentTarget = targetArea.getText();

        if (!currentTarget.isEmpty()) {
            sourceArea.setText(currentTarget);
            // targetArea 会被监听器自动触发更新，不需要手动设
        } else {
            // 如果没结果，就只清空重新算
            tryConvert();
        }
    }

    private void updateLabels(boolean isPropToYaml) {
        if (isPropToYaml) {
            directionToggle.setText("Properties ➡ YAML");
            leftLabel.setText("Properties (Source)");
            rightLabel.setText("YAML (Result)");
        } else {
            directionToggle.setText("YAML ➡ Properties");
            leftLabel.setText("YAML (Source)");
            rightLabel.setText("Properties (Result)");
        }
    }

    @FXML
    public void onImportFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("导入配置文件");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config Files", "*.properties", "*.yml", "*.yaml"));
        File file = fc.showOpenDialog(sourceArea.getScene().getWindow());

        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                sourceArea.setText(content);

                // 智能识别：如果是 .yml 结尾，自动切到 Yaml->Prop 模式
                if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                    directionToggle.setSelected(true); // 选中 = YAML -> Properties
                    updateLabels(false);
                } else if (file.getName().endsWith(".properties")) {
                    directionToggle.setSelected(false);
                    updateLabels(true);
                }

            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "读取文件失败: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    public void onClear() {
        sourceArea.clear();
        targetArea.clear();
    }

    @FXML
    public void onCopyResult() {
        ClipboardContent content = new ClipboardContent();
        content.putString(targetArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("结果已复制");
    }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "tool.code.yaml_props";
    }

    @Override
    protected Class<YamlPropsPersistentState> getStorageType() {
        return YamlPropsPersistentState.class;
    }

    @Override
    protected void restoreValues(YamlPropsPersistentState state) {
        if (state == null) return;

        // 恢复方向
        directionToggle.setSelected(!state.isPropToYaml());
        updateLabels(state.isPropToYaml());

        // 恢复内容 (会触发监听器自动转换)
        if (state.getSourceText() != null) {
            sourceArea.setText(state.getSourceText());
        }
    }

    @Override
    protected YamlPropsPersistentState captureValues() {
        YamlPropsPersistentState state = new YamlPropsPersistentState();
        state.setSourceText(sourceArea.getText());
        state.setTargetText(targetArea.getText());
        state.setPropToYaml(!directionToggle.isSelected());
        return state;
    }
}