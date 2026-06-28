package com.minyu.jtoolkit.module.excel2sql;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.core.component.PathTextField;
import com.minyu.jtoolkit.module.BaseController;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class ExcelToSqlController extends BaseController<ExcelToSqlPersistentState> {

    public static final String TYPE_TEXT = "文本";
    public static final String TYPE_NUMBER = "数字";
    public static final String TYPE_DATE = "日期";
    public static final String TYPE_NULL = "NULL";
    public static final String TYPE_AUTO = "自增ID";
    public static final String TYPE_FUNC = "函数";

    private static final List<String> COLUMN_TYPES = List.of(TYPE_TEXT, TYPE_NUMBER, TYPE_DATE, TYPE_NULL, TYPE_AUTO, TYPE_FUNC);
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    @FXML private PathTextField pathField;
    @FXML private ComboBox<String> sheetCombo;
    @FXML private TextField tableNameField;
    @FXML private ToggleSwitch syncTableNameSwitch;
    @FXML private ToggleSwitch skipHeaderSwitch;
    @FXML private ComboBox<String> sqlModeCombo;
    @FXML private ComboBox<String> dbDialectCombo;
    @FXML private ComboBox<String> batchSizeCombo;

    @FXML private TableView<ColumnMapping> columnTable;
    @FXML private TableColumn<ColumnMapping, String> colIndex;
    @FXML private TableColumn<ColumnMapping, String> colHeader;
    @FXML private TableColumn<ColumnMapping, String> colDbField;
    @FXML private TableColumn<ColumnMapping, String> colType;
    @FXML private TableColumn<ColumnMapping, String> colFormat;

    @FXML private EnhancedTextArea resultArea;
    @FXML private Label statusLabel;
    @FXML private Button refreshBtn;
    @FXML private Button historyBtn;

    private final ObservableList<ColumnMapping> columnMappings = FXCollections.observableArrayList();
    private final ObservableList<String> recentFiles = FXCollections.observableArrayList();
    private File currentFile;
    private boolean isRestoring = false;
    /** 全局表头配置记忆（key=表头名），跨 Sheet 切换保持，Cell 编辑即时同步 */
    private final Map<String, ColumnMappingData> columnMemory = new HashMap<>();

    @FXML
    public void initView() {
        pathField.getFileChooser().getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls", "*.csv"));

        pathField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                File f = new File(newVal);
                if (f.exists() && f.isFile()) {
                    loadFile(f);
                }
            }
        });

        sqlModeCombo.setItems(FXCollections.observableArrayList("INSERT INTO", "REPLACE INTO", "INSERT IGNORE INTO"));
        sqlModeCombo.getSelectionModel().selectFirst();

        dbDialectCombo.setItems(FXCollections.observableArrayList("MySQL / PostgreSQL", "Oracle"));
        dbDialectCombo.getSelectionModel().selectFirst();

        batchSizeCombo.setItems(FXCollections.observableArrayList("1", "5", "10", "50", "100", "500"));
        batchSizeCombo.setEditable(true);
        batchSizeCombo.getSelectionModel().selectFirst();

        sheetCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                if (syncTableNameSwitch.isSelected()) {
                    tableNameField.setText(toUnderline(val));
                }
                onLoadPreview();
            }
        });

        syncTableNameSwitch.selectedProperty().addListener((obs, old, val) -> {
            tableNameField.setDisable(val);
            if (val && sheetCombo.getValue() != null) {
                tableNameField.setText(toUnderline(sheetCombo.getValue()));
            }
        });

        columnTable.setItems(columnMappings);
        columnTable.setEditable(true);

        colIndex.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getIndex())));
        colHeader.setCellValueFactory(data -> data.getValue().headerProperty());

        colDbField.setCellValueFactory(data -> data.getValue().dbFieldProperty());
        colDbField.setCellFactory(col -> new AlwaysEditingTextFieldCell());

        colType.setCellValueFactory(data -> data.getValue().typeProperty());
        colType.setCellFactory(col -> new AlwaysEditingComboCell());

        colFormat.setCellValueFactory(data -> data.getValue().formatProperty());
        colFormat.setCellFactory(col -> new AlwaysEditingTextFieldCell());

        historyBtn.setOnAction(e -> onShowHistory());
    }

    private void loadFile(File file) {
        currentFile = file;
        pathField.setText(file.getAbsolutePath());
        statusLabel.setText("正在读取 Sheet...");

        addToRecentFiles(file.getAbsolutePath());

        new Thread(() -> {
            try {
                List<String> sheetNames = FastExcel.read(file).build().excelExecutor().sheetList()
                        .stream().map(s -> s.getSheetName()).collect(Collectors.toList());

                Platform.runLater(() -> {
                    isRestoring = true;
                    try {
                        sheetCombo.setItems(FXCollections.observableArrayList(sheetNames));
                        if (!sheetNames.isEmpty()) {
                            sheetCombo.getSelectionModel().selectFirst();
                            if (syncTableNameSwitch.isSelected()) {
                                tableNameField.setText(toUnderline(sheetNames.getFirst()));
                            }
                        }
                    } finally {
                        isRestoring = false;
                    }
                    statusLabel.setText("文件加载成功");
                    onLoadPreview();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("读取失败: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void addToRecentFiles(String path) {
        recentFiles.remove(path);
        recentFiles.addFirst(path);
        if (recentFiles.size() > 20) {
            recentFiles.removeLast();
        }
    }

    @FXML
    public void onLoadPreview() {
        if (currentFile == null || sheetCombo.getValue() == null) return;

        final AtomicBoolean first = new AtomicBoolean(true);

        FastExcel.read(currentFile, new AnalysisEventListener<Map<Integer, Object>>() {
            @Override
            public void invoke(Map<Integer, Object> data, AnalysisContext context) {
                if (first.getAndSet(false)) {
                    Platform.runLater(() -> updateColumns(data));
                }
                throw new RuntimeException("StopReading");
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}

            @Override
            public void onException(Exception exception, AnalysisContext context) {
                if (!"StopReading".equals(exception.getMessage())) {
                    exception.printStackTrace();
                }
            }
        }).sheet(sheetCombo.getValue()).headRowNumber(0).doRead();
    }

    @FXML
    public void onRefresh() {
        if (currentFile == null) {
            statusLabel.setText("请先选择文件");
            return;
        }
        statusLabel.setText("正在刷新...");
        loadFile(currentFile);
    }

    @FXML
    public void onShowHistory() {
        var modalPane = (ModalPane) pathField.getScene().lookup("#main-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException("ModalPane not found in Scene.");
        }

        ListView<String> listView = new ListView<>();
        listView.setItems(recentFiles);
        HBox.setHgrow(listView, Priority.ALWAYS);
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    setOnMouseClicked(null);
                } else {
                    File f = new File(path);
                    setText(f.getName());

                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem deleteItem = new MenuItem("删除此记录");
                    deleteItem.setOnAction(event -> recentFiles.remove(path));
                    contextMenu.getItems().add(deleteItem);
                    setContextMenu(contextMenu);

                    setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                            pathField.setText(path);
                            modalPane.hide();
                        }
                    });
                }
            }
        });

        Button clearAllBtn = new Button("清空所有历史");
        clearAllBtn.getStyleClass().add(Styles.DANGER);
        clearAllBtn.setMaxWidth(Double.MAX_VALUE);
        clearAllBtn.setOnAction(e -> {
            recentFiles.clear();
            modalPane.hide();
        });

        VBox vBox = new VBox(10, new Label("历史记录"), listView, clearAllBtn);
        vBox.setPadding(new Insets(10));

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(vBox);
        modalBox.setMaxWidth(350);
        modalBox.setOnClose(e -> modalPane.hide());

        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);

        modalPane.setAlignment(Pos.CENTER_RIGHT);
        modalPane.usePredefinedTransitionFactories(Side.RIGHT);
        modalPane.show(modalBox);
    }

    private void updateColumns(Map<Integer, Object> firstRow) {
        columnMappings.clear();
        firstRow.forEach((index, value) -> {
            String header = value != null ? value.toString() : "";
            ColumnMappingData saved = columnMemory.get(header);
            if (saved != null) {
                columnMappings.add(new ColumnMapping(index, header,
                        saved.getDbField() != null ? saved.getDbField() : toUnderline(header),
                        saved.getType() != null ? saved.getType() : TYPE_TEXT,
                        saved.getFormat() != null ? saved.getFormat() : ""));
            } else {
                columnMappings.add(new ColumnMapping(index, header, toUnderline(header), TYPE_TEXT, ""));
            }
        });
        // 不清理 columnMemory，跨 Sheet 保持表头配置记忆
    }

    @FXML
    public void onGenerate() {
        if (currentFile == null) {
            statusLabel.setText("请先选择文件");
            return;
        }

        String tableName = tableNameField.getText();
        String sqlPrefix = sqlModeCombo.getValue();
        int headRow = skipHeaderSwitch.isSelected() ? 1 : 0;

        // 过滤列：排除 1) 字段名为空  2) 自增ID+空格式（自增列不参与INSERT）  3) 函数+空格式
        List<ColumnMapping> insertCols = columnMappings.stream()
                .filter(c -> c.getDbField() != null && !c.getDbField().isBlank())
                .filter(c -> !(TYPE_AUTO.equals(c.getType()) && c.getFormat().isBlank()))
                .filter(c -> !(TYPE_FUNC.equals(c.getType()) && c.getFormat().isBlank()))
                .collect(Collectors.toList());

        if (insertCols.isEmpty()) {
            statusLabel.setText("请至少保留一列有效的字段映射");
            return;
        }

        String columnsStr = insertCols.stream().map(ColumnMapping::getDbField).collect(Collectors.joining(", "));

        statusLabel.setText("生成 SQL 中...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                int batchSize;
                try {
                    batchSize = Integer.parseInt(batchSizeCombo.getValue());
                    if (batchSize < 1) throw new NumberFormatException("批次大小必须 >= 1");
                } catch (NumberFormatException e) {
                    throw new RuntimeException("每批插入条数格式错误: " + batchSizeCombo.getValue());
                }

                List<String> rows = new ArrayList<>();
                FastExcel.read(currentFile, new AnalysisEventListener<Map<Integer, Object>>() {
                    @Override
                    public void invoke(Map<Integer, Object> rowData, AnalysisContext context) {
                        StringBuilder valSb = new StringBuilder("(");
                        for (int i = 0; i < insertCols.size(); i++) {
                            ColumnMapping col = insertCols.get(i);
                            Object rawVal = rowData.get(col.getIndex());
                            valSb.append(formatSqlValue(rawVal, col));
                            if (i < insertCols.size() - 1) valSb.append(", ");
                        }
                        valSb.append(")");
                        rows.add(valSb.toString());
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {}
                }).sheet(sheetCombo.getValue()).headRowNumber(headRow).doRead();

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rows.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, rows.size());
                    String valuesBlock = String.join(", ", rows.subList(i, end));
                    sb.append(String.format("%s %s (%s) VALUES %s;\n",
                            sqlPrefix, tableName, columnsStr, valuesBlock));
                }
                return sb.toString();
            }

            @Override
            protected void succeeded() {
                resultArea.setText(getValue());
                statusLabel.setText("生成完成 (" + getValue().split("\n").length + " 行)");
                saveValues();
            }

            @Override
            protected void failed() {
                statusLabel.setText("生成失败: " + getException().getMessage());
                getException().printStackTrace();
            }
        };
        new Thread(task).start();
    }

    /** 根据列类型将 Excel 原始值格式化为 SQL 值 */
    private String formatSqlValue(Object rawValue, ColumnMapping col) {
        String type = col.getType();
        String format = col.getFormat();

        // NULL 类型 — 始终输出 NULL
        if (TYPE_NULL.equals(type)) {
            return "NULL";
        }

        // 自增ID类型 — 空格式则列被过滤不参与生成，有格式则作为序列表达式（如 seq.nextval）
        if (TYPE_AUTO.equals(type)) {
            // 防御：空格式的列应在 onGenerate 过滤阶段就已排除，不应走到这里
            if (format == null || format.isBlank()) {
                throw new RuntimeException("自增ID列附加为空时不应参与生成，请检查列映射配置");
            }
            return format;
        }

        // 函数类型 — 完全忽略 Excel 原值，只取 UI 配置的附加字段
        if (TYPE_FUNC.equals(type)) {
            if (format == null || format.isBlank()) {
                throw new RuntimeException("选择'函数'类型时，[附加]列不能为空，请输入具体的SQL函数名(如 NOW())！");
            }
            return format;
        }

        // 空值处理（公式单元格无法求值的也会是 null）
        if (rawValue == null) {
            return "NULL";
        }

        String strVal = rawValue.toString().trim();
        if (strVal.isEmpty()) {
            return "NULL";
        }

        // 数字类型 — 严格校验，拒绝非数字数据；附加字段控制小数位数
        if (TYPE_NUMBER.equals(type)) {
            double d;
            if (rawValue instanceof Number) {
                d = ((Number) rawValue).doubleValue();
            } else {
                try {
                    d = Double.parseDouble(strVal);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("列数据格式错误，期望数字但遇到非法字符: " + strVal + "。请清理 Excel 格式后再试！");
                }
            }

            // 附加字段：为空保留原始小数位（1.8→1.8, 2.22→2.22），否则按指定位数格式化
            if (format == null || format.isBlank()) {
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);  // 整数去 .0
                }
                return String.valueOf(d);
            }
            try {
                int decimals = Integer.parseInt(format.trim());
                if (decimals < 0) throw new NumberFormatException("小数位数不能为负数");
                return String.format("%." + decimals + "f", d);
            } catch (NumberFormatException e) {
                throw new RuntimeException("列[附加]字段格式错误，期望非负整数(小数位数)但遇到: " + format);
            }
        }

        // 日期类型 — Excel序列号自动转为日期字符串，生成 TO_DATE 函数
        if (TYPE_DATE.equals(type)) {
            String datePattern = (format != null && !format.isBlank()) ? format : "yyyy-MM-dd";
            String dateStr;
            if (rawValue instanceof Number) {
                long serial = ((Number) rawValue).longValue();
                try {
                    LocalDate date = EXCEL_EPOCH.plusDays(serial);
                    dateStr = date.format(DateTimeFormatter.ofPattern(datePattern));
                } catch (Exception e) {
                    throw new RuntimeException("日期转换失败（序列号=" + serial + ", 格式=" + datePattern + "）: " + e.getMessage());
                }
            } else {
                dateStr = strVal;
            }
            if (dateStr.isEmpty()) return "NULL";
            if ("Oracle".equals(dbDialectCombo.getValue())) {
                return "TO_DATE('" + dateStr.replace("'", "''") + "', '" + datePattern + "')";
            }
            return "'" + dateStr.replace("'", "''") + "'";
        }

        // 文本类型 — 加引号
        String displayValue;

        if (rawValue instanceof Number) {
            double d = ((Number) rawValue).doubleValue();

            if (format != null && !format.isBlank()) {
                // 日期序列号格式化
                try {
                    LocalDate date = EXCEL_EPOCH.plusDays((long) d);
                    displayValue = date.format(DateTimeFormatter.ofPattern(format));
                } catch (Exception e) {
                    displayValue = strVal;
                }
            } else if (d == Math.floor(d) && !Double.isInfinite(d)) {
                displayValue = String.valueOf((long) d);
            } else {
                displayValue = String.valueOf(d);
            }
        } else {
            displayValue = strVal;
        }

        return "'" + displayValue.replace("'", "''") + "'";
    }

    private String toUnderline(String camel) {
        if (camel == null) return "";
        return camel.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    /** 同步 columnMemory 并持久化 */
    private void updateColumnMemory(ColumnMapping item) {
        columnMemory.put(item.getHeader(), new ColumnMappingData(
                item.getIndex(), item.getHeader(), item.getDbField(), item.getType(), item.getFormat()));
        saveValues();
    }

    /**
     * 始终显示 TextField 的单元格 — 单击即可编辑，失焦或回车提交
     */
    private class AlwaysEditingTextFieldCell extends TableCell<ColumnMapping, String> {
        private final TextField textField = new TextField();
        private String originalValue = "";

        {
            textField.setMaxWidth(Double.MAX_VALUE);
            textField.focusedProperty().addListener((obs, old, focused) -> {
                if (focused) {
                    originalValue = textField.getText();
                } else {
                    commitIfChanged();
                }
            });
            textField.setOnAction(e -> commitIfChanged());
        }

        private void commitIfChanged() {
            if (isEmpty() || getTableRow() == null) return;
            String newVal = textField.getText();
            if (!newVal.equals(originalValue)) {
                ColumnMapping item = getTableRow().getItem();
                if (item != null) {
                    TableColumn<ColumnMapping, ?> col = getTableColumn();
                    if (col != null && item.dbFieldProperty().equals(col.getCellObservableValue(item))) {
                        item.setDbField(newVal);
                    } else if (col != null && item.formatProperty().equals(col.getCellObservableValue(item))) {
                        item.setFormat(newVal);
                    }
                    updateColumnMemory(item);
                }
                originalValue = newVal;
            }
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                textField.setText(item);
                originalValue = item;
                setGraphic(textField);
                setText(null);
            }
        }
    }

    /**
     * 始终显示 ComboBox 的单元格 — 单击即可下拉选择
     */
    private class AlwaysEditingComboCell extends TableCell<ColumnMapping, String> {
        private final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(COLUMN_TYPES));

        {
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.valueProperty().addListener((obs, old, val) -> {
                if (val != null && !isEmpty() && getTableRow() != null) {
                    ColumnMapping item = getTableRow().getItem();
                    if (item != null && !val.equals(item.getType())) {
                        item.setType(val);
                        updateColumnMemory(item);
                    }
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
            } else {
                combo.setValue(item);
                setGraphic(combo);
                setText(null);
            }
        }
    }

    @Override
    protected String getViewKey() {
        return "tool.data.excel2sql";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                pathField.textProperty(),
                tableNameField.textProperty(),
                syncTableNameSwitch.selectedProperty(),
                skipHeaderSwitch.selectedProperty(),
                sqlModeCombo.valueProperty(),
                dbDialectCombo.valueProperty(),
                batchSizeCombo.valueProperty()
        );
    }

    @Override
    protected void initDefaultValues() {
        sqlModeCombo.getSelectionModel().selectFirst();
        dbDialectCombo.getSelectionModel().selectFirst();
        batchSizeCombo.getSelectionModel().selectFirst();
        skipHeaderSwitch.setSelected(true);
        syncTableNameSwitch.setSelected(true);
        tableNameField.setDisable(true);
    }

    @Override
    protected void restoreValues(ExcelToSqlPersistentState state) {
        if (state == null) return;

        isRestoring = true;
        try {
            if (state.getRecentFiles() != null) {
                recentFiles.setAll(state.getRecentFiles());
            }
            syncTableNameSwitch.setSelected(state.isSyncTableName());
            tableNameField.setDisable(state.isSyncTableName());
            if (state.getTableName() != null) tableNameField.setText(state.getTableName());
            skipHeaderSwitch.setSelected(state.isSkipHeader());
            if (state.getSqlMode() != null) sqlModeCombo.setValue(state.getSqlMode());
            if (state.getLastFilePath() != null) {
                File f = new File(state.getLastFilePath());
                if (f.exists()) pathField.setText(state.getLastFilePath());
            }
            if (state.getDbDialect() != null) dbDialectCombo.setValue(state.getDbDialect());
            if (state.getBatchSize() != null) batchSizeCombo.setValue(state.getBatchSize());
            if (state.getColumnMappings() != null) {
                for (ColumnMappingData m : state.getColumnMappings()) {
                    if (m.getHeader() != null && !m.getHeader().isBlank()) {
                        columnMemory.put(m.getHeader(), m);
                    }
                }
            }
        } finally {
            isRestoring = false;
        }
    }

    @Override
    protected ExcelToSqlPersistentState captureValues() {
        ExcelToSqlPersistentState state = new ExcelToSqlPersistentState();
        state.setLastFilePath(pathField.getText());
        state.setTableName(tableNameField.getText());
        state.setSyncTableName(syncTableNameSwitch.isSelected());
        state.setSkipHeader(skipHeaderSwitch.isSelected());
        state.setSqlMode(sqlModeCombo.getValue());
        state.setDbDialect(dbDialectCombo.getValue());
        state.setBatchSize(batchSizeCombo.getValue());
        state.setRecentFiles(new ArrayList<>(recentFiles));
        state.setColumnMappings(new ArrayList<>(columnMemory.values()));
        return state;
    }

    public static class ColumnMapping {
        private final int index;
        private final SimpleStringProperty header;
        private final SimpleStringProperty dbField;
        private final SimpleStringProperty type;
        private final SimpleStringProperty format;

        public ColumnMapping(int index, String header, String dbField, String type, String format) {
            this.index = index;
            this.header = new SimpleStringProperty(header);
            this.dbField = new SimpleStringProperty(dbField);
            this.type = new SimpleStringProperty(type);
            this.format = new SimpleStringProperty(format);
        }

        public int getIndex() { return index; }
        public String getHeader() { return header.get(); }
        public String getDbField() { return dbField.get(); }
        public void setDbField(String val) { this.dbField.set(val); }
        public String getType() { return type.get(); }
        public void setType(String val) { this.type.set(val); }
        public String getFormat() { return format.get(); }
        public void setFormat(String val) { this.format.set(val); }

        public SimpleStringProperty headerProperty() { return header; }
        public SimpleStringProperty dbFieldProperty() { return dbField; }
        public SimpleStringProperty typeProperty() { return type; }
        public SimpleStringProperty formatProperty() { return format; }
    }

    /** 列映射持久化数据结构 */
    public static class ColumnMappingData {
        private int index;
        private String header;
        private String dbField;
        private String type;
        private String format;

        public ColumnMappingData() {}

        public ColumnMappingData(int index, String header, String dbField, String type, String format) {
            this.index = index; this.header = header; this.dbField = dbField;
            this.type = type; this.format = format;
        }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public String getDbField() { return dbField; }
        public void setDbField(String dbField) { this.dbField = dbField; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
