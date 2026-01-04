package com.minyu.jtoolkit.module.radix;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class NumberBaseController extends BaseController<NumberPersistentState> {

    @FXML private ToggleSwitch formatSwitch;
    @FXML private TextField hexField;
    @FXML private TextField decField;
    @FXML private TextField octField;
    @FXML private TextField binField;
    @FXML private Label statusLabel;

    // 这是一个非常关键的标志位
    private boolean isUpdating = false;

    // 严格按照要求的最大值常量
    private static final BigInteger LIMIT_HEX = new BigInteger("576460752303423487");
    private static final BigInteger LIMIT_DEC = new BigInteger("922337203685477580");
    private static final BigInteger LIMIT_OCT = new BigInteger("1152921504606846975");
    private static final BigInteger LIMIT_BIN = new BigInteger("4611686018427387903");

    @FXML
    public void initView() {
        // 配置每个输入框
        setupField(hexField, 16, LIMIT_HEX, "-?[0-9a-fA-F ]*");
        setupField(decField, 10, LIMIT_DEC, "-?[0-9,]*");
        setupField(octField, 8, LIMIT_OCT, "-?[0-7 ]*");
        setupField(binField, 2, LIMIT_BIN, "-?[01 ]*");

        // 格式化开关监听
        formatSwitch.selectedProperty().addListener((obs, old, val) -> {
            if (!decField.getText().isEmpty()) {
                updateOthers(decField.getText(), 10, null);
            }
        });
    }

    private void setupField(TextField field, int radix, BigInteger limit, String regex) {
        Pattern pattern = Pattern.compile(regex);

        // 1. 安装 TextFormatter (负责拦截非法输入和超限判断)
        field.setTextFormatter(new TextFormatter<>(change -> {
            // 【关键修复】如果是程序正在自动更新其他框，直接放行，不走校验逻辑
            // 否则二进制输入的大数（合法）会被八进制（认为不合法）给拦截并报错
            if (isUpdating) {
                return change;
            }

            String newText = change.getControlNewText();

            // 规则：非法字符正则校验
            if (!pattern.matcher(newText).matches()) {
                return null;
            }

            // 处理空值或中间状态
            String cleanText = newText.replaceAll("[,\\s_]", "");
            if (cleanText.isEmpty() || "-".equals(cleanText)) {
                statusLabel.setText("");
                return change;
            }

            try {
                BigInteger value = new BigInteger(cleanText, radix);
                BigInteger absValue = value.abs();

                // 规则：超过当前输入框设定的最大值
                if (absValue.compareTo(limit) > 0) {
                    // 触发副作用：清空其他框 + 报错 + 拒绝本次输入
                    clearOtherFields(field);
                    showError(radix, limit);
                    return null;
                }

                // 输入合法，清除之前的报错
                statusLabel.setText("");
                return change;

            } catch (NumberFormatException e) {
                return null;
            }
        }));

        // 2. 安装 Listener (负责触发更新)
        field.textProperty().addListener((obs, old, val) -> {
            if (isUpdating) return;
            updateOthers(val, radix, field);
        });
    }

    private void showError(int radix, BigInteger limit) {
        String msg = switch (radix) {
            case 16 -> "当前值 十六进制 无法转换，因为它超过了最大值(" + limit + ")";
            case 10 -> "当前值 十进制 无法转换，因为它超过了最大值(" + limit + ")";
            case 8 -> "当前值 八进制 无法转换，因为它超过了最大值(" + limit + ")";
            case 2 -> "当前值二进制 无法转换，因为它超过了最大值(" + limit + ")";
            default -> "";
        };
        statusLabel.setText(msg);
    }

    private void updateOthers(String text, int radix, TextInputControl sourceControl) {
        if (isUpdating) return;

        if (text == null || text.isBlank() || "-".equals(text.replaceAll("\\s", ""))) {
            clearOtherFields(sourceControl);
            return;
        }

        try {
            String cleanText = text.replaceAll("[,\\s_]", "");
            BigInteger value = new BigInteger(cleanText, radix);
            updateFieldValues(value, sourceControl);
        } catch (Exception e) {
            // 忽略
        }
    }

    private void updateFieldValues(BigInteger value, TextInputControl source) {
        // 开启更新锁，防止 TextFormatter 误判
        isUpdating = true;
        try {
            boolean fmt = formatSwitch.isSelected();
            if (source != hexField) hexField.setText(format(value.toString(16).toUpperCase(), 16, fmt));
            if (source != decField) decField.setText(format(value.toString(10), 10, fmt));
            if (source != octField) octField.setText(format(value.toString(8), 8, fmt));
            if (source != binField) binField.setText(format(value.toString(2), 2, fmt));
        } finally {
            isUpdating = false;
        }
    }

    private void clearOtherFields(TextInputControl except) {
        if (isUpdating) return;
        isUpdating = true;
        try {
            if (except != hexField) hexField.clear();
            if (except != decField) decField.clear();
            if (except != octField) octField.clear();
            if (except != binField) binField.clear();
        } finally {
            isUpdating = false;
        }
    }

    private String format(String raw, int radix, boolean enable) {
        if (!enable || raw == null) return raw;

        boolean isNegative = raw.startsWith("-");
        String processStr = isNegative ? raw.substring(1) : raw;

        if (radix == 2) {
            int remainder = processStr.length() % 4;
            if (remainder != 0) {
                int padCount = 4 - remainder;
                processStr = "0".repeat(padCount) + processStr;
            }
        }

        StringBuilder sb = getStringBuilder(processStr, radix);
        String result = sb.reverse().toString();
        return isNegative ? "-" + result : result;
    }

    private static @NonNull StringBuilder getStringBuilder(String raw, int radix) {
        StringBuilder sb = new StringBuilder();
        int len = raw.length();
        int count = 0;

        if (radix == 10) {
            for (int i = len - 1; i >= 0; i--) {
                sb.append(raw.charAt(i));
                count++;
                if (count % 3 == 0 && i > 0) {
                    sb.append(",");
                }
            }
        } else {
            for (int i = len - 1; i >= 0; i--) {
                sb.append(raw.charAt(i));
                count++;
                if (count % 4 == 0 && i > 0) {
                    sb.append(" ");
                }
            }
        }
        return sb;
    }

    // ================== 按钮操作区域 ==================

    private void pasteTo(TextField target) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasContent(DataFormat.PLAIN_TEXT)) {
            String content = clipboard.getString();
            if (content != null) {
                target.setText(content);
            }
        }
    }

    @FXML public void onPasteHex() { pasteTo(hexField); }
    @FXML public void onPasteDec() { pasteTo(decField); }
    @FXML public void onPasteOct() { pasteTo(octField); }
    @FXML public void onPasteBin() { pasteTo(binField); }

    private void copy(String text) {
        if (text == null || text.isEmpty()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("已复制");
    }

    @FXML public void onCopyHex() { copy(hexField.getText()); }
    @FXML public void onCopyDec() { copy(decField.getText()); }
    @FXML public void onCopyOct() { copy(octField.getText()); }
    @FXML public void onCopyBin() { copy(binField.getText()); }

    @FXML public void onClearHex() { hexField.clear(); }
    @FXML public void onClearDec() { decField.clear(); }
    @FXML public void onClearOct() { octField.clear(); }
    @FXML public void onClearBin() { binField.clear(); }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "number_base";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(decField.textProperty(), formatSwitch.selectedProperty());
    }

    @Override
    protected void restoreValues(NumberPersistentState state) {
        if (state == null) return;
        formatSwitch.setSelected(state.isFormatEnabled());
        if (state.getDecimalValue() != null && !state.getDecimalValue().isEmpty()) {
            decField.setText(state.getDecimalValue());
        }
    }

    @Override
    protected NumberPersistentState captureValues() {
        NumberPersistentState state = new NumberPersistentState();
        String cleanDec = decField.getText().replaceAll("[,\\s]", "");
        state.setDecimalValue(cleanDec);
        state.setFormatEnabled(formatSwitch.isSelected());
        return state;
    }
}