package com.minyu.jtoolkit.module.radix;

import com.minyu.jtoolkit.module.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
public class RadixController extends BaseController<RadixViewState> {

    @FXML private ToggleButton formatToggle;
    @FXML private TextField hexField;
    @FXML private TextField decField;
    @FXML private TextField octField;
    @FXML private TextArea binField;
    @FXML private Label statusLabel; // 错误提示 Label

    private boolean isUpdating = false;

    // 定义 Long 的最大值和最小值 (BigInteger 形式)
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

    @FXML
    public void initView() {
        

        hexField.textProperty().addListener((obs, old, val) -> handleInput(val, 16, hexField));
        decField.textProperty().addListener((obs, old, val) -> handleInput(val, 10, decField));
        octField.textProperty().addListener((obs, old, val) -> handleInput(val, 8, octField));
        binField.textProperty().addListener((obs, old, val) -> handleInput(val, 2, binField));

        super.observeChanges(decField.textProperty(), formatToggle.selectedProperty());
        updateToggleStyle();
    }

    private void handleInput(String text, int radix, TextInputControl sourceControl) {
        if (isUpdating) return;

        // 每次输入先清空错误提示
        statusLabel.setText("");

        if (text == null || text.isBlank()) {
            clearAll(sourceControl);
            return;
        }

        try {
            // 1. 清洗数据
            String cleanText = text.replaceAll("[,\\s_]", "");
            if (cleanText.isEmpty()) return;

            // 2. 解析为 BigInteger
            BigInteger value = new BigInteger(cleanText, radix);

            // 3. 【新增】检查范围是否超过 Long.MAX_VALUE
            if (value.compareTo(MAX_LONG) > 0 || value.compareTo(MIN_LONG) < 0) {
                statusLabel.setText("当前值 十进制 无法转换，因为它超过了最大值(" + Long.MAX_VALUE + ")");
                // 超过范围时，不更新其他输入框，避免误导
                return;
            }

            // 4. 更新其他输入框
            updateFields(value, sourceControl);

        } catch (NumberFormatException e) {
            // 忽略非法输入
        }
    }

    private void updateFields(BigInteger value, TextInputControl source) {
        isUpdating = true;
        try {
            boolean fmt = formatToggle.isSelected();
            if (source != hexField) hexField.setText(format(value.toString(16).toUpperCase(), 16, fmt));
            if (source != decField) decField.setText(format(value.toString(10), 10, fmt));
            if (source != octField) octField.setText(format(value.toString(8), 8, fmt));
            if (source != binField) binField.setText(format(value.toString(2), 2, fmt));
        } finally {
            isUpdating = false;
        }
    }

    private void clearAll(TextInputControl except) {
        isUpdating = true;
        if (except != hexField) hexField.clear();
        if (except != decField) decField.clear();
        if (except != octField) octField.clear();
        if (except != binField) binField.clear();
        statusLabel.setText("");
        isUpdating = false;
    }

    private String format(String raw, int radix, boolean enable) {
        if (!enable) return raw;
        StringBuilder sb = new StringBuilder();
        int len = raw.length();

        if (radix == 10) {
            int count = 0;
            for (int i = len - 1; i >= 0; i--) {
                sb.append(raw.charAt(i));
                count++;
                if (count % 3 == 0 && i > 0 && raw.charAt(i-1) != '-') sb.append(",");
            }
            return sb.reverse().toString();
        } else {
            int count = 0;
            for (int i = len - 1; i >= 0; i--) {
                sb.append(raw.charAt(i));
                count++;
                if (count % 4 == 0 && i > 0) sb.append(" ");
            }
            return sb.reverse().toString();
        }
    }

    @FXML
    public void onToggleFormat() {
        boolean selected = formatToggle.isSelected();
        formatToggle.setText(selected ? "ON" : "OFF");
        updateToggleStyle();
        // 强制刷新
        handleInput(decField.getText(), 10, null);
    }

    private void updateToggleStyle() {
        if (formatToggle.isSelected()) {
            formatToggle.setStyle("-fx-base: #2e8b57; -fx-text-fill: white;");
        } else {
            formatToggle.setStyle("-fx-base: #e0e0e0; -fx-text-fill: black;");
        }
    }

    @FXML public void onClear() { clearAll(null); }

    private void copy(String text) {
        if (text == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML public void onCopyHex() { copy(hexField.getText()); }
    @FXML public void onCopyDec() { copy(decField.getText()); }
    @FXML public void onCopyOct() { copy(octField.getText()); }
    @FXML public void onCopyBin() { copy(binField.getText()); }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "tool.code.radix";
    }

    @Override
    protected Class<RadixViewState> getStorageType() {
        return RadixViewState.class;
    }

    @Override
    protected void restoreValues(RadixViewState state) {
        if (state == null) return;

        formatToggle.setSelected(state.isFormatEnabled());
        updateToggleStyle();

        if (state.getDecimalValue() != null) {
            decField.setText(state.getDecimalValue());
            // 如果开启了格式化，手动触发刷新
            if (state.isFormatEnabled()) {
                handleInput(state.getDecimalValue(), 10, null);
            }
        }
    }

    @Override
    protected RadixViewState captureValues() {
        RadixViewState state = new RadixViewState();
        String cleanDec = decField.getText().replaceAll("[,\\s]", "");
        state.setDecimalValue(cleanDec);
        state.setFormatEnabled(formatToggle.isSelected());
        return state;
    }
}