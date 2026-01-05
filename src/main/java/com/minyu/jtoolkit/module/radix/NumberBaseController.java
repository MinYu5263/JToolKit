package com.minyu.jtoolkit.module.radix;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.core.component.EnhancedTextField;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class NumberBaseController extends BaseController<NumberPersistentState> {

    @FXML
    private ToggleSwitch formatSwitch;
    @FXML
    private Label statusLabel;

    @FXML
    private EnhancedTextField hexField;
    @FXML
    private EnhancedTextField decField;
    @FXML
    private EnhancedTextField octField;
    @FXML
    private EnhancedTextField binField;

    private boolean isUpdating = false;

    private static final BigInteger LIMIT_HEX = new BigInteger("576460752303423487");
    private static final BigInteger LIMIT_DEC = new BigInteger("922337203685477580");
    private static final BigInteger LIMIT_OCT = new BigInteger("1152921504606846975");
    private static final BigInteger LIMIT_BIN = new BigInteger("4611686018427387903");

    private static StringBuilder getStringBuilder(String raw, int radix) {
        StringBuilder sb = new StringBuilder();
        int len = raw.length();
        int count = 0;
        int step = (radix == 10) ? 3 : 4;
        String sep = (radix == 10) ? "," : " ";

        for (int i = len - 1; i >= 0; i--) {
            sb.append(raw.charAt(i));
            count++;
            if (count % step == 0 && i > 0) {
                sb.append(sep);
            }
        }
        return sb;
    }

    @FXML
    public void initView() {
        setupField(hexField, 16, LIMIT_HEX, "-?[0-9a-fA-F ]*");
        setupField(decField, 10, LIMIT_DEC, "-?[0-9,]*");
        setupField(octField, 8, LIMIT_OCT, "-?[0-7 ]*");
        setupField(binField, 2, LIMIT_BIN, "-?[01 ]*");

        formatSwitch.selectedProperty().addListener((obs, old, val) -> {
            String currentText = decField.getText();
            if (!currentText.isEmpty()) {
                updateOthers(currentText, 10, null);
            }
        });
    }

    private void setupField(EnhancedTextField enhancedField, int radix, BigInteger limit, String regex) {
        Pattern pattern = Pattern.compile(regex);

        enhancedField.setTextFormatter(new TextFormatter<>(change -> {
            if (isUpdating) return change;
            String newText = change.getControlNewText();
            if (!pattern.matcher(newText).matches()) return null;
            return change;
        }));

        enhancedField.textProperty().addListener((obs, old, val) -> {
            if (isUpdating) return;

            String cleanText = val.replaceAll("[,\\s_]", "");
            if (cleanText.isEmpty() || "-".equals(cleanText)) {
                statusLabel.setText("");
                clearOtherFields(enhancedField);
                return;
            }

            try {
                BigInteger value = new BigInteger(cleanText, radix);

                if (value.abs().compareTo(limit) > 0) {
                    showError(radix, limit);
                    clearOtherFields(enhancedField);
                } else {
                    statusLabel.setText("");
                    updateOthers(val, radix, enhancedField);
                }
            } catch (NumberFormatException e) {
                statusLabel.setText("");
            }
        });
    }

    // 逻辑辅助方法保持几乎不变
    private void showError(int radix, BigInteger limit) {
        String baseName = switch (radix) {
            case 16 -> "十六进制";
            case 10 -> "十进制";
            case 8 -> "八进制";
            case 2 -> "二进制";
            default -> "";
        };
        statusLabel.setText("当前值 " + baseName + " 无法转换，因为它超过了最大值(" + limit + ")");
    }

    private void updateOthers(String text, int radix, EnhancedTextField sourceControl) {
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
            // ignore
        }
    }

    private void updateFieldValues(BigInteger value, EnhancedTextField source) {
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

    private void clearOtherFields(EnhancedTextField except) {
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
                processStr = "0".repeat(4 - remainder) + processStr;
            }
        }
        StringBuilder sb = getStringBuilder(processStr, radix);
        String result = sb.reverse().toString();
        return isNegative ? "-" + result : result;
    }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "number_base";
    }

    @Override
    protected List<Observable> getObservables() {
        // 监听内部 TextField 的属性
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