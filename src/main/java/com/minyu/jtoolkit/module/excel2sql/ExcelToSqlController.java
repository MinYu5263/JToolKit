package com.minyu.jtoolkit.module.excel2sql;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import com.minyu.jtoolkit.module.BaseController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExcelToSqlController extends BaseController<ExcelToSqlPersistentState> {

    @FXML private TextField filePathField;
    @FXML private ComboBox<String> sheetCombo;
    @FXML private TextField tableNameField;
    @FXML private CheckBox chkSkipHeader;
    @FXML private ChoiceBox<String> sqlModeChoice;

    @FXML private TableView<ColumnMapping> columnTable;
    @FXML private TableColumn<ColumnMapping, String> colIndex;
    @FXML private TableColumn<ColumnMapping, String> colHeader;
    @FXML private TableColumn<ColumnMapping, String> colDbField;

    @FXML private TextArea resultArea;
    @FXML private Label statusLabel;

    private final ObservableList<ColumnMapping> columnMappings = FXCollections.observableArrayList();
    private File currentFile;

    @FXML
    public void initView() {
        initUI();

    }

    private void initUI() {
        // SQL 模式
        sqlModeChoice.setItems(FXCollections.observableArrayList("INSERT INTO", "REPLACE INTO", "INSERT IGNORE INTO"));
        sqlModeChoice.getSelectionModel().selectFirst();

        // 表格初始化
        columnTable.setItems(columnMappings);
        colIndex.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getIndex())));
        colHeader.setCellValueFactory(data -> data.getValue().headerProperty());

        // 数据库字段列允许编辑
        colDbField.setCellValueFactory(data -> data.getValue().dbFieldProperty());
        colDbField.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    @FXML
    public void onBrowse() {
        FileChooser fc = new FileChooser();
        fc.setTitle("选择 Excel 文件");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls", "*.csv"));
        File file = fc.showOpenDialog(filePathField.getScene().getWindow());
        if (file != null) {
            loadFile(file);
        }
    }

    private void loadFile(File file) {
        currentFile = file;
        filePathField.setText(file.getAbsolutePath());
        statusLabel.setText("正在读取 Sheet...");

        // 异步读取 Sheet 列表
        new Thread(() -> {
            try {
                // FastExcel 读取所有 Sheet 名称
                List<String> sheetNames = FastExcel.read(file).build().excelExecutor().sheetList()
                        .stream().map(s -> s.getSheetName()).collect(Collectors.toList());

                Platform.runLater(() -> {
                    sheetCombo.setItems(FXCollections.observableArrayList(sheetNames));
                    if (!sheetNames.isEmpty()) {
                        sheetCombo.getSelectionModel().selectFirst();
                        onLoadPreview(); // 自动加载预览
                    }
                    statusLabel.setText("文件加载成功");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("读取失败: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void onLoadPreview() {
        if (currentFile == null || sheetCombo.getValue() == null) return;

        // 使用 FastExcel 读取第一行作为表头
        FastExcel.read(currentFile, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // 读取到第一行数据后，更新 UI 并中断读取
                Platform.runLater(() -> updateColumns(data));
                throw new RuntimeException("StopReading"); // 故意抛出异常来停止读取后续行
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}

            @Override
            public void onException(Exception exception, AnalysisContext context) {
                // 忽略我们故意抛出的停止异常
                if (!"StopReading".equals(exception.getMessage())) {
                    exception.printStackTrace();
                }
            }
        }).sheet(sheetCombo.getValue()).headRowNumber(0).doRead();
    }

    private void updateColumns(Map<Integer, String> firstRow) {
        columnMappings.clear();
        // 将 Excel 表头自动转为下划线格式作为默认 DB 字段名
        firstRow.forEach((index, header) -> {
            String dbField = toUnderline(header);
            columnMappings.add(new ColumnMapping(index, header, dbField));
        });
    }

    @FXML
    public void onGenerate() {
        if (currentFile == null) {
            statusLabel.setText("请先选择文件");
            return;
        }

        String tableName = tableNameField.getText();
        String sqlPrefix = sqlModeChoice.getValue();
        int headRow = chkSkipHeader.isSelected() ? 1 : 0; // 跳过几行

        // 过滤出有效的列映射（DB字段不为空的）
        List<ColumnMapping> validCols = columnMappings.stream()
                .filter(c -> c.getDbField() != null && !c.getDbField().isBlank())
                .collect(Collectors.toList());

        if (validCols.isEmpty()) {
            statusLabel.setText("请至少保留一列有效的字段映射");
            return;
        }

        String columnsStr = validCols.stream().map(ColumnMapping::getDbField).collect(Collectors.joining(", "));

        statusLabel.setText("生成 SQL 中...");

        // 异步任务生成 SQL
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                StringBuilder sb = new StringBuilder();

                // FastExcel 读取数据
                FastExcel.read(currentFile, new AnalysisEventListener<Map<Integer, String>>() {
                    @Override
                    public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                        StringBuilder valSb = new StringBuilder();
                        valSb.append("(");

                        for (int i = 0; i < validCols.size(); i++) {
                            ColumnMapping col = validCols.get(i);
                            String val = rowData.get(col.getIndex());

                            if (val == null) {
                                valSb.append("NULL");
                            } else {
                                // 简单的 SQL 转义：单引号变双单引号
                                valSb.append("'").append(val.replace("'", "''")).append("'");
                            }

                            if (i < validCols.size() - 1) valSb.append(", ");
                        }
                        valSb.append(")");

                        // 拼接完整 SQL
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
                statusLabel.setText("生成完成 (" + resultArea.getText().split("\n").length + " 行)");
            }

            @Override
            protected void failed() {
                statusLabel.setText("生成失败: " + getException().getMessage());
                getException().printStackTrace();
            }
        };
        new Thread(task).start();
    }

    @FXML
    public void onCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(resultArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("已复制到剪贴板");
    }

    // 简单的驼峰转下划线工具方法
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
    protected Class<ExcelToSqlPersistentState> getStorageType() {
        return ExcelToSqlPersistentState.class;
    }

    @Override
    protected void restoreValues(ExcelToSqlPersistentState state) {
        if (state == null) return;

        if (state.getLastFilePath() != null) {
            File f = new File(state.getLastFilePath());
            if (f.exists()) loadFile(f);
        }
        if (state.getTableName() != null) tableNameField.setText(state.getTableName());
        chkSkipHeader.setSelected(state.isSkipHeader());
        if (state.getSqlMode() != null) sqlModeChoice.setValue(state.getSqlMode());
    }

    @Override
    protected ExcelToSqlPersistentState captureValues() {
        ExcelToSqlPersistentState state = new ExcelToSqlPersistentState();
        state.setLastFilePath(filePathField.getText());
        state.setTableName(tableNameField.getText());
        state.setSkipHeader(chkSkipHeader.isSelected());
        state.setSqlMode(sqlModeChoice.getValue());
        return state;
    }

    // === 内部类：表格映射模型 ===
    public static class ColumnMapping {
        private int index;
        private SimpleStringProperty header;
        private SimpleStringProperty dbField;

        public ColumnMapping(int index, String header, String dbField) {
            this.index = index;
            this.header = new SimpleStringProperty(header);
            this.dbField = new SimpleStringProperty(dbField);
        }

        public int getIndex() { return index; }
        public String getHeader() { return header.get(); }
        public String getDbField() { return dbField.get(); }
        public void setDbField(String val) { this.dbField.set(val); }

        public SimpleStringProperty headerProperty() { return header; }
        public SimpleStringProperty dbFieldProperty() { return dbField; }
    }
}