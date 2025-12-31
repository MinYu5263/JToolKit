package com.minyu.jtoolkit.module.cron;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.ViewDataService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class CronController extends BaseController<CronViewState> {

    @FXML private TextField cronExpressionField;
    @FXML private TextArea resultArea;

    // === 秒控件 ===
    @FXML private ToggleGroup secGroup;
    @FXML private RadioButton secTypeEvery, secTypeCycle, secTypeRange;
    @FXML private Spinner<Integer> secCycleStart, secCycleStep, secRangeStart, secRangeEnd;

    // === 分控件 ===
    @FXML private ToggleGroup minGroup;
    @FXML private RadioButton minTypeEvery, minTypeCycle, minTypeRange;
    @FXML private Spinner<Integer> minCycleStart, minCycleStep, minRangeStart, minRangeEnd;

    // === 时控件 ===
    @FXML private ToggleGroup hourGroup;
    @FXML private RadioButton hourTypeEvery, hourTypeCycle, hourTypeRange;
    @FXML private Spinner<Integer> hourCycleStart, hourCycleStep, hourRangeStart, hourRangeEnd;

    // Quartz Cron 解析器
    private final CronParser cronParser;

    public CronController(ViewDataService viewDataService) {
        super();
        // 初始化 Quartz 解析器
        this.cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
    }

    @FXML
    public void initView() {
        // 1. 初始化 Spinner (0-59)
        initSpinner(secCycleStart, 0, 59, 0); initSpinner(secCycleStep, 1, 59, 1);
        initSpinner(secRangeStart, 0, 59, 0); initSpinner(secRangeEnd, 0, 59, 0);

        initSpinner(minCycleStart, 0, 59, 0); initSpinner(minCycleStep, 1, 59, 1);
        initSpinner(minRangeStart, 0, 59, 0); initSpinner(minRangeEnd, 0, 59, 0);

        initSpinner(hourCycleStart, 0, 23, 0); initSpinner(hourCycleStep, 1, 23, 1);
        initSpinner(hourRangeStart, 0, 23, 0); initSpinner(hourRangeEnd, 0, 23, 0);

        // 2. 恢复状态


        // 3. 注册监听器（任意控件变化 -> 重新生成表达式）
        registerListeners();

        // 4. 首次解析
        onParse();
    }

    /**
     * 初始化 Spinner 工厂
     */
    private void initSpinner(Spinner<Integer> spinner, int min, int max, int initial) {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
        spinner.setValueFactory(factory);
    }

    /**
     * 注册所有控件的变更监听
     */
    private void registerListeners() {
        // 监听 RadioButton 切换
        super.observeChanges(secGroup.selectedToggleProperty(), minGroup.selectedToggleProperty(), hourGroup.selectedToggleProperty());

        // 监听 Spinner 数值变化 (Spinner.valueProperty)
        // 注意：Spinner 需要特殊处理，这里简单处理，实际可封装
        secCycleStart.valueProperty().addListener(e -> autoGenerate());
        secCycleStep.valueProperty().addListener(e -> autoGenerate());
        secRangeStart.valueProperty().addListener(e -> autoGenerate());
        secRangeEnd.valueProperty().addListener(e -> autoGenerate());
        // ... (对所有 Spinner 绑定 autoGenerate)

        // 文本框手动修改监听
        cronExpressionField.textProperty().addListener((obs, old, newVal) -> {
            // 当文本框被手动修改时，尝试防抖保存并解析
            // 注意：这里为了防止循环调用，需要判断焦点是否在文本框
            if (cronExpressionField.isFocused()) {
                onParse();
            }
        });
    }

    /**
     * 自动生成 Cron 字符串 (UI -> String)
     */
    private void autoGenerate() {
        if (cronExpressionField.isFocused()) return; // 如果正在手动输入，不覆盖

        String sec = buildPart(secGroup, secTypeEvery, secTypeCycle, secTypeRange,
                secCycleStart, secCycleStep, secRangeStart, secRangeEnd);

        String min = buildPart(minGroup, minTypeEvery, minTypeCycle, minTypeRange,
                minCycleStart, minCycleStep, minRangeStart, minRangeEnd);

        String hour = buildPart(hourGroup, hourTypeEvery, hourTypeCycle, hourTypeRange,
                hourCycleStart, hourCycleStep, hourRangeStart, hourRangeEnd);

        // 这里简化处理：日、月、周 暂时给默认值，完整版需要类似逻辑
        String day = "*";
        String month = "*";
        String week = "?";
        String year = "*";

        String cron = String.format("%s %s %s %s %s %s %s", sec, min, hour, day, month, week, year);
        cronExpressionField.setText(cron.trim());

        onParse(); // 触发预览
        super.saveValues(); // 保存状态
    }

    private String buildPart(ToggleGroup group, RadioButton every, RadioButton cycle, RadioButton range,
                             Spinner<Integer> cStart, Spinner<Integer> cStep, Spinner<Integer> rStart, Spinner<Integer> rEnd) {
        Toggle selected = group.getSelectedToggle();
        if (selected == every) {
            return "*";
        } else if (selected == cycle) {
            return cStart.getValue() + "/" + cStep.getValue();
        } else if (selected == range) {
            return rStart.getValue() + "-" + rEnd.getValue();
        }
        return "*";
    }

    @FXML
    private void onParse() {
        String expression = cronExpressionField.getText();
        if (expression == null || expression.isBlank()) return;

        try {
            Cron cron = cronParser.parse(expression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime now = ZonedDateTime.now();
            StringBuilder sb = new StringBuilder();

            // 计算未来 5 次
            for (int i = 0; i < 5; i++) {
                Optional<ZonedDateTime> next = executionTime.nextExecution(now);
                if (next.isPresent()) {
                    now = next.get();
                    sb.append(String.format("第 %d 次: %s\n", i + 1, now.toLocalDateTime()));
                } else {
                    sb.append("无更多执行时间");
                    break;
                }
            }
            resultArea.setText(sb.toString());
            resultArea.setStyle("-fx-text-fill: black;"); // 恢复颜色

        } catch (IllegalArgumentException e) {
            resultArea.setText("Cron 表达式格式错误: " + e.getMessage());
            resultArea.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void onCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(cronExpressionField.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    // === BaseController 实现 ===

    @Override
    protected String getViewKey() {
        return "tool.cron.generator";
    }

    @Override
    protected Class<CronViewState> getStorageType() {
        return CronViewState.class;
    }

    @Override
    protected void restoreValues(CronViewState state) {
        if (state != null && state.getLastExpression() != null) {
            cronExpressionField.setText(state.getLastExpression());
            // 注意：这里没有做复杂的 String -> UI 逆向解析，
            // 只是恢复了文本框和预览结果，这是最性价比的做法。
            onParse();
        }
    }

    @Override
    protected CronViewState captureValues() {
        return new CronViewState(cronExpressionField.getText());
    }
}