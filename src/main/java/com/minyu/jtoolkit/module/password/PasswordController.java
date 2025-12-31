package com.minyu.jtoolkit.module.password;

import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.ViewDataService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordController extends BaseController<PasswordViewState> {

    @FXML
    private TextField passwordField;
    @FXML
    private Label msgLabel;

    @FXML
    private Slider lengthSlider;
    @FXML
    private Label lengthLabel;

    @FXML
    private CheckBox chkUpper;
    @FXML
    private CheckBox chkLower;
    @FXML
    private CheckBox chkDigits;
    @FXML
    private CheckBox chkSpecial;
    @FXML
    private TextField excludeField;

    // 字符常量定义
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    // 强随机数生成器
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordController(ViewDataService viewDataService) {
        super();
    }

    @FXML
    public void initView() {
        

        // 1. 长度滑块监听
        lengthLabel.setText(String.valueOf((int) lengthSlider.getValue()));
        lengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lengthLabel.setText(String.valueOf(newVal.intValue()));
            onGenerate(); // 拖动滑块实时生成
        });

        // 2. 选项变更监听
        super.observeChanges(
                chkUpper.selectedProperty(), chkLower.selectedProperty(),
                chkDigits.selectedProperty(), chkSpecial.selectedProperty(),
                excludeField.textProperty()
        );

        // 绑定所有触发生成的事件
        chkUpper.selectedProperty().addListener(e -> onGenerate());
        chkLower.selectedProperty().addListener(e -> onGenerate());
        chkDigits.selectedProperty().addListener(e -> onGenerate());
        chkSpecial.selectedProperty().addListener(e -> onGenerate());
        excludeField.textProperty().addListener(e -> onGenerate());

        // 3. 首次生成
        onGenerate();
    }

    @FXML
    public void onGenerate() {
        int length = (int) lengthSlider.getValue();
        StringBuilder poolBuilder = new StringBuilder();

        // 1. 构建基础池
        if (chkUpper.isSelected()) poolBuilder.append(UPPER);
        if (chkLower.isSelected()) poolBuilder.append(LOWER);
        if (chkDigits.isSelected()) poolBuilder.append(DIGITS);
        if (chkSpecial.isSelected()) poolBuilder.append(SPECIAL);

        String rawPool = poolBuilder.toString();

        // 2. 处理排除字符 (支持空格分隔，或者直接连写)
        String excludeStr = excludeField.getText();
        if (excludeStr != null && !excludeStr.isEmpty()) {
            // 去掉空格，把每个字符都当做要排除的对象
            String toExclude = excludeStr.replace(" ", "");
            for (char c : toExclude.toCharArray()) {
                rawPool = rawPool.replace(String.valueOf(c), "");
            }
        }

        // 3. 校验池子是否为空
        if (rawPool.isEmpty()) {
            passwordField.setText("");
            passwordField.setPromptText("错误：字符池为空，请检查选项");
            return;
        }

        // 4. 生成密码
        StringBuilder sb = new StringBuilder(length);
        char[] pool = rawPool.toCharArray();

        // 简单策略：直接从池中随机取 (对于极高安全要求，应确保每类至少出现一次，
        // 但对于日常使用，纯随机取在长度>8时概率上基本包含各类字符)
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(pool.length);
            sb.append(pool[index]);
        }

        // 5. (可选) 打乱结果 (虽然 SecureRandom 选出来的已经是乱的了，这步主要是心理安慰)
        // 只有当采用"强制每类字符取一个"策略时，shuffle 才有意义。
        // 这里采用纯随机策略，所以直接输出即可。

        passwordField.setText(sb.toString());
        msgLabel.setText(""); // 清空之前的提示
    }

    @FXML
    public void onCopy() {
        String pwd = passwordField.getText();
        if (pwd == null || pwd.isEmpty()) return;

        ClipboardContent content = new ClipboardContent();
        content.putString(pwd);
        Clipboard.getSystemClipboard().setContent(content);

        msgLabel.setText("已复制到剪贴板！");
        // 2秒后清除提示
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            javafx.application.Platform.runLater(() -> msgLabel.setText(""));
        }).start();
    }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "tool.password.generator";
    }

    @Override
    protected Class<PasswordViewState> getStorageType() {
        return PasswordViewState.class;
    }

    @Override
    protected void restoreValues(PasswordViewState state) {
        if (state == null) return;

        lengthSlider.setValue(state.getLength());
        chkUpper.setSelected(state.isUseUpper());
        chkLower.setSelected(state.isUseLower());
        chkDigits.setSelected(state.isUseDigits());
        chkSpecial.setSelected(state.isUseSpecial());
        if (state.getExcludeChars() != null) {
            excludeField.setText(state.getExcludeChars());
        }

        // 恢复完状态后，强制刷新一次
        onGenerate();
    }

    @Override
    protected PasswordViewState captureValues() {
        PasswordViewState state = new PasswordViewState();
        state.setLength((int) lengthSlider.getValue());
        state.setUseUpper(chkUpper.isSelected());
        state.setUseLower(chkLower.isSelected());
        state.setUseDigits(chkDigits.isSelected());
        state.setUseSpecial(chkSpecial.isSelected());
        state.setExcludeChars(excludeField.getText());
        return state;
    }
}