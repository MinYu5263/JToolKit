package com.minyu.jtoolkit.module.password;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
public class PasswordController extends BaseController<PasswordPersistentState> {
    @FXML
    private Spinner<Integer> lengthSpinner;
    @FXML
    private ToggleSwitch lowerSwitch;
    @FXML
    private ToggleSwitch upperSwitch;
    @FXML
    private ToggleSwitch digitsSwitch;
    @FXML
    private ToggleSwitch specialSwitch;
    @FXML
    private TextField excludeField; // 排除字符输入框

    @FXML
    private Spinner<Integer> quantitySpinner; // 新增：生成数量控制
    @FXML
    private EnhancedTextArea resultArea;

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private final SecureRandom secureRandom = new SecureRandom();

    private boolean isRestoring = false;

    @FXML
    public void initView() {
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));

        lengthSpinner.valueProperty().addListener((obs, old, val) -> tryGenerate());
        quantitySpinner.valueProperty().addListener((obs, old, val) -> tryGenerate());

        lowerSwitch.selectedProperty().addListener((obs, old, val) -> tryGenerate());
        upperSwitch.selectedProperty().addListener((obs, old, val) -> tryGenerate());
        digitsSwitch.selectedProperty().addListener((obs, old, val) -> tryGenerate());
        specialSwitch.selectedProperty().addListener((obs, old, val) -> tryGenerate());

        excludeField.textProperty().addListener((obs, old, val) -> tryGenerate());
    }

    /**
     * 点击“生成密码”按钮时调用
     */
    @FXML
    public void onGenerate() {
        tryGenerate();
    }

    private void tryGenerate() {
        if (isRestoring) return;

        StringBuilder poolBuilder = new StringBuilder();
        if (upperSwitch.isSelected()) poolBuilder.append(UPPER);
        if (lowerSwitch.isSelected()) poolBuilder.append(LOWER);
        if (digitsSwitch.isSelected()) poolBuilder.append(DIGITS);
        if (specialSwitch.isSelected()) poolBuilder.append(SPECIAL);

        String rawPool = poolBuilder.toString();

        String excludeStr = excludeField.getText();
        if (excludeStr != null && !excludeStr.isEmpty()) {
            String toExclude = excludeStr.replace(" ", ""); // 允许用户用空格分隔
            for (char c : toExclude.toCharArray()) {
                rawPool = rawPool.replace(String.valueOf(c), "");
            }
        }

        if (rawPool.isEmpty()) {
            resultArea.setText("错误：字符池为空，请至少选择一种字符类型或减少排除项。");
            return;
        }

        int length = lengthSpinner.getValue();
        int quantity = quantitySpinner.getValue();
        char[] pool = rawPool.toCharArray();

        StringBuilder finalOutput = new StringBuilder();

        for (int k = 0; k < quantity; k++) {
            StringBuilder singlePwd = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = secureRandom.nextInt(pool.length);
                singlePwd.append(pool[index]);
            }

            finalOutput.append(singlePwd);

            if (k < quantity - 1) {
                finalOutput.append("\n");
            }
        }

        resultArea.setText(finalOutput.toString());
    }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "password_generator";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                lengthSpinner.valueProperty(),
                quantitySpinner.valueProperty(),
                lowerSwitch.selectedProperty(),
                upperSwitch.selectedProperty(),
                digitsSwitch.selectedProperty(),
                specialSwitch.selectedProperty(),
                excludeField.textProperty()
        );
    }

    @Override
    protected void restoreValues(PasswordPersistentState state) {
        if (state == null) return;

        isRestoring = true;
        try {
            if (lengthSpinner.getValueFactory() != null)
                lengthSpinner.getValueFactory().setValue(state.getLength());

            if (quantitySpinner.getValueFactory() != null)
                quantitySpinner.getValueFactory().setValue(state.getQuantity());

            lowerSwitch.setSelected(state.isUseLower());
            upperSwitch.setSelected(state.isUseUpper());
            digitsSwitch.setSelected(state.isUseDigits());
            specialSwitch.setSelected(state.isUseSpecial());

            if (state.getExcludeChars() != null) {
                excludeField.setText(state.getExcludeChars());
            }
        } finally {
            isRestoring = false;
        }
        tryGenerate();
    }

    @Override
    protected PasswordPersistentState captureValues() {
        PasswordPersistentState state = new PasswordPersistentState();
        state.setLength(lengthSpinner.getValue());
        state.setQuantity(quantitySpinner.getValue());
        state.setUseLower(lowerSwitch.isSelected());
        state.setUseUpper(upperSwitch.isSelected());
        state.setUseDigits(digitsSwitch.isSelected());
        state.setUseSpecial(specialSwitch.isSelected());
        state.setExcludeChars(excludeField.getText());
        return state;
    }
}