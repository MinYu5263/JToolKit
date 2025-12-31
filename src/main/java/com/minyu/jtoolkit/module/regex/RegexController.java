package com.minyu.jtoolkit.module.regex;

import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexController extends BaseController<RegexViewState> {

    // === UI 组件 ===
    @FXML private TextField regexField;
    @FXML private ComboBox<String> templateCombo;
    @FXML private CheckBox chkIgnoreCase;
    @FXML private CheckBox chkMultiline;
    @FXML private CheckBox chkDotAll;
    @FXML private Label statusLabel;

    @FXML private TextArea sourceTextArea;
    @FXML private Label matchCountLabel;

    @FXML private TableView<MatchItem> resultTable;
    @FXML private TableColumn<MatchItem, Number> colIndex;
    @FXML private TableColumn<MatchItem, String> colMatch;
    @FXML private TableColumn<MatchItem, Number> colStart;
    @FXML private TableColumn<MatchItem, Number> colEnd;
    @FXML private TableColumn<MatchItem, String> colGroups;

    // === 数据源 ===
    private final ObservableList<MatchItem> matchList = FXCollections.observableArrayList();
    private final Map<String, String> regexTemplates = new LinkedHashMap<>();

    public void initView() {
        initTemplates();
        initTable();

        // 1. 恢复状态


        // 2. 绑定监听 (实时计算 + 自动保存)
        registerListeners();

        // 3. 首次执行
        runRegex();
    }

    private void initTemplates() {
        // 预置常用正则
        regexTemplates.put("Email 邮箱", "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
        regexTemplates.put("Mobile 手机号 (CN)", "^1[3-9]\\d{9}$");
        regexTemplates.put("IPv4 地址", "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}");
        regexTemplates.put("Date (yyyy-MM-dd)", "\\d{4}-\\d{2}-\\d{2}");
        regexTemplates.put("UUID", "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
        regexTemplates.put("Chinese 中文字符", "[\\u4e00-\\u9fa5]");

        templateCombo.getItems().addAll(regexTemplates.keySet());

        // 选择模板后自动填充
        templateCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && regexTemplates.containsKey(newVal)) {
                regexField.setText(regexTemplates.get(newVal));
            }
        });
    }

    private void initTable() {
        resultTable.setItems(matchList);
        colIndex.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getIndex()));
        colMatch.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getContent()));
        colStart.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getStart()));
        colEnd.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getEnd()));
        colGroups.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGroups()));
    }

    private void registerListeners() {
        // 监控所有影响正则结果的输入
        super.observeChanges(
                regexField.textProperty(),
                sourceTextArea.textProperty(),
                chkIgnoreCase.selectedProperty(),
                chkMultiline.selectedProperty(),
                chkDotAll.selectedProperty()
        );

        // 为了避免每次打字都疯狂触发计算（虽然 BaseController 有防抖存库，但 UI 渲染需要及时），
        // 我们在这里单独加一个简单的 UI 响应逻辑，或者直接利用 BaseController 的防抖保存逻辑来触发计算？
        // 为了更好的体验，建议：
        // 1. 存库用 BaseController 的 1秒防抖。
        // 2. UI 计算用 200ms 的本地防抖 (或者直接实时，Java 正则引擎很快)。

        // 这里简单处理：直接监听，利用 JavaFX 属性绑定触发计算
        regexField.textProperty().addListener(e -> runRegex());
        sourceTextArea.textProperty().addListener(e -> runRegex());
        chkIgnoreCase.selectedProperty().addListener(e -> runRegex());
        chkMultiline.selectedProperty().addListener(e -> runRegex());
        chkDotAll.selectedProperty().addListener(e -> runRegex());
    }

    private void runRegex() {
        String patternStr = regexField.getText();
        String source = sourceTextArea.getText();

        if (patternStr == null || patternStr.isEmpty()) {
            statusLabel.setText("请输入正则表达式");
            statusLabel.setStyle("-fx-text-fill: grey;");
            matchList.clear();
            matchCountLabel.setText("共找到 0 处匹配");
            return;
        }

        try {
            // 构建 Pattern 标志位
            int flags = 0;
            if (chkIgnoreCase.isSelected()) flags |= Pattern.CASE_INSENSITIVE;
            if (chkMultiline.isSelected()) flags |= Pattern.MULTILINE;
            if (chkDotAll.isSelected()) flags |= Pattern.DOTALL;

            Pattern pattern = Pattern.compile(patternStr, flags);
            Matcher matcher = pattern.matcher(source == null ? "" : source);

            matchList.clear();
            int count = 0;

            // 开始查找
            while (matcher.find()) {
                count++;
                // 收集捕获组信息 (Group 1, Group 2...)
                StringBuilder groups = new StringBuilder();
                if (matcher.groupCount() > 0) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        groups.append("[").append(i).append("]: ").append(matcher.group(i)).append("; ");
                    }
                }

                matchList.add(new MatchItem(
                        count,
                        matcher.group(),
                        matcher.start(),
                        matcher.end(),
                        groups.toString()
                ));

                // 防止超长文本导致内存爆炸，限制显示前 1000 条
                if (count >= 1000) break;
            }

            matchCountLabel.setText("共找到 " + count + " 处匹配" + (count >= 1000 ? " (仅显示前1000条)" : ""));
            statusLabel.setText("正则表达式有效");
            statusLabel.setStyle("-fx-text-fill: green;");

        } catch (PatternSyntaxException e) {
            statusLabel.setText("正则语法错误: " + e.getDescription());
            statusLabel.setStyle("-fx-text-fill: red;");
            matchList.clear();
            matchCountLabel.setText("错误");
        } catch (Exception e) {
            statusLabel.setText("发生错误: " + e.getMessage());
        }
    }

    // === BaseController 实现 ===

    @Override
    protected String getViewKey() {
        return "tool.regex.tester";
    }

    @Override
    protected Class<RegexViewState> getStorageType() {
        return RegexViewState.class;
    }

    @Override
    protected void restoreValues(RegexViewState state) {
        if (state == null) return;

        regexField.setText(state.getRegexPattern());
        sourceTextArea.setText(state.getSourceText());
        chkIgnoreCase.setSelected(state.isIgnoreCase());
        chkMultiline.setSelected(state.isMultiline());
        chkDotAll.setSelected(state.isDotAll());

        // 恢复后立即执行一次匹配
        runRegex();
    }

    @Override
    protected RegexViewState captureValues() {
        return new RegexViewState(
                regexField.getText(),
                sourceTextArea.getText(),
                chkIgnoreCase.isSelected(),
                chkMultiline.isSelected(),
                chkDotAll.isSelected()
        );
    }

    // === 内部类：表格数据模型 ===
    public static class MatchItem {
        private final int index;
        private final String content;
        private final int start;
        private final int end;
        private final String groups;

        public MatchItem(int index, String content, int start, int end, String groups) {
            this.index = index;
            this.content = content;
            this.start = start;
            this.end = end;
            this.groups = groups;
        }

        public int getIndex() { return index; }
        public String getContent() { return content; }
        public int getStart() { return start; }
        public int getEnd() { return end; }
        public String getGroups() { return groups; }
    }
}