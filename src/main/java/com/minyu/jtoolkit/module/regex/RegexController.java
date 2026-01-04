package com.minyu.jtoolkit.module.regex;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.module.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexController extends BaseController<RegexPersistentState> {

    // === UI 组件 ===
    @FXML
    private TextField regexField;
    @FXML
    private ComboBox<String> templateCombo;
    @FXML
    private Label statusLabel;

    // 文本区域组件
    @FXML
    private TextArea sourceTextArea;
    @FXML
    private ScrollPane highlightScroll;
    @FXML
    private TextFlow highlightTextFlow;
    @FXML
    private ToggleButton btnToggleView; // 视图切换按钮
    @FXML
    private Label matchCountLabel;

    // 选项开关
    @FXML
    private ToggleSwitch swGlobal;
    @FXML
    private ToggleSwitch swIgnoreCase;
    @FXML
    private ToggleSwitch swMultiline;
    @FXML
    private ToggleSwitch swDotAll;
    @FXML
    private ToggleSwitch swComments;
    @FXML
    private ToggleSwitch swUnicode;
    @FXML
    private ToggleSwitch swCanonEq;

    // 数据源
    private final Map<String, String> regexTemplates = new LinkedHashMap<>();

    // 样式定义 (必须与 FXML 中的 TextArea 字体保持一致)
    private static final String FONT_STYLE = "-fx-font-family: 'JetBrains Mono', 'Consolas', 'Monospaced'; -fx-font-size: 14px;";
    // 匹配项样式：使用背景色模拟高亮 (Label 支持背景色，Text 不支持)
    private static final String MATCH_STYLE = FONT_STYLE + "-fx-text-fill: white; -fx-background-color: #264f78; -fx-background-radius: 2;";
    // 普通文本样式
    private static final String NORMAL_STYLE = FONT_STYLE + "-fx-fill: -color-fg-default;";

    public void initView() {
        initTemplates();
        initViewToggle();
        registerListeners();
        // 初始运行一次
        validateRegexOnly();
    }

    private void initTemplates() {
        // 初始化常用模板数据
        regexTemplates.put("Email 邮箱", "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
        regexTemplates.put("Mobile 手机号 (CN)", "^1[3-9]\\d{9}$");
        regexTemplates.put("Date (yyyy-MM-dd)", "\\d{4}-\\d{2}-\\d{2}");
        regexTemplates.put("IPv4", "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}");
        regexTemplates.put("Chinese 中文字符", "[\\u4e00-\\u9fa5]+");
        regexTemplates.put("ID Card 身份证", "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");

        // 填充下拉框
        templateCombo.getItems().addAll(regexTemplates.keySet());

        // 监听选择
        templateCombo.setOnAction(e -> {
            String selected = templateCombo.getValue();
            if (selected != null && regexTemplates.containsKey(selected)) {
                regexField.setText(regexTemplates.get(selected));
                // 如果当前在预览模式，自动触发更新
                if (btnToggleView.isSelected()) {
                    runRegex();
                } else {
                    validateRegexOnly();
                }
            }
        });
    }

    private void initViewToggle() {
        // 监听切换按钮：选中=预览模式(TextFlow)，未选中=编辑模式(TextArea)
        btnToggleView.selectedProperty().addListener((obs, oldVal, isPreview) -> {
            if (isPreview) {
                // -> 切换到预览：执行正则计算，显示 TextFlow
                runRegex();
                sourceTextArea.setVisible(false);
                highlightScroll.setVisible(true);
                btnToggleView.setText("返回编辑");
                btnToggleView.setGraphic(new FontIcon("mdal-edit"));
            } else {
                // -> 切换到编辑：显示 TextArea
                sourceTextArea.setVisible(true);
                highlightScroll.setVisible(false);
                btnToggleView.setText("预览高亮");
                btnToggleView.setGraphic(new FontIcon("mdmz-visibility"));
            }
        });
    }

    private void registerListeners() {
        // 自动保存 (仅存数据，不存UI临时状态)
        super.observeChanges(
                regexField.textProperty(),
                sourceTextArea.textProperty(),
                swGlobal.selectedProperty(),
                swIgnoreCase.selectedProperty(),
                swMultiline.selectedProperty(),
                swDotAll.selectedProperty(),
                swComments.selectedProperty(),
                swUnicode.selectedProperty(),
                swCanonEq.selectedProperty()
        );

        // 实时监听输入
        regexField.textProperty().addListener(e -> {
            if (btnToggleView.isSelected()) runRegex(); // 预览模式下实时渲染高亮
            else validateRegexOnly(); // 编辑模式下仅验证语法
        });

        // 选项变更立即刷新
        swGlobal.selectedProperty().addListener(e -> {
            if (btnToggleView.isSelected()) runRegex();
        });
        swIgnoreCase.selectedProperty().addListener(e -> {
            if (btnToggleView.isSelected()) runRegex();
        });
        // ... (其他开关同理，可根据需要添加)
    }

    /**
     * 仅验证正则语法并更新状态栏 (轻量级)
     */
    private void validateRegexOnly() {
        String patternStr = regexField.getText();
        if (patternStr == null || patternStr.isEmpty()) {
            updateStatus("Ready", "-color-fg-muted", "mdal-info");
            return;
        }
        try {
            Pattern.compile(patternStr);
            updateStatus("Regex Valid", "-color-success-fg", "mdal-check_circle");
        } catch (PatternSyntaxException e) {
            updateStatus("Invalid: " + e.getDescription(), "-color-danger-fg", "mdal-error");
        } catch (Exception e) {
            updateStatus("Error", "-color-fg-muted", "mdal-info");
        }
    }

    /**
     * 执行正则匹配并渲染 TextFlow (重量级)
     */
    private void runRegex() {
        String patternStr = regexField.getText();
        String source = sourceTextArea.getText();

        // 清空旧内容
        highlightTextFlow.getChildren().clear();

        // 1. 验证语法
        if (patternStr == null || patternStr.isEmpty()) {
            updateStatus("Please input regex", "-color-fg-muted", "mdal-info");
            matchCountLabel.setText("0 matches");
            if (source != null) highlightTextFlow.getChildren().add(styledText(source, NORMAL_STYLE));
            return;
        }

        // 2. 执行匹配与高亮渲染
        try {
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

            Pattern pattern = Pattern.compile(patternStr, flags);
            Matcher matcher = pattern.matcher(source == null ? "" : source);

            int count = 0;
            boolean global = swGlobal.isSelected();
            int lastEnd = 0;

            while (matcher.find()) {
                count++;

                // A. 添加未匹配部分 (普通文本)
                if (matcher.start() > lastEnd) {
                    String normalText = source.substring(lastEnd, matcher.start());
                    highlightTextFlow.getChildren().add(styledText(normalText, NORMAL_STYLE));
                }

                // B. 添加匹配部分 (高亮)
                // 使用 Label 而不是 Text，因为 Label 支持设置背景色
                Label matchLabel = new Label(matcher.group());
                matchLabel.setStyle(MATCH_STYLE);

                // 添加 Tooltip 显示捕获组详情
                if (matcher.groupCount() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        sb.append("[").append(i).append("]: ").append(matcher.group(i)).append("\n");
                    }
                    matchLabel.setTooltip(new Tooltip(sb.toString().trim()));
                }
                highlightTextFlow.getChildren().add(matchLabel);

                lastEnd = matcher.end();

                if (!global) break;

                // 性能熔断：防止超大文本导致卡死
                if (count >= 5000) {
                    updateStatus("Matches truncated (5000+)", "-color-warning-fg", "mdmz-warning");
                    break;
                }
            }

            // C. 添加剩余文本
            if (source != null && lastEnd < source.length()) {
                highlightTextFlow.getChildren().add(styledText(source.substring(lastEnd), NORMAL_STYLE));
            }

            matchCountLabel.setText(count + " matches");
            updateStatus("Regex Valid", "-color-success-fg", "mdal-check_circle");

        } catch (PatternSyntaxException e) {
            updateStatus("Syntax Error: " + e.getDescription(), "-color-danger-fg", "mdal-error");
            matchCountLabel.setText("Error");
            // 出错时显示原文本
            if (source != null) highlightTextFlow.getChildren().add(styledText(source, NORMAL_STYLE));
        } catch (Exception e) {
            updateStatus("Error: " + e.getMessage(), "-color-danger-fg", "mdal-error");
        }
    }

    private Text styledText(String content, String style) {
        Text t = new Text(content);
        t.setStyle(style);
        return t;
    }

    private void updateStatus(String msg, String colorCss, String iconLiteral) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + colorCss + ";");
        statusLabel.setGraphic(new FontIcon(iconLiteral));
    }

    // === 持久化 (保持不变) ===
    @Override
    protected String getViewKey() {
        return "regex_tester";
    }

    @Override
    protected Class<RegexPersistentState> getStorageType() {
        return RegexPersistentState.class;
    }

    @Override
    protected void restoreValues(RegexPersistentState state) {
        if (state == null) return;
        regexField.setText(state.getRegexPattern());
        sourceTextArea.setText(state.getSourceText());

        swGlobal.setSelected(state.isGlobal());
        swIgnoreCase.setSelected(state.isIgnoreCase());
        swMultiline.setSelected(state.isMultiline());
        swDotAll.setSelected(state.isDotAll());
        swComments.setSelected(state.isComments());
        swUnicode.setSelected(state.isUnicode());
        swCanonEq.setSelected(state.isCanonEq());

        validateRegexOnly(); // 默认只验证语法
    }

    @Override
    protected RegexPersistentState captureValues() {
        return new RegexPersistentState(
                regexField.getText(),
                sourceTextArea.getText(),
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