package com.minyu.jtoolkit.module.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.minyu.jtoolkit.module.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 格式化工具
 */
@Slf4j
@Component
public class JsonController extends BaseController<JsonPersistentState> {

    @FXML
    private TextArea inputArea;

    @FXML
    private TextArea outputArea;

    @FXML
    public void initView() {
        
        super.observeChanges(inputArea.textProperty(), outputArea.textProperty());
    }

    @FXML
    public void onFormat() {
        try {
            String raw = inputArea.getText();
            if (raw == null || raw.isBlank()) return;

            Object obj = JSON.parse(raw);
            String pretty = JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
            outputArea.setText(pretty);
        } catch (Exception e) {
            outputArea.setText("解析错误: " + e.getMessage());
        }
    }

    @FXML
    public void onCompact() {
        try {
            String raw = inputArea.getText();
            if (raw == null || raw.isBlank()) return;

            Object obj = JSON.parse(raw);
            outputArea.setText(JSON.toJSONString(obj));
        } catch (Exception e) {
            outputArea.setText("解析错误: " + e.getMessage());
        }
    }

    @FXML
    public void onClear() {
        inputArea.clear();
        outputArea.clear();
    }

    @Override
    protected String getViewKey() {
        return "json_formatter";
    }

    @Override
    protected Class<JsonPersistentState> getStorageType() {
        return JsonPersistentState.class;
    }

    @Override
    protected void restoreValues(JsonPersistentState state) {
        inputArea.setText(state.getInputContent());
        outputArea.setText(state.getOutputContent());
    }

    @Override
    protected JsonPersistentState captureValues() {
        JsonPersistentState state = new JsonPersistentState();
        state.setInputContent(inputArea.getText());
        state.setOutputContent(outputArea.getText());
        return state;
    }
}