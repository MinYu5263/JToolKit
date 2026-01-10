package com.minyu.jtoolkit.module.cron;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.minyu.jtoolkit.module.BaseController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CronController extends BaseController<CronPersistentState> {

    @FXML private TextField cronExpressionField;
    @FXML private TextArea resultArea;
    @FXML private Button btnParse;

    // ================= 秒 (Seconds) =================
    @FXML private ToggleGroup secGroup;
    @FXML private RadioButton secTypeEvery;     // 每秒
    @FXML private RadioButton secTypeRange;     // 周期 (Range)
    @FXML private Spinner<Integer> secRangeStart, secRangeEnd;
    @FXML private RadioButton secTypeIncrement; // 循环 (Increment)
    @FXML private Spinner<Integer> secIncrementStart, secIncrementStep;
    @FXML private RadioButton secTypeSpecific;  // 指定
    @FXML private Pane secSpecificContainer;    // **注意：在FXML中给包含0-59 CheckBox的VBox加上此ID**

    // ================= 分 (Minutes) =================
    @FXML private ToggleGroup minGroup;
    @FXML private RadioButton minTypeEvery;
    @FXML private RadioButton minTypeRange;
    @FXML private Spinner<Integer> minRangeStart, minRangeEnd;
    @FXML private RadioButton minTypeIncrement;
    @FXML private Spinner<Integer> minIncrementStart, minIncrementStep;
    @FXML private RadioButton minTypeSpecific;
    @FXML private Pane minSpecificContainer;

    // ================= 时 (Hours) =================
    @FXML private ToggleGroup hourGroup;
    @FXML private RadioButton hourTypeEvery;
    @FXML private RadioButton hourTypeRange;
    @FXML private Spinner<Integer> hourRangeStart, hourRangeEnd;
    @FXML private RadioButton hourTypeIncrement;
    @FXML private Spinner<Integer> hourIncrementStart, hourIncrementStep;
    @FXML private RadioButton hourTypeSpecific;
    @FXML private Pane hourSpecificContainer;

    // ================= 日 (Day of Month) =================
    @FXML private ToggleGroup dayGroup;
    @FXML private RadioButton dayTypeEvery;
    @FXML private RadioButton dayTypeRange;
    @FXML private Spinner<Integer> dayRangeStart, dayRangeEnd;
    @FXML private RadioButton dayTypeIncrement;
    @FXML private Spinner<Integer> dayIncrementStart, dayIncrementStep;
    @FXML private RadioButton dayTypeNearestWeekday; // 每月x号最近的工作日
    @FXML private Spinner<Integer> dayNearestWeekday;
    @FXML private RadioButton dayTypeLastDay;        // 本月最后一天 (L)
    @FXML private RadioButton dayTypeLastWeekday;    // 本月最后工作日 (LW)
    @FXML private RadioButton dayTypeSpecific;
    @FXML private Pane daySpecificContainer;

    // ================= 月 (Month) =================
    @FXML private ToggleGroup monthGroup;
    @FXML private RadioButton monthTypeEvery;
    @FXML private RadioButton monthTypeRange;
    @FXML private Spinner<Integer> monthRangeStart, monthRangeEnd;
    @FXML private RadioButton monthTypeIncrement;
    @FXML private Spinner<Integer> monthIncrementStart, monthIncrementStep;
    @FXML private RadioButton monthTypeSpecific;
    @FXML private Pane monthSpecificContainer;

    // ================= 周 (Day of Week) =================
    @FXML private ToggleGroup weekGroup;
    @FXML private RadioButton weekTypeEvery;
    @FXML private RadioButton weekTypeNone;          // 不指定 (?)
    @FXML private RadioButton weekTypeRange;
    @FXML private Spinner<Integer> weekRangeStart, weekRangeEnd;
    @FXML private RadioButton weekTypeNthDay;        // 第X周的星期Y
    @FXML private Spinner<Integer> weekNthWeek, weekNthDayVal;
    @FXML private RadioButton weekTypeLast;          // 本月最后一个星期X
    @FXML private Spinner<Integer> weekLastDayVal;
    @FXML private RadioButton weekTypeSpecific;
    @FXML private Pane weekSpecificContainer;

    // ================= 年 (Year) =================
    @FXML private ToggleGroup yearGroup;
    @FXML private RadioButton yearTypeNone;          // 不指定 (留空)
    @FXML private RadioButton yearTypeEvery;
    @FXML private RadioButton yearTypeRange;
    @FXML private Spinner<Integer> yearRangeStart, yearRangeEnd;

    private final CronParser cronParser;

    public CronController() {
        // 使用 Quartz 定义
        this.cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
    }

    @Override
    public void initView() {
        // 1. 初始化所有 Spinner
        initCommonSpinners();

        // 2. 绑定事件监听 (任何修改触发自动生成)
        bindListeners();

        // 3. 按钮事件
        btnParse.setOnAction(e -> onParse());

        // 4. 首次运行
        autoGenerate();
    }

    private void initCommonSpinners() {
        // 秒、分: 0-59
        initSpinner(secRangeStart, 0, 59, 0); initSpinner(secRangeEnd, 0, 59, 59);
        initSpinner(secIncrementStart, 0, 59, 0); initSpinner(secIncrementStep, 1, 59, 1);
        initSpinner(minRangeStart, 0, 59, 0); initSpinner(minRangeEnd, 0, 59, 59);
        initSpinner(minIncrementStart, 0, 59, 0); initSpinner(minIncrementStep, 1, 59, 1);

        // 时: 0-23
        initSpinner(hourRangeStart, 0, 23, 0); initSpinner(hourRangeEnd, 0, 23, 23);
        initSpinner(hourIncrementStart, 0, 23, 0); initSpinner(hourIncrementStep, 1, 23, 1);

        // 日: 1-31
        initSpinner(dayRangeStart, 1, 31, 1); initSpinner(dayRangeEnd, 1, 31, 31);
        initSpinner(dayIncrementStart, 1, 31, 1); initSpinner(dayIncrementStep, 1, 31, 1);
        initSpinner(dayNearestWeekday, 1, 31, 1);

        // 月: 1-12
        initSpinner(monthRangeStart, 1, 12, 1); initSpinner(monthRangeEnd, 1, 12, 12);
        initSpinner(monthIncrementStart, 1, 12, 1); initSpinner(monthIncrementStep, 1, 12, 1);

        // 周: 1-7
        initSpinner(weekRangeStart, 1, 7, 1); initSpinner(weekRangeEnd, 1, 7, 7);
        initSpinner(weekNthWeek, 1, 5, 1); initSpinner(weekNthDayVal, 1, 7, 1);
        initSpinner(weekLastDayVal, 1, 7, 1);

        // 年: 2023-2099
        int currentYear = ZonedDateTime.now().getYear();
        initSpinner(yearRangeStart, currentYear, 2099, currentYear);
        initSpinner(yearRangeEnd, currentYear, 2099, currentYear + 1);
    }

    private void initSpinner(Spinner<Integer> spinner, int min, int max, int initial) {
        if (spinner == null) return;
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
        spinner.setValueFactory(factory);
        // 允许直接输入数值并提交
        spinner.setEditable(true);
        spinner.valueProperty().addListener(e -> autoGenerate());
    }

    private void bindListeners() {
        // 绑定 ToggleGroup 切换
        List<ToggleGroup> groups = List.of(secGroup, minGroup, hourGroup, dayGroup, monthGroup, weekGroup, yearGroup);
        for (ToggleGroup group : groups) {
            group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> autoGenerate());
        }

        // 绑定所有 CheckBox 点击事件
        bindCheckBoxesInContainer(secSpecificContainer);
        bindCheckBoxesInContainer(minSpecificContainer);
        bindCheckBoxesInContainer(hourSpecificContainer);
        bindCheckBoxesInContainer(daySpecificContainer);
        bindCheckBoxesInContainer(monthSpecificContainer);
        bindCheckBoxesInContainer(weekSpecificContainer);
    }

    /**
     * 递归遍历容器找到所有 CheckBox 并绑定事件
     */
    private void bindCheckBoxesInContainer(Pane container) {
        if (container == null) return;
        for (Node node : container.getChildren()) {
            if (node instanceof CheckBox) {
                ((CheckBox) node).selectedProperty().addListener(e -> autoGenerate());
            } else if (node instanceof Pane) {
                // 处理嵌套布局（如 HBox 在 VBox 里）
                bindCheckBoxesInContainer((Pane) node);
            }
        }
    }

    private void autoGenerate() {
        if (cronExpressionField.isFocused()) return; // 防抖

        String sec = generateStandardPart(secGroup, secTypeEvery, secTypeRange, secRangeStart, secRangeEnd,
                secTypeIncrement, secIncrementStart, secIncrementStep, secTypeSpecific, secSpecificContainer);

        String min = generateStandardPart(minGroup, minTypeEvery, minTypeRange, minRangeStart, minRangeEnd,
                minTypeIncrement, minIncrementStart, minIncrementStep, minTypeSpecific, minSpecificContainer);

        String hour = generateStandardPart(hourGroup, hourTypeEvery, hourTypeRange, hourRangeStart, hourRangeEnd,
                hourTypeIncrement, hourIncrementStart, hourIncrementStep, hourTypeSpecific, hourSpecificContainer);

        String month = generateStandardPart(monthGroup, monthTypeEvery, monthTypeRange, monthRangeStart, monthRangeEnd,
                monthTypeIncrement, monthIncrementStart, monthIncrementStep, monthTypeSpecific, monthSpecificContainer);

        String year = generateYearPart();

        // === 处理 日(Day) 和 周(Week) 的互斥关系 ===
        String day;
        String week;

        // 如果用户在 "周" 选择了 "不指定(?)"，或者在 "日" 选择了具体配置，则日优先
        boolean weekIsNone = weekGroup.getSelectedToggle() == weekTypeNone;

        if (weekIsNone) {
            week = "?";
            day = generateDayPart();
        } else {
            // 如果用户专门配置了周，日必须变 ?
            // 注意：这里简化逻辑，如果日选了 "?" (通常UI里没有日的不指定选项，默认是Every)，则根据周的配置来
            // 为了保证 Quartz 格式正确，如果周有特定配置，日强制为 ?
            week = generateWeekPart();
            day = "?";
        }

        // 修正：如果日和周都碰巧生成了 ? (例如周选None，日选None)，Quartz是不允许的。
        // 但根据当前UI，日没有 "None" 选项，通常是 "Every" (*)。
        // 互斥规则：
        // 1. 如果周选了 "Every (*)" 或 "Specific" 等，日这就得是 "?"
        // 2. 如果周选了 "None (?)"，日就可以是 "*" 或其他

        if (!"?".equals(week)) {
            day = "?";
        } else if ("?".equals(day)) {
            // 如果两个都是 ?，这不合法。通常默认日是 *
            day = "*";
        }

        String cron = String.format("%s %s %s %s %s %s", sec, min, hour, day, month, week);
        if (year != null && !year.isEmpty()) {
            cron += " " + year;
        }

        cronExpressionField.setText(cron);
        saveValues();
    }

    // 生成标准的 时、分、秒、月 部分
    private String generateStandardPart(ToggleGroup group, RadioButton every,
                                        RadioButton range, Spinner<Integer> rStart, Spinner<Integer> rEnd,
                                        RadioButton increment, Spinner<Integer> iStart, Spinner<Integer> iStep,
                                        RadioButton specific, Pane specificContainer) {
        Toggle selected = group.getSelectedToggle();
        if (selected == every) return "*";
        if (selected == range) return rStart.getValue() + "-" + rEnd.getValue();
        if (selected == increment) return iStart.getValue() + "/" + iStep.getValue();
        if (selected == specific) return getSelectedCheckBoxValues(specificContainer);
        return "*";
    }

    // 生成 日 部分
    private String generateDayPart() {
        Toggle selected = dayGroup.getSelectedToggle();
        if (selected == dayTypeEvery) return "*";
        if (selected == dayTypeRange) return dayRangeStart.getValue() + "-" + dayRangeEnd.getValue();
        if (selected == dayTypeIncrement) return dayIncrementStart.getValue() + "/" + dayIncrementStep.getValue();
        if (selected == dayTypeNearestWeekday) return dayNearestWeekday.getValue() + "W";
        if (selected == dayTypeLastDay) return "L";
        if (selected == dayTypeLastWeekday) return "LW";
        if (selected == dayTypeSpecific) return getSelectedCheckBoxValues(daySpecificContainer);
        return "*";
    }

    // 生成 周 部分
    private String generateWeekPart() {
        Toggle selected = weekGroup.getSelectedToggle();
        if (selected == weekTypeNone) return "?";
        if (selected == weekTypeEvery) return "*"; // Quartz中周用*也是合法的，代表每天
        if (selected == weekTypeRange) return weekRangeStart.getValue() + "-" + weekRangeEnd.getValue();
        if (selected == weekTypeNthDay) return weekNthDayVal.getValue() + "#" + weekNthWeek.getValue();
        if (selected == weekTypeLast) return weekLastDayVal.getValue() + "L";
        if (selected == weekTypeSpecific) return getSelectedCheckBoxValues(weekSpecificContainer);
        return "?";
    }

    // 生成 年 部分
    private String generateYearPart() {
        Toggle selected = yearGroup.getSelectedToggle();
        if (selected == yearTypeNone) return "";
        if (selected == yearTypeEvery) return "*";
        if (selected == yearTypeRange) return yearRangeStart.getValue() + "-" + yearRangeEnd.getValue();
        return "";
    }

    /**
     * 获取容器内所有被选中的 CheckBox 的文本值，并用逗号拼接
     */
    private String getSelectedCheckBoxValues(Pane container) {
        List<String> selected = new ArrayList<>();
        collectCheckBoxValues(container, selected);
        if (selected.isEmpty()) return "*"; // 如果没选，默认 *
        return String.join(",", selected);
    }

    private void collectCheckBoxValues(Pane container, List<String> result) {
        if (container == null) return;
        for (Node node : container.getChildren()) {
            if (node instanceof CheckBox) {
                CheckBox cb = (CheckBox) node;
                if (cb.isSelected()) {
                    // 假设 CheckBox 的 text 就是数值 (00, 01 -> 0, 1)
                    // 需要去除前导0，比如 "01" -> "1"
                    try {
                        // 尝试转数字再转字符串去掉前导0
                        String text = cb.getText();
                        // 如果是 "周一" 这种，需特殊处理，但Quartz周通常用 1-7 (SUN-SAT)
                        // 这里假设 FXML 里 CheckBox 要么是数字，要么在Controller里做映射
                        // 简单处理：如果是数字，去0；否则保留
                        if (text.matches("\\d+")) {
                            result.add(String.valueOf(Integer.parseInt(text)));
                        } else {
                            // 处理中文周
                            result.add(convertWeekTextToNum(text));
                        }
                    } catch (Exception e) {
                        result.add(cb.getText());
                    }
                }
            } else if (node instanceof Pane) {
                collectCheckBoxValues((Pane) node, result);
            }
        }
    }

    private String convertWeekTextToNum(String text) {
        switch (text) {
            case "周日": return "1";
            case "周一": return "2";
            case "周二": return "3";
            case "周三": return "4";
            case "周四": return "5";
            case "周五": return "6";
            case "周六": return "7";
            default: return text;
        }
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
            resultArea.setStyle("-fx-text-fill: black;");

        } catch (Exception e) {
            resultArea.setText("解析错误: " + e.getMessage());
            resultArea.setStyle("-fx-text-fill: red;");
        }
    }

    // === BaseController ===
    @Override
    protected String getViewKey() { return "cron"; }

    @Override
    protected Class<CronPersistentState> getStorageType() { return CronPersistentState.class; }

    @Override
    protected void restoreValues(CronPersistentState state) {
        if (state != null && state.getLastExpression() != null) {
            cronExpressionField.setText(state.getLastExpression());
            onParse();
        }
    }

    @Override
    protected CronPersistentState captureValues() {
        return new CronPersistentState(cronExpressionField.getText());
    }
}