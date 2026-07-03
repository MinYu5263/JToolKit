package com.minyu.jtoolkit.module.cron;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.*;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CronController extends BaseController<CronPersistentState> {

    @FXML
    private TextField cronExpressionField;
    @FXML
    private TextArea resultArea;
    @FXML
    private Button btnParse;
    @FXML
    private TextField dateTimeFormatField;
    @FXML
    private Button btnReverseParse;
    @FXML
    private ComboBox<Integer> executionCountCombo;

    @FXML
    private ToggleGroup secGroup;
    @FXML
    private RadioButton secTypeEvery;
    @FXML
    private RadioButton secTypeRange;
    @FXML
    private Spinner<Integer> secRangeStart, secRangeEnd;
    @FXML
    private RadioButton secTypeIncrement;
    @FXML
    private Spinner<Integer> secIncrementStart, secIncrementStep;
    @FXML
    private RadioButton secTypeSpecific;
    @FXML
    private Pane secSpecificContainer;
    @FXML
    private ToggleGroup minGroup;
    @FXML
    private RadioButton minTypeEvery;
    @FXML
    private RadioButton minTypeRange;
    @FXML
    private Spinner<Integer> minRangeStart, minRangeEnd;
    @FXML
    private RadioButton minTypeIncrement;
    @FXML
    private Spinner<Integer> minIncrementStart, minIncrementStep;
    @FXML
    private RadioButton minTypeSpecific;
    @FXML
    private Pane minSpecificContainer;

    @FXML
    private ToggleGroup hourGroup;
    @FXML
    private RadioButton hourTypeEvery;
    @FXML
    private RadioButton hourTypeRange;
    @FXML
    private Spinner<Integer> hourRangeStart, hourRangeEnd;
    @FXML
    private RadioButton hourTypeIncrement;
    @FXML
    private Spinner<Integer> hourIncrementStart, hourIncrementStep;
    @FXML
    private RadioButton hourTypeSpecific;
    @FXML
    private Pane hourSpecificContainer;

    @FXML
    private ToggleGroup dayGroup;
    @FXML
    private RadioButton dayTypeEvery;
    @FXML
    private RadioButton dayTypeRange;
    @FXML
    private Spinner<Integer> dayRangeStart, dayRangeEnd;
    @FXML
    private RadioButton dayTypeIncrement;
    @FXML
    private Spinner<Integer> dayIncrementStart, dayIncrementStep;
    @FXML
    private RadioButton dayTypeNearestWeekday;
    @FXML
    private Spinner<Integer> dayNearestWeekday;
    @FXML
    private RadioButton dayTypeLastDay;
    @FXML
    private RadioButton dayTypeLastWeekday;
    @FXML
    private RadioButton dayTypeSpecific;
    @FXML
    private Pane daySpecificContainer;

    @FXML
    private ToggleGroup monthGroup;
    @FXML
    private RadioButton monthTypeEvery;
    @FXML
    private RadioButton monthTypeRange;
    @FXML
    private Spinner<Integer> monthRangeStart, monthRangeEnd;
    @FXML
    private RadioButton monthTypeIncrement;
    @FXML
    private Spinner<Integer> monthIncrementStart, monthIncrementStep;
    @FXML
    private RadioButton monthTypeSpecific;
    @FXML
    private Pane monthSpecificContainer;

    @FXML
    private ToggleGroup weekGroup;
    @FXML
    private RadioButton weekTypeEvery;
    @FXML
    private RadioButton weekTypeNone;
    @FXML
    private RadioButton weekTypeRange;
    @FXML
    private Spinner<Integer> weekRangeStart, weekRangeEnd;
    @FXML
    private RadioButton weekTypeNthDay;
    @FXML
    private Spinner<Integer> weekNthWeek, weekNthDayVal;
    @FXML
    private RadioButton weekTypeLast;
    @FXML
    private Spinner<Integer> weekLastDayVal;
    @FXML
    private RadioButton weekTypeSpecific;
    @FXML
    private Pane weekSpecificContainer;

    @FXML
    private ToggleGroup yearGroup;
    @FXML
    private RadioButton yearTypeNone;
    @FXML
    private RadioButton yearTypeEvery;
    @FXML
    private RadioButton yearTypeRange;
    @FXML
    private Spinner<Integer> yearRangeStart, yearRangeEnd;

    private final CronParser cronParser;

    private boolean isRestoring = false;

    public CronController() {
        this.cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
    }

    @Override
    public void initView() {
        initCommonSpinners();

        executionCountCombo.getItems().addAll(5, 10, 25, 50, 100);

        bindListeners();

        btnParse.setOnAction(e -> onParse());
        btnReverseParse.setOnAction(e -> onReverseParse());
    }

    private void initCommonSpinners() {
        // 秒、分: 0-59
        initSpinner(secRangeStart, 0, 59, 0);
        initSpinner(secRangeEnd, 0, 59, 59);
        initSpinner(secIncrementStart, 0, 59, 0);
        initSpinner(secIncrementStep, 1, 59, 1);
        initSpinner(minRangeStart, 0, 59, 0);
        initSpinner(minRangeEnd, 0, 59, 59);
        initSpinner(minIncrementStart, 0, 59, 0);
        initSpinner(minIncrementStep, 1, 59, 1);

        // 时: 0-23
        initSpinner(hourRangeStart, 0, 23, 0);
        initSpinner(hourRangeEnd, 0, 23, 23);
        initSpinner(hourIncrementStart, 0, 23, 0);
        initSpinner(hourIncrementStep, 1, 23, 1);

        // 日: 1-31
        initSpinner(dayRangeStart, 1, 31, 1);
        initSpinner(dayRangeEnd, 1, 31, 31);
        initSpinner(dayIncrementStart, 1, 31, 1);
        initSpinner(dayIncrementStep, 1, 31, 1);
        initSpinner(dayNearestWeekday, 1, 31, 1);

        // 月: 1-12
        initSpinner(monthRangeStart, 1, 12, 1);
        initSpinner(monthRangeEnd, 1, 12, 12);
        initSpinner(monthIncrementStart, 1, 12, 1);
        initSpinner(monthIncrementStep, 1, 12, 1);

        // 周: 1-7
        initSpinner(weekRangeStart, 1, 7, 1);
        initSpinner(weekRangeEnd, 1, 7, 7);
        initSpinner(weekNthWeek, 1, 5, 1);
        initSpinner(weekNthDayVal, 1, 7, 1);
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
        spinner.setEditable(true);
        spinner.valueProperty().addListener(e -> autoGenerate());
    }

    private void bindListeners() {
        List<ToggleGroup> groups = List.of(secGroup, minGroup, hourGroup, dayGroup, monthGroup, weekGroup, yearGroup);
        for (ToggleGroup group : groups) {
            group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> autoGenerate());
        }

        bindCheckBoxesInContainer(secSpecificContainer);
        bindCheckBoxesInContainer(minSpecificContainer);
        bindCheckBoxesInContainer(hourSpecificContainer);
        bindCheckBoxesInContainer(daySpecificContainer);
        bindCheckBoxesInContainer(monthSpecificContainer);
        bindCheckBoxesInContainer(weekSpecificContainer);
    }

    private void bindCheckBoxesInContainer(Pane container) {
        if (container == null) return;
        for (Node node : container.getChildren()) {
            if (node instanceof CheckBox cb) {
                cb.selectedProperty().addListener(e -> autoGenerate());
            } else if (node instanceof Pane p) {
                bindCheckBoxesInContainer(p);
            }
        }
    }

    private void autoGenerate() {
        if (isRestoring) return;
        if (cronExpressionField.isFocused()) return;

        String sec = generateStandardPart(secGroup, secTypeEvery, secTypeRange, secRangeStart, secRangeEnd,
                secTypeIncrement, secIncrementStart, secIncrementStep, secTypeSpecific, secSpecificContainer);

        String min = generateStandardPart(minGroup, minTypeEvery, minTypeRange, minRangeStart, minRangeEnd,
                minTypeIncrement, minIncrementStart, minIncrementStep, minTypeSpecific, minSpecificContainer);

        String hour = generateStandardPart(hourGroup, hourTypeEvery, hourTypeRange, hourRangeStart, hourRangeEnd,
                hourTypeIncrement, hourIncrementStart, hourIncrementStep, hourTypeSpecific, hourSpecificContainer);

        String month = generateStandardPart(monthGroup, monthTypeEvery, monthTypeRange, monthRangeStart, monthRangeEnd,
                monthTypeIncrement, monthIncrementStart, monthIncrementStep, monthTypeSpecific, monthSpecificContainer);

        String year = generateYearPart();

        // 日和周互斥：一方有值时另一方必须为 ?
        String day;
        String week;

        if (weekGroup.getSelectedToggle() == weekTypeNone) {
            week = "?";
            day = generateDayPart();
        } else {
            week = generateWeekPart();
            day = "?";
        }

        if (!"?".equals(week)) {
            day = "?";
        } else if ("?".equals(day)) {
            day = "*";
        }

        String cron = String.format("%s %s %s %s %s %s", sec, min, hour, day, month, week);
        if (!year.isEmpty()) {
            cron += " " + year;
        }

        cronExpressionField.setText(cron);
        saveValues();
    }

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

    private String generateWeekPart() {
        Toggle selected = weekGroup.getSelectedToggle();
        if (selected == weekTypeNone) return "?";
        if (selected == weekTypeEvery) return "*";
        if (selected == weekTypeRange) return weekRangeStart.getValue() + "-" + weekRangeEnd.getValue();
        if (selected == weekTypeNthDay) return weekNthDayVal.getValue() + "#" + weekNthWeek.getValue();
        if (selected == weekTypeLast) return weekLastDayVal.getValue() + "L";
        if (selected == weekTypeSpecific) return getSelectedCheckBoxValues(weekSpecificContainer);
        return "?";
    }

    private String generateYearPart() {
        Toggle selected = yearGroup.getSelectedToggle();
        if (selected == yearTypeNone) return "";
        if (selected == yearTypeEvery) return "*";
        if (selected == yearTypeRange) return yearRangeStart.getValue() + "-" + yearRangeEnd.getValue();
        return "";
    }

    private String getSelectedCheckBoxValues(Pane container) {
        List<String> selected = new ArrayList<>();
        collectCheckBoxValues(container, selected);
        if (selected.isEmpty()) return "*";
        return String.join(",", selected);
    }

    private void collectCheckBoxValues(Pane container, List<String> result) {
        if (container == null) return;
        for (Node node : container.getChildren()) {
            if (node instanceof CheckBox cb) {
                if (cb.isSelected()) {
                    String text = cb.getText();
                    if (text.matches("\\d+")) {
                        result.add(String.valueOf(Integer.parseInt(text)));
                    } else {
                        result.add(convertWeekTextToNum(text));
                    }
                }
            } else if (node instanceof Pane p) {
                collectCheckBoxValues(p, result);
            }
        }
    }

    private String convertWeekTextToNum(String text) {
        return switch (text) {
            case "周日" -> "1";
            case "周一" -> "2";
            case "周二" -> "3";
            case "周三" -> "4";
            case "周四" -> "5";
            case "周五" -> "6";
            case "周六" -> "7";
            default -> text;
        };
    }

    @FXML
    private void onParse() {
        String expression = cronExpressionField.getText();
        if (expression == null || expression.isBlank()) return;

        try {
            Cron cron = cronParser.parse(expression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime now = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormatField.getText());
            StringBuilder sb = new StringBuilder();

            int count = executionCountCombo.getValue() != null ? executionCountCombo.getValue() : 5;

            for (int i = 0; i < count; i++) {
                Optional<ZonedDateTime> next = executionTime.nextExecution(now);
                if (next.isPresent()) {
                    now = next.get();

                    sb.append(String.format("第 %d 次: %s\n", i + 1, now.format(formatter)));
                } else {
                    sb.append("无更多执行时间");
                    break;
                }
            }
            resultArea.setText(sb.toString());
            resultArea.setStyle("-fx-text-fill: -color-fg-default;");

        } catch (Exception e) {
            resultArea.setText("解析错误: " + e.getMessage());
            resultArea.setStyle("-fx-text-fill: -color-danger-fg;");
        }
    }

    @FXML
    private void onReverseParse() {
        String expression = cronExpressionField.getText();
        if (expression == null || expression.isBlank()) return;

        try {
            Cron cron = cronParser.parse(expression);
            isRestoring = true;

            restoreStandardPart(cron.retrieve(CronFieldName.SECOND), secGroup,
                    secTypeEvery, secTypeRange, secRangeStart, secRangeEnd,
                    secTypeIncrement, secIncrementStart, secIncrementStep,
                    secTypeSpecific, secSpecificContainer);

            restoreStandardPart(cron.retrieve(CronFieldName.MINUTE), minGroup,
                    minTypeEvery, minTypeRange, minRangeStart, minRangeEnd,
                    minTypeIncrement, minIncrementStart, minIncrementStep,
                    minTypeSpecific, minSpecificContainer);

            restoreStandardPart(cron.retrieve(CronFieldName.HOUR), hourGroup,
                    hourTypeEvery, hourTypeRange, hourRangeStart, hourRangeEnd,
                    hourTypeIncrement, hourIncrementStart, hourIncrementStep,
                    hourTypeSpecific, hourSpecificContainer);

            restoreDayPart(cron.retrieve(CronFieldName.DAY_OF_MONTH));

            restoreStandardPart(cron.retrieve(CronFieldName.MONTH), monthGroup,
                    monthTypeEvery, monthTypeRange, monthRangeStart, monthRangeEnd,
                    monthTypeIncrement, monthIncrementStart, monthIncrementStep,
                    monthTypeSpecific, monthSpecificContainer);

            restoreWeekPart(cron.retrieve(CronFieldName.DAY_OF_WEEK));

            if (cron.retrieve(CronFieldName.YEAR) != null) {
                restoreYearPart(cron.retrieve(CronFieldName.YEAR));
            } else {
                yearTypeNone.setSelected(true);
            }

            resultArea.setText("UI 已根据表达式更新");
            resultArea.setStyle("-fx-text-fill: -color-success-emphasis;");

        } catch (Exception e) {
            e.printStackTrace();
            resultArea.setText("反解析失败: " + e.getMessage() + "\n可能包含复杂组合暂不支持 UI 映射");
            resultArea.setStyle("-fx-text-fill: -color-danger-fg;");
        } finally {
            isRestoring = false; // 解锁
        }
    }

    private void restoreStandardPart(CronField field, ToggleGroup group,
                                     RadioButton typeEvery,
                                     RadioButton typeRange, Spinner<Integer> rStart, Spinner<Integer> rEnd,
                                     RadioButton typeIncrement, Spinner<Integer> iStart, Spinner<Integer> iStep,
                                     RadioButton typeSpecific, Pane specificContainer) {
        if (field == null) return;
        FieldExpression exp = field.getExpression();

        if (exp instanceof Always) {
            typeEvery.setSelected(true);
        } else if (exp instanceof Between between) {
            typeRange.setSelected(true);
            rStart.getValueFactory().setValue((int) between.getFrom().getValue());
            rEnd.getValueFactory().setValue((int) between.getTo().getValue());
        } else if (exp instanceof Every every) {
            typeIncrement.setSelected(true);
            int start = 0;
            if (every.getExpression() instanceof On on) {
                start = on.getTime().getValue();
            } else if (every.getExpression() instanceof Between b) {
                start = (int) b.getFrom().getValue();
            }
            iStart.getValueFactory().setValue(start);
            iStep.getValueFactory().setValue(every.getPeriod().getValue());
        } else if (exp instanceof And || exp instanceof On) {
            // 指定 (列表或单个数字)
            typeSpecific.setSelected(true);
            clearCheckBoxes(specificContainer);
            List<Integer> values = getValuesFromExpression(exp);
            checkCheckBoxes(specificContainer, values);
        }
    }

    private void restoreDayPart(CronField field) {
        if (field == null) return;
        FieldExpression exp = field.getExpression();

        if (exp instanceof Always) {
            dayTypeEvery.setSelected(true);
        } else if (exp instanceof QuestionMark) {
            // 日和周的互斥由 autoGenerate 统一处理
        } else if (exp instanceof Between between) {
            dayTypeRange.setSelected(true);
            dayRangeStart.getValueFactory().setValue((int) between.getFrom().getValue());
            dayRangeEnd.getValueFactory().setValue((int) between.getTo().getValue());
        } else if (exp instanceof Every every) {
            dayTypeIncrement.setSelected(true);
            int start = 1;
            if (every.getExpression() instanceof On on) start = on.getTime().getValue();
            dayIncrementStart.getValueFactory().setValue(start);
            dayIncrementStep.getValueFactory().setValue(every.getPeriod().getValue());
        } else if (exp instanceof And || (exp instanceof On && !isSpecialChar(exp))) {
            dayTypeSpecific.setSelected(true);
            clearCheckBoxes(daySpecificContainer);
            checkCheckBoxes(daySpecificContainer, getValuesFromExpression(exp));
        } else {
            String expStr = exp.asString();
            if ("L".equals(expStr)) {
                dayTypeLastDay.setSelected(true);
            } else if ("LW".equals(expStr)) {
                dayTypeLastWeekday.setSelected(true);
            } else if (expStr.endsWith("W")) {
                dayTypeNearestWeekday.setSelected(true);
                dayNearestWeekday.getValueFactory().setValue(Integer.parseInt(expStr.replace("W", "")));
            }
        }
    }

    private void restoreWeekPart(CronField field) {
        if (field == null) return;
        FieldExpression exp = field.getExpression();

        if (exp instanceof QuestionMark) {
            weekTypeNone.setSelected(true);
        } else if (exp instanceof Always) {
            weekTypeEvery.setSelected(true);
        } else if (exp instanceof Between between) {
            weekTypeRange.setSelected(true);
            weekRangeStart.getValueFactory().setValue((int) between.getFrom().getValue());
            weekRangeEnd.getValueFactory().setValue((int) between.getTo().getValue());
        } else if (exp instanceof On on) {
            String s = on.asString();
            if (s.contains("#")) {
                weekTypeNthDay.setSelected(true);
                weekNthDayVal.getValueFactory().setValue(on.getTime().getValue());
                weekNthWeek.getValueFactory().setValue(on.getNth().getValue());
            } else if (s.contains("L")) {
                weekTypeLast.setSelected(true);
                weekLastDayVal.getValueFactory().setValue(on.getTime().getValue());
            } else {
                weekTypeSpecific.setSelected(true);
                clearCheckBoxes(weekSpecificContainer);
                checkCheckBoxes(weekSpecificContainer, List.of(on.getTime().getValue()));
            }
        } else if (exp instanceof And) {
            weekTypeSpecific.setSelected(true);
            clearCheckBoxes(weekSpecificContainer);
            checkCheckBoxes(weekSpecificContainer, getValuesFromExpression(exp));
        }
    }

    private void restoreYearPart(CronField field) {
        FieldExpression exp = field.getExpression();
        if (exp instanceof Always) {
            yearTypeEvery.setSelected(true);
        } else if (exp instanceof Between between) {
            yearTypeRange.setSelected(true);
            yearRangeStart.getValueFactory().setValue((int) between.getFrom().getValue());
            yearRangeEnd.getValueFactory().setValue((int) between.getTo().getValue());
        } else {
            yearTypeNone.setSelected(true);
        }
    }

    private boolean isSpecialChar(FieldExpression exp) {
        String s = exp.asString();
        return s.contains("L") || s.contains("W") || s.contains("#");
    }

    private List<Integer> getValuesFromExpression(FieldExpression exp) {
        List<Integer> res = new ArrayList<>();
        if (exp instanceof On on) {
            res.add(on.getTime().getValue());
        } else if (exp instanceof And and) {
            for (FieldExpression child : and.getExpressions()) {
                if (child instanceof On on) {
                    res.add(on.getTime().getValue());
                }
            }
        }
        return res;
    }

    private void clearCheckBoxes(Pane container) {
        if (container == null) return;
        for (Node node : container.getChildren()) {
            if (node instanceof CheckBox cb) cb.setSelected(false);
            else if (node instanceof Pane p) clearCheckBoxes(p);
        }
    }

    private void checkCheckBoxes(Pane container, List<Integer> values) {
        if (container == null) return;
        for (Node node : container.getChildren()) {
            if (node instanceof CheckBox cb) {
                if (values.contains(parseCheckBoxValue(cb.getText()))) {
                    cb.setSelected(true);
                }
            } else if (node instanceof Pane p) {
                checkCheckBoxes(p, values);
            }
        }
    }

    private int parseCheckBoxValue(String text) {
        if (text.matches("\\d+")) {
            return Integer.parseInt(text);
        }
        return switch (text) {
            case "周日" -> 1;
            case "周一" -> 2;
            case "周二" -> 3;
            case "周三" -> 4;
            case "周四" -> 5;
            case "周五" -> 6;
            case "周六" -> 7;
            default -> -1;
        };
    }

    @Override
    protected String getViewKey() {
        return "cron";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                cronExpressionField.textProperty(),
                dateTimeFormatField.textProperty(),
                executionCountCombo.valueProperty()
        );
    }

    @Override
    protected void restoreValues(CronPersistentState state) {
        if (state != null && state.getLastExpression() != null) {
            cronExpressionField.setText(state.getLastExpression());
            dateTimeFormatField.setText(state.getDateTimeFormat());
            executionCountCombo.setValue(state.getExecutionCount());
            onReverseParse();
            onParse();
        }
    }

    @Override
    protected void initDefaultValues() {
        dateTimeFormatField.setText("yyyy-MM-dd EEE HH:mm:ss");
        executionCountCombo.setValue(5);
        autoGenerate();
    }

    @Override
    protected CronPersistentState captureValues() {
        var cronPersistentState = new CronPersistentState();
        cronPersistentState.setLastExpression(cronExpressionField.getText());
        cronPersistentState.setDateTimeFormat(dateTimeFormatField.getText());
        cronPersistentState.setExecutionCount(executionCountCombo.getValue());
        return cronPersistentState;
    }
}