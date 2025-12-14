package com.minyu.jtoolkit.module.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.ViewStateService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 格式化工具
 */
@Slf4j
@Component
public class JsonController extends BaseController<JsonViewState> {

    @FXML
    private TextArea inputArea;

    @FXML
    private TextArea outputArea;

    public JsonController(ViewStateService viewStateService) {
        super(viewStateService);
    }

    @FXML
    public void initialize() {
        super.loadState();
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
    protected String getStorageKey() {
        return "tool.json.formatter";
    }

    @Override
    protected Class<JsonViewState> getStateType() {
        return JsonViewState.class;
    }

    @Override
    protected void restoreUI(JsonViewState state) {
        inputArea.setText(state.getInputContent());
        outputArea.setText(state.getOutputContent());
    }

    @Override
    protected JsonViewState captureUI() {
        JsonViewState state = new JsonViewState();
        state.setInputContent(inputArea.getText());
        state.setOutputContent(outputArea.getText());
        return state;
    }
}