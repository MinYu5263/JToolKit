package com.minyu.jtoolkit.module.text_analyzer;

import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.module.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextAnalyzerController extends BaseController<TextAnalyzerPersistentState> {

    @FXML
    private EnhancedTextArea textArea;

    // 统计标签
    @FXML
    private Label lblTotalLength, lblLines, lblWords;
    @FXML
    private Label lblSelLength, lblSelStart, lblSelEnd;

    // 核心状态
    private String originalText = "";
    private boolean isProgrammaticChange = false; // 锁：是否为程序触发的修改

    @FXML
    public void initView() {


        textArea.textProperty().addListener((obs, old, val) -> {
            if (!isProgrammaticChange) {
                originalText = val;
            }
            updateStats();
            saveValues();
        });

        // 2. 选区变化监听
        textArea.selectionProperty().addListener((obs, old, val) -> updateSelectionStats());

        updateStats();
        updateSelectionStats();
    }

    // ================== 新增功能：显示原文 ==================

    @FXML
    public void onShowOriginal() {
        if (originalText != null) {
            // 还原也是一种"程序修改"，不应该覆盖 originalText 本身
            isProgrammaticChange = true;
            textArea.setText(originalText);
            isProgrammaticChange = false;
        }
    }

    // ================== 核心算法：替换文本 ==================

    /**
     * 执行转换操作 (会触发 isProgrammaticChange 锁)
     */
    private void replaceText(UnaryOperator<String> transformer) {
        IndexRange range = textArea.getSelection();
        boolean hasSelection = range.getLength() > 0;

        String source = hasSelection ? textArea.getSelectedText() : textArea.getText();
        if (source == null || source.isEmpty()) return;

        String result = transformer.apply(source);

        // === 加锁开始 ===
        isProgrammaticChange = true;
        try {
            if (hasSelection) {
                textArea.replaceSelection(result);
                textArea.selectRange(range.getStart(), range.getStart() + result.length());
            } else {
                textArea.setText(result);
            }
        } finally {
            // === 解锁 ===
            isProgrammaticChange = false;
        }
    }

    // ================== 转换 Action (保持不变) ==================

    @FXML
    public void toLowerCase() {
        replaceText(String::toLowerCase);
    }

    @FXML
    public void toUpperCase() {
        replaceText(String::toUpperCase);
    }

    @FXML
    public void toCamelCase() {
        replaceText(t -> joinWords(t, "", false));
    }

    @FXML
    public void toPascalCase() {
        replaceText(t -> joinWords(t, "", true));
    }

    @FXML
    public void toSnakeCase() {
        replaceText(t -> joinWords(t, "_", false).toLowerCase());
    }

    @FXML
    public void toConstantCase() {
        replaceText(t -> joinWords(t, "_", false).toUpperCase());
    }

    @FXML
    public void toKebabCase() {
        replaceText(t -> joinWords(t, "-", false).toLowerCase());
    }

    @FXML
    public void toTitleCase() {
        replaceText(text -> {
            Matcher m = Pattern.compile("\\b\\w").matcher(text.toLowerCase());
            StringBuilder sb = new StringBuilder();
            while (m.find()) m.appendReplacement(sb, m.group().toUpperCase());
            m.appendTail(sb);
            return sb.toString();
        });
    }

    @FXML
    public void toSentenceCase() {
        replaceText(text -> {
            if (text.isEmpty()) return "";
            String lower = text.toLowerCase();
            return lower.substring(0, 1).toUpperCase() + lower.substring(1);
        });
    }

    @FXML
    public void toAlternatingCase() {
        replaceText(text -> {
            StringBuilder sb = new StringBuilder();
            boolean upper = false;
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                    upper = !upper;
                } else sb.append(c);
            }
            return sb.toString();
        });
    }

    @FXML
    public void toInverseCase() {
        replaceText(text -> {
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isLowerCase(c)) sb.append(Character.toUpperCase(c));
                else if (Character.isUpperCase(c)) sb.append(Character.toLowerCase(c));
                else sb.append(c);
            }
            return sb.toString();
        });
    }

    // ================== 分词算法 (保持不变) ==================

    private String joinWords(String input, String delimiter, boolean capitalizeFirst) {
        List<String> words = splitToWords(input);
        if (words.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i).toLowerCase();
            if (i > 0) sb.append(delimiter);
            if (capitalizeFirst || (i > 0 && delimiter.isEmpty())) {
                if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            } else sb.append(w);
        }
        return sb.toString();
    }

    private List<String> splitToWords(String input) {
        List<String> words = new ArrayList<>();
        String[] parts = input.split("[\\s_\\-]+|(?<=[a-z])(?=[A-Z])");
        for (String part : parts) if (part != null && !part.isBlank()) words.add(part.trim());
        return words;
    }

    // ================== 统计更新 (保持不变) ==================

    private void updateStats() {
        String text = textArea.getText();
        if (text == null) text = "";
        lblTotalLength.setText(String.valueOf(text.length()));
        lblLines.setText(String.valueOf(text.isEmpty() ? 0 : text.split("\r\n|\r|\n", -1).length));
        lblWords.setText(String.valueOf(text.isBlank() ? 0 : text.trim().split("\\s+").length));
    }

    private void updateSelectionStats() {
        IndexRange range = textArea.getSelection();
        lblSelLength.setText(String.valueOf(range.getLength()));
        lblSelStart.setText(String.valueOf(range.getStart()));
        lblSelEnd.setText(String.valueOf(range.getEnd()));
    }

    // ================== 持久化 ==================

    @Override
    protected String getViewKey() {
        return "text_analyzer";
    }

    @Override
    protected void restoreValues(TextAnalyzerPersistentState state) {
        if (state != null) {
            // 恢复时也不应该视为用户输入，防止覆盖 originalText
            isProgrammaticChange = true;
            if (state.getText() != null) textArea.setText(state.getText());
            if (state.getOriginalText() != null) this.originalText = state.getOriginalText();
            isProgrammaticChange = false;
        }
    }

    @Override
    protected TextAnalyzerPersistentState captureValues() {
        TextAnalyzerPersistentState state = new TextAnalyzerPersistentState();
        state.setText(textArea.getText());
        state.setOriginalText(originalText);
        return state;
    }
}