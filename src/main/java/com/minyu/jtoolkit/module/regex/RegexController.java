package com.minyu.jtoolkit.module.regex;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.module.BaseController;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexController extends BaseController<RegexPersistentState> {

    // === UI 组件 ===
    @FXML private TextField regexField;
    @FXML private ComboBox<String> templateCombo;
    @FXML private Label statusLabel;
    @FXML private Label matchCountLabel;
    @FXML private StackPane editorContainer; // 对应 FXML 中的新容器

    // === 选项开关 ===
    @FXML private ToggleSwitch swGlobal;
    @FXML private ToggleSwitch swIgnoreCase;
    @FXML private ToggleSwitch swMultiline;
    @FXML private ToggleSwitch swDotAll;
    @FXML private ToggleSwitch swComments;
    @FXML private ToggleSwitch swUnicode;
    @FXML private ToggleSwitch swCanonEq;

    // === RichTextFX 核心组件 ===
    private CodeArea codeArea;

    // === 数据源 ===
    private final Map<String, String> regexTemplates = new LinkedHashMap<>();

    // CSS 类名 (必须在你的 CSS 文件中定义)
    private static final String MATCH_CLASS = "regex-match";

    public void initView() {
        initCodeArea();
        initTemplates();
        registerListeners();
    }

    /**
     * 初始化 CodeArea 并放入 StackPane
     */
    private void initCodeArea() {
        codeArea = new CodeArea();
        // 设置字体

        // 这里的 CSS 路径建议根据你的项目实际情况修改，或者在主 Application 中统一加载
        // codeArea.getStylesheets().add(getClass().getResource("/assets/styles/highlight.css").toExternalForm());

        // 使用 VirtualizedScrollPane 包裹以支持高性能滚动
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        editorContainer.getChildren().add(scrollPane);
    }

    private void initTemplates() {
        regexTemplates.put("Email 邮箱", "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
        regexTemplates.put("Mobile 手机号 (CN)", "^1[3-9]\\d{9}$");
        regexTemplates.put("Date (yyyy-MM-dd)", "\\d{4}-\\d{2}-\\d{2}");
        regexTemplates.put("IPv4", "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}");
        regexTemplates.put("Chinese 中文字符", "[\\u4e00-\\u9fa5]+");
        regexTemplates.put("ID Card 身份证", "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");

        templateCombo.getItems().addAll(regexTemplates.keySet());
        templateCombo.setOnAction(e -> {
            String selected = templateCombo.getValue();
            if (selected != null && regexTemplates.containsKey(selected)) {
                regexField.setText(regexTemplates.get(selected));
            }
        });
    }

    private void registerListeners() {
        // 核心：监听文本或正则变化 -> 触发高亮
        codeArea.textProperty().addListener((obs, old, val) -> computeHighlighting());
        regexField.textProperty().addListener((obs, old, val) -> computeHighlighting());

        // 监听所有开关
        List.of(swGlobal, swIgnoreCase, swMultiline, swDotAll, swComments, swUnicode, swCanonEq)
                .forEach(sw -> sw.selectedProperty().addListener(obs -> computeHighlighting()));
    }

    /**
     * 核心高亮逻辑
     */
    private void computeHighlighting() {
        String text = codeArea.getText();
        String regex = regexField.getText();

        matchCountLabel.setText("0 matches");

        // 判空
        if (text == null || text.isEmpty() || regex == null || regex.isEmpty()) {
            codeArea.clearStyle(0, text.length());
            updateStatus("Ready", "-color-fg-muted", "mdal-info");
            return;
        }

        try {
            // 1. 组装 Pattern
            int flags = 0;
            if (swIgnoreCase.isSelected()) flags |= Pattern.CASE_INSENSITIVE;
            if (swMultiline.isSelected()) flags |= Pattern.MULTILINE;
            if (swDotAll.isSelected()) flags |= Pattern.DOTALL;
            if (swComments.isSelected()) flags |= Pattern.COMMENTS;
            if (swCanonEq.isSelected()) flags |= Pattern.CANON_EQ;
            if (swUnicode.isSelected()) {
                flags |= Pattern.UNICODE_CHARACTER_CLASS;
                flags |= Pattern.UNICODE_CASE;
            }

            Pattern pattern = Pattern.compile(regex, flags);
            Matcher matcher = pattern.matcher(text);

            // 2. 计算样式区间
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            int lastEnd = 0;
            int count = 0;
            boolean global = swGlobal.isSelected();

            while (matcher.find()) {
                // 添加前面未匹配的文本段（无样式）
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);

                // 添加匹配到的文本段（高亮样式）
                spansBuilder.add(Collections.singleton(MATCH_CLASS), matcher.end() - matcher.start());

                lastEnd = matcher.end();
                count++;

                // 如果没开启全局匹配，只匹配第一个
                if (!global) break;
            }

            // 添加最后剩余的文本段
            spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);

            // 3. 应用样式 (必须在 JavaFX 线程，此处通常已经是 UI 线程)
            StyleSpans<Collection<String>> spans = spansBuilder.create();
            codeArea.setStyleSpans(0, spans);

            matchCountLabel.setText(count + " matches");
            updateStatus("Regex Valid", "-color-success-fg", "mdal-check_circle");

        } catch (PatternSyntaxException e) {
            // 正则语法错误，清除高亮并报错
            codeArea.clearStyle(0, text.length());
            updateStatus("Invalid Regex: " + e.getDescription(), "-color-danger-fg", "mdal-error");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(String msg, String colorCss, String iconLiteral) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + colorCss + ";");
        statusLabel.setGraphic(new FontIcon(iconLiteral));
    }

    // === 持久化实现 ===
    @Override
    protected String getViewKey() {
        return "regex_tester";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                regexField.textProperty(),
                codeArea.textProperty(),
                swGlobal.selectedProperty(),
                swIgnoreCase.selectedProperty(),
                swMultiline.selectedProperty(),
                swDotAll.selectedProperty(),
                swComments.selectedProperty(),
                swUnicode.selectedProperty(),
                swCanonEq.selectedProperty()
        );
    }

    @Override
    protected void restoreValues(RegexPersistentState state) {
        if (state == null) return;
        regexField.setText(state.getRegexPattern());

        // 恢复文本 (CodeArea)
        if (state.getSourceText() != null) {
            codeArea.replaceText(state.getSourceText());
        }

        swGlobal.setSelected(state.isGlobal());
        swIgnoreCase.setSelected(state.isIgnoreCase());
        swMultiline.setSelected(state.isMultiline());
        swDotAll.setSelected(state.isDotAll());
        swComments.setSelected(state.isComments());
        swUnicode.setSelected(state.isUnicode());
        swCanonEq.setSelected(state.isCanonEq());

        // 恢复后触发一次高亮
        Platform.runLater(this::computeHighlighting);
    }

    @Override
    protected RegexPersistentState captureValues() {
        return new RegexPersistentState(
                regexField.getText(),
                codeArea.getText(),
                swGlobal.isSelected(),
                swIgnoreCase.isSelected(),
                swMultiline.isSelected(),
                swDotAll.isSelected(),
                swComments.isSelected(),
                swUnicode.isSelected(),
                swCanonEq.isSelected()
        );
    }
}