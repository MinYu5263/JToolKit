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
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class ExcelToSqlController extends BaseController<ExcelToSqlPersistentState> {

    // === 列类型常量 ===
    public static final String TYPE_TEXT = "文本";
    public static final String TYPE_NUMBER = "数字";
    public static final String TYPE_NULL = "NULL";
    public static final String TYPE_FUNC = "函数";

    private static final List<String> COLUMN_TYPES = List.of(TYPE_TEXT, TYPE_NUMBER, TYPE_NULL, TYPE_FUNC);

    // Excel 日期序列号基准: 1899-12-30
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    @FXML private PathTextField pathField;
    @FXML private ComboBox<String> sheetCombo;
    @FXML private TextField tableNameField;
    @FXML private ToggleSwitch syncTableNameSwitch;
    @FXML private ToggleSwitch skipHeaderSwitch;
    @FXML private ComboBox<String> sqlModeCombo;

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

    @FXML
    public void initView() {
        // 1. 配置文件选择器过滤
        pathField.getFileChooser().getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls", "*.csv"));

        // 2. 路径变更 → 加载文件
        pathField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                File f = new File(newVal);
                if (f.exists() && f.isFile()) {
                    loadFile(f);
                }
            }
        });

        // 3. SQL 模式
        sqlModeCombo.setItems(FXCollections.observableArrayList("INSERT INTO", "REPLACE INTO", "INSERT IGNORE INTO"));
        sqlModeCombo.getSelectionModel().selectFirst();
        sqlModeCombo.valueProperty().addListener((obs, old, val) -> triggerUpdate());

        // 4. Sheet 切换
        sheetCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                if (syncTableNameSwitch.isSelected()) {
                    tableNameField.setText(toUnderline(val));
                }
                onLoadPreview();
            }
        });

        // 5. 表名变更 → 重新生成
        tableNameField.textProperty().addListener((obs, old, val) -> triggerUpdate());

        // 6. 同步开关
        syncTableNameSwitch.selectedProperty().addListener((obs, old, val) -> {
            tableNameField.setDisable(val);
            if (val && sheetCombo.getValue() != null) {
                tableNameField.setText(toUnderline(sheetCombo.getValue()));
                triggerUpdate();
            }
        });

        // 7. 跳过表头切换 → 重新生成
        skipHeaderSwitch.selectedProperty().addListener((obs, old, val) -> triggerUpdate());

        // 8. 初始化表格
        columnTable.setItems(columnMappings);
        colIndex.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getIndex())));
        colHeader.setCellValueFactory(data -> data.getValue().headerProperty());
        colDbField.setCellValueFactory(data -> data.getValue().dbFieldProperty());
        colDbField.setCellFactory(TextFieldTableCell.forTableColumn());
        colDbField.setOnEditCommit(event -> {
            event.getRowValue().setDbField(event.getNewValue());
            triggerUpdate();
        });

        // 类型列 — ComboBox 编辑器
        colType.setCellValueFactory(data -> data.getValue().typeProperty());
        colType.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(COLUMN_TYPES)));
        colType.setOnEditCommit(event -> {
            event.getRowValue().setType(event.getNewValue());
            triggerUpdate();
        });

        // 附加列 — 文本编辑器（用于日期格式或 SQL 函数表达式）
        colFormat.setCellValueFactory(data -> data.getValue().formatProperty());
        colFormat.setCellFactory(TextFieldTableCell.forTableColumn());
        colFormat.setOnEditCommit(event -> {
            event.getRowValue().setFormat(event.getNewValue());
            triggerUpdate();
        });

        // 9. 历史按钮
        historyBtn.setOnAction(e -> onShowHistory());
    }

    private void triggerUpdate() {
        if (isRestoring) return;
        if (currentFile != null) {
            onGenerate();
        }
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

        FastExcel.read(currentFile, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
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

    private void updateColumns(Map<Integer, String> firstRow) {
        columnMappings.clear();
        firstRow.forEach((index, header) -> {
            String dbField = toUnderline(header);
            columnMappings.add(new ColumnMapping(index, header, dbField, TYPE_TEXT, ""));
        });
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

        // 过滤出需要包含在 INSERT 中的列
        // 排除了：1) 字段名为空的  2) 函数类型且附加字段为空的（自增ID场景）
        List<ColumnMapping> insertCols = columnMappings.stream()
                .filter(c -> c.getDbField() != null && !c.getDbField().isBlank())
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
                StringBuilder sb = new StringBuilder();

                FastExcel.read(currentFile, new AnalysisEventListener<Map<Integer, String>>() {
                    @Override
                    public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                        StringBuilder valSb = new StringBuilder();
                        valSb.append("(");

                        for (int i = 0; i < insertCols.size(); i++) {
                            ColumnMapping col = insertCols.get(i);
                            String rawVal = rowData.get(col.getIndex());
                            String sqlVal = formatSqlValue(rawVal, col);

                            valSb.append(sqlVal);

                            if (i < insertCols.size() - 1) valSb.append(", ");
                        }
                        valSb.append(")");

                        sb.append(String.format("%s %s (%s) VALUES %s;\n",
                                sqlPrefix, tableName, columnsStr, valSb.toString()));
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {}
                }).sheet(sheetCombo.getValue()).headRowNumber(headRow).doRead();

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

    /**
     * 根据列类型和附加字段，将 Excel 原始值格式化为 SQL 值
     */
    private String formatSqlValue(String rawValue, ColumnMapping col) {
        String type = col.getType();
        String format = col.getFormat();

        // NULL 类型 — 始终输出 NULL
        if (TYPE_NULL.equals(type)) {
            return "NULL";
        }

        // 函数类型 — 直接使用附加字段作为 SQL 表达式
        if (TYPE_FUNC.equals(type)) {
            if (format != null && !format.isBlank()) {
                return format; // e.g., NOW(), SYSDATE, NEXTVAL('seq')
            }
            return "NULL"; // 不应到达这里（函数+空格式已在过滤阶段排除）
        }

        // 空值处理
        if (rawValue == null || rawValue.isBlank()) {
            return "NULL";
        }

        String trimmed = rawValue.trim();

        // 数字类型 — 不加引号
        if (TYPE_NUMBER.equals(type)) {
            try {
                // 验证是有效数字
                Double.parseDouble(trimmed);
                return trimmed;
            } catch (NumberFormatException e) {
                // 非数字时回退为 NULL
                return "NULL";
            }
        }

        // 文本类型 (默认) — 加引号
        String displayValue = trimmed;

        // 如果指定了格式，尝试作为日期格式化（Excel 序列号 → 日期字符串）
        if (format != null && !format.isBlank()) {
            try {
                long serial = Long.parseLong(trimmed);
                LocalDate date = EXCEL_EPOCH.plusDays(serial);
                displayValue = date.format(DateTimeFormatter.ofPattern(format));
            } catch (Exception ignored) {
                // 解析失败，保持原值
            }
        }

        return "'" + displayValue.replace("'", "''") + "'";
    }

    private String toUnderline(String camel) {
        if (camel == null) return "";
        return camel.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    // === BaseController 实现 ===

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
                sqlModeCombo.valueProperty()
        );
    }

    @Override
    protected void initDefaultValues() {
        sqlModeCombo.getSelectionModel().selectFirst();
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
        state.setRecentFiles(new ArrayList<>(recentFiles));
        return state;
    }

    // === 内部类：表格映射模型 ===
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
}
