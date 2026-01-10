package com.minyu.jtoolkit.module.json;

import atlantafx.base.controls.ToggleSwitch;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.module.BaseController;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON 格式化工具控制器
 */
@Slf4j
@Component
public class JsonController extends BaseController<JsonPersistentState> {

    @FXML
    private EnhancedTextArea inputArea;
    @FXML
    private EnhancedTextArea outputArea;
    // 定义配置项列表
    private final List<IndentOption> indentOptions = List.of(
            new IndentOption("2个空格", "2space", JSONWriter.Feature.PrettyFormatWith2Space),
            new IndentOption("4个空格", "4space", JSONWriter.Feature.PrettyFormatWith4Space),
            new IndentOption("1个制表符", "1tab", JSONWriter.Feature.PrettyFormat)
    );
    // 使用自定义 record 对象
    @FXML
    private ComboBox<IndentOption> indentCombo;
    @FXML
    private ToggleSwitch compactSwitch;
    private PauseTransition formatDebounce;

    @FXML
    public void initView() {
        initIndentCombo();

        // 防抖初始化
        formatDebounce = new PauseTransition(Duration.millis(300));
        formatDebounce.setOnFinished(e -> performFormat());

        // 监听变化
        inputArea.textProperty().addListener((o, old, val) -> formatDebounce.playFromStart());
        indentCombo.valueProperty().addListener((o, old, val) -> performFormat());
        compactSwitch.selectedProperty().addListener((o, old, val) -> performFormat());

        // 同步滚动
        inputArea.scrollTopProperty().bindBidirectional(outputArea.scrollTopProperty());
        inputArea.scrollLeftProperty().bindBidirectional(outputArea.scrollLeftProperty());
    }

    // 初始化下拉选 (参考你的 ThemeItem 写法)
    private void initIndentCombo() {
        indentCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(IndentOption item) {
                return item == null ? "" : item.label();
            }

            @Override
            public IndentOption fromString(String string) {
                return null;
            }
        });

        indentCombo.getItems().addAll(indentOptions);
        // 默认选中第一个 (2space)
        indentCombo.getSelectionModel().selectFirst();
    }

    private void performFormat() {
        String raw = inputArea.getText();
        if (raw == null || raw.isBlank()) {
            outputArea.clear();
            return;
        }

        try {
            Object obj = JSON.parse(raw);
            String result;

            if (compactSwitch.isSelected()) {
                result = JSON.toJSONString(obj);
            } else {
                // 直接从选中的对象里拿 Feature，无需任何 switch 判断
                IndentOption option = indentCombo.getValue();
                // 这里的判空是为了防止极端情况，理论上 initView 保证了不为空
                JSONWriter.Feature feature = (option != null) ? option.feature() : JSONWriter.Feature.PrettyFormatWith2Space;

                result = JSON.toJSONString(obj, feature);
            }
            outputArea.setText(result);
        } catch (Exception e) {
            outputArea.setText("JSON 格式错误: \n" + e.getMessage());
        }
    }

    @Override
    protected void restoreValues(JsonPersistentState state) {
        inputArea.setText(state.getInputContent());
        compactSwitch.setSelected(state.isCompactMode());

        // 根据保存的 key (英文) 还原下拉选状态
        String savedKey = state.getIndentKey();
        if (savedKey != null) {
            indentOptions.stream()
                    .filter(opt -> opt.key().equals(savedKey))
                    .findFirst()
                    .ifPresent(opt -> indentCombo.setValue(opt));
        }
    }

    @Override
    protected JsonPersistentState captureValues() {
        JsonPersistentState state = new JsonPersistentState();
        state.setInputContent(inputArea.getText());
        state.setOutputContent(outputArea.getText());
        state.setCompactMode(compactSwitch.isSelected());

        // 保存时只存 key (例如 "2space")
        if (indentCombo.getValue() != null) {
            state.setIndentKey(indentCombo.getValue().key());
        }
        return state;
    }

    @Override
    protected String getViewKey() {
        return "json_formatter";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                inputArea.textProperty(),
                outputArea.textProperty(),
                indentCombo.valueProperty(),
                compactSwitch.selectedProperty()
        );
    }

    private record IndentOption(String label, String key, JSONWriter.Feature feature) {
    }
}