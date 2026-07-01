package com.minyu.jtoolkit.module.sql2excel;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.module.sql2excel.SqlToExcelPersistentState.DbConnectionProfile;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SqlToExcelController extends BaseController<SqlToExcelPersistentState> {
    private static final double PREVIEW_COLUMN_MIN_WIDTH = 96;
    private static final double PREVIEW_COLUMN_MAX_WIDTH = 280;
    private static final double PREVIEW_COLUMN_CHAR_WIDTH = 8;
    private static final int PREVIEW_COLUMN_SAMPLE_ROWS = 20;
    private static final int EXCEL_SHEET_NAME_MAX_LENGTH = 31;

    // === UI Components ===
    @FXML private ComboBox<DbConnectionProfile> profileCombo;
    @FXML private ComboBox<String> databaseCombo;
    @FXML private Button btnRefreshDb;
    @FXML private Label connStatusLabel;

    @FXML private EnhancedTextArea sqlArea;
    @FXML private Label previewStatusLabel;
    @FXML private TabPane previewTabPane;

    // === Data ===
    private final ObservableList<DbConnectionProfile> profiles = FXCollections.observableArrayList();

    // 防止恢复持久化状态时触发副作用
    private boolean isRestoring = false;

    @FXML
    public void initView() {
        sqlArea.getImportFileChooser().getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("SQL / 文本文件", "*.sql", "*.txt"));

        profileCombo.setItems(profiles);
        profileCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                databaseCombo.getItems().clear();
                // 状态恢复阶段只回显选中项，不触发自动连接，避免数据库未启动时报错
                if (!isRestoring) {
                    onRefreshDatabases();
                }
            }
        });

        sqlArea.textProperty().addListener((obs, old, newVal) -> {
            if (isRestoring) return;
        });

        showEmptyPreviewTab();
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                profileCombo.valueProperty(),
                databaseCombo.valueProperty(),
                sqlArea.textProperty()
        );
    }

    // ================== 连接管理 ==================

    @FXML
    public void onManageConnections() {
        showConnectionManagerDialog();
    }

    /**
     * 刷新数据库列表 (获取 Catalog/Schema)
     */
    @FXML
    public void onRefreshDatabases() {
        DbConnectionProfile profile = profileCombo.getValue();
        if (profile == null) return;

        setConnStatus("正在连接服务器", "status-connecting");
        databaseCombo.setDisable(true);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                try (Connection conn = createConnection(profile, null)) {
                    List<String> dbs = new ArrayList<>();
                    DatabaseMetaData meta = conn.getMetaData();

                    try (ResultSet rs = meta.getCatalogs()) {
                        while (rs.next()) {
                            dbs.add(rs.getString("TABLE_CAT"));
                        }
                    }
                    if (dbs.isEmpty()) {
                        try (ResultSet rs = meta.getSchemas()) {
                            while (rs.next()) {
                                dbs.add(rs.getString("TABLE_SCHEM"));
                            }
                        }
                    }
                    return dbs;
                }
            }

            @Override
            protected void succeeded() {
                databaseCombo.setItems(FXCollections.observableArrayList(getValue()));
                databaseCombo.setDisable(false);
                setConnStatus("数据库连接成功", "status-connected");
            }

            @Override
            protected void failed() {
                databaseCombo.setDisable(false);
                setConnStatus("连接失败: " + getException().getMessage(), "status-error");
                getException().printStackTrace();
            }
        };
        new Thread(task).start();
    }

    /**
     * 创建数据库连接
     */
    private Connection createConnection(DbConnectionProfile profile, String database) throws Exception {
        String url = buildJdbcUrl(profile, database);
        Properties props = new Properties();
        props.setProperty("user", profile.getUsername());
        props.setProperty("password", profile.getPassword());

        props.setProperty("connectTimeout", "5000");
        props.setProperty("socketTimeout", "30000");

        if ("MySQL".equals(profile.getDbType())) {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } else if ("PostgreSQL".equals(profile.getDbType())) {
            Class.forName("org.postgresql.Driver");
        }

        return DriverManager.getConnection(url, props);
    }

    private String buildJdbcUrl(DbConnectionProfile p, String specificDb) {
        String dbName = (specificDb != null && !specificDb.isBlank()) ? specificDb : "";

        if ("MySQL".equals(p.getDbType())) {
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    p.getHost(), p.getPort(), dbName);
        } else if ("PostgreSQL".equals(p.getDbType())) {
            if (dbName.isEmpty()) dbName = "postgres";
            return String.format("jdbc:postgresql://%s:%s/%s", p.getHost(), p.getPort(), dbName);
        }
        return "";
    }

    private void setConnStatus(String text, String styleClass) {
        connStatusLabel.setText(text);
        connStatusLabel.getStyleClass().setAll("status-label", styleClass);
        connStatusLabel.setTooltip(new Tooltip(text));
    }

    /**
     * 重置连接状态为默认 "未连接"
     */
    private void resetConnStatus() {
        connStatusLabel.setText("未连接");
        connStatusLabel.getStyleClass().setAll("status-label");
        connStatusLabel.setTooltip(null);
    }

    // ================== 业务功能 ==================

    /** -- sheet: 注释匹配正则 */
    private static final Pattern SHEET_NAME_PATTERN = Pattern.compile("(?i)--\\s*sheet:\\s*([^\\r\\n]+)");

    /**
     * 解析 SQL 文本，按分号拆分并提取 -- sheet: 注释作为 Sheet 名
     */
    private List<ParsedSqlTask> parseSqlWithSheetNames(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) return List.of();
        List<ParsedSqlTask> result = new ArrayList<>();
        String[] parts = rawSql.split(";");
        int seq = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            seq++;

            Matcher m = SHEET_NAME_PATTERN.matcher(trimmed);
            String sheetName;
            String sql;
            if (m.find()) {
                sheetName = m.group(1).trim();
                sql = m.replaceFirst("").trim();
            } else {
                sheetName = "Sheet" + seq;
                sql = trimmed;
            }
            result.add(new ParsedSqlTask(sheetName, sql));
        }
        return result;
    }

    @FXML
    public void onPreviewCurrent() {
        DbConnectionProfile profile = profileCombo.getValue();
        String selectedDb = databaseCombo.getValue();

        if (profile == null) {
            new Alert(Alert.AlertType.WARNING, "请先选择一个连接").show();
            return;
        }
        if (selectedDb == null || selectedDb.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "请选择或输入目标数据库").show();
            return;
        }

        List<ParsedSqlTask> tasks = parseSqlWithSheetNames(sqlArea.getText());
        if (tasks.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "SQL 语句为空").show();
            return;
        }

        previewStatusLabel.textProperty().unbind();
        previewStatusLabel.setText("预览中，共 " + tasks.size() + " 条 SQL");
        previewTabPane.getTabs().clear();

        Task<List<PreviewResult>> task = new Task<>() {
            @Override
            protected List<PreviewResult> call() throws Exception {
                List<PreviewResult> results = new ArrayList<>();
                try (Connection conn = createConnection(profile, selectedDb)) {
                    for (ParsedSqlTask sqlTask : tasks) {
                        results.add(loadPreviewResult(conn, sqlTask));
                        updateMessage("已预览 " + results.size() + "/" + tasks.size() + " 条 SQL");
                    }
                }
                return results;
            }

            @Override
            protected void succeeded() {
                previewStatusLabel.textProperty().unbind();
                List<PreviewResult> results = getValue();
                previewTabPane.getTabs().clear();
                for (PreviewResult result : results) {
                    previewTabPane.getTabs().add(createPreviewTab(result));
                }
                if (previewTabPane.getTabs().isEmpty()) {
                    showEmptyPreviewTab();
                } else {
                    previewTabPane.getSelectionModel().selectFirst();
                }
                int totalRows = results.stream().mapToInt(result -> result.rows().size()).sum();
                previewStatusLabel.setText("预览完成，共 " + results.size()
                        + " 条 SQL，显示 " + totalRows + " 行");
            }

            @Override
            protected void failed() {
                previewStatusLabel.textProperty().unbind();
                previewStatusLabel.setText("错误");
                showEmptyPreviewTab();
                new Alert(Alert.AlertType.ERROR, "SQL 错误:\n" + getException().getMessage()).show();
            }
        };
        previewStatusLabel.textProperty().bind(task.messageProperty());
        new Thread(task).start();
    }

    private PreviewResult loadPreviewResult(Connection conn, ParsedSqlTask sqlTask) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlTask.getSql())) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                headers.add(meta.getColumnLabel(i));
            }

            List<List<String>> rows = new ArrayList<>();
            int count = 0;
            while (rs.next() && count < 50) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    row.add(val == null ? "" : val.toString());
                }
                rows.add(row);
                count++;
            }

            return new PreviewResult(sqlTask.getSheetName(), headers, rows);
        } catch (SQLException e) {
            throw new SQLException("[" + sqlTask.getSheetName() + "] " + e.getMessage(), e);
        }
    }

    private Tab createPreviewTab(PreviewResult result) {
        TableView<List<String>> table = createPreviewTable();

        for (int i = 0; i < result.headers().size(); i++) {
            final int idx = i;
            TableColumn<List<String>, String> col = new TableColumn<>(result.headers().get(i));
            col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(idx)));
            col.setMinWidth(PREVIEW_COLUMN_MIN_WIDTH);
            col.setPrefWidth(calculatePreviewColumnWidth(result, idx));
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(result.rows()));

        Tab tab = new Tab(result.sheetName() + " (" + result.rows().size() + ")");
        tab.setClosable(false);
        tab.setContent(table);
        tab.setTooltip(new Tooltip(result.sheetName()));
        return tab;
    }

    private void showEmptyPreviewTab() {
        previewTabPane.getTabs().setAll(createEmptyPreviewTab());
        previewTabPane.getSelectionModel().selectFirst();
    }

    private Tab createEmptyPreviewTab() {
        Tab tab = new Tab("预览");
        tab.setClosable(false);
        tab.setContent(createPreviewTable());
        return tab;
    }

    private TableView<List<String>> createPreviewTable() {
        TableView<List<String>> table = new TableView<>();
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.setPlaceholder(new Label("暂无数据"));
        return table;
    }

    private double calculatePreviewColumnWidth(PreviewResult result, int columnIndex) {
        int maxLength = result.headers().get(columnIndex) == null ? 0 : result.headers().get(columnIndex).length();
        int sampleRows = Math.min(result.rows().size(), PREVIEW_COLUMN_SAMPLE_ROWS);
        for (int i = 0; i < sampleRows; i++) {
            String value = result.rows().get(i).get(columnIndex);
            if (value != null) {
                maxLength = Math.max(maxLength, value.length());
            }
        }

        double contentWidth = maxLength * PREVIEW_COLUMN_CHAR_WIDTH + 32;
        return Math.max(PREVIEW_COLUMN_MIN_WIDTH, Math.min(PREVIEW_COLUMN_MAX_WIDTH, contentWidth));
    }

    @FXML
    public void onBatchExport() {
        List<ParsedSqlTask> tasks = parseSqlWithSheetNames(sqlArea.getText());
        if (tasks.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "请先输入 SQL 语句").show();
            return;
        }

        DbConnectionProfile profile = profileCombo.getValue();
        String dbName = databaseCombo.getValue();
        if (profile == null || dbName == null) {
            new Alert(Alert.AlertType.WARNING, "请配置完整的连接和数据库").show();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setInitialFileName("export.xlsx");
        File file = fc.showSaveDialog(sqlArea.getScene().getWindow());
        if (file == null) return;

        setConnStatus("导出中", "status-connecting");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = createConnection(profile, dbName);
                     ExcelWriter writer = FastExcel.write(file).build()) {

                    int sheetIdx = 0;
                    List<String> usedSheetNames = new ArrayList<>();
                    for (ParsedSqlTask pt : tasks) {
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(pt.getSql())) {

                            ResultSetMetaData meta = rs.getMetaData();
                            List<List<String>> head = new ArrayList<>();
                            for (int i = 1; i <= meta.getColumnCount(); i++) {
                                List<String> h = new ArrayList<>();
                                h.add(meta.getColumnLabel(i));
                                head.add(h);
                            }

                            List<List<String>> data = new ArrayList<>();
                            while (rs.next()) {
                                List<String> row = new ArrayList<>();
                                for (int i = 1; i <= meta.getColumnCount(); i++) {
                                    row.add(formatExportValue(rs.getObject(i)));
                                }
                                data.add(row);
                            }

                            String sheetName = uniqueSheetName(pt.getSheetName(), usedSheetNames, sheetIdx + 1);
                            WriteSheet sheet = FastExcel.writerSheet(sheetIdx++, sheetName)
                                    .head(head)
                                    .registerWriteHandler(buildPlainBorderStyleStrategy())
                                    .build();
                            writer.write(data, sheet);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                setConnStatus("导出成功，共 " + tasks.size() + " 个 Sheet", "status-connected");
            }

            @Override
            protected void failed() {
                setConnStatus("导出失败", "status-error");
                new Alert(Alert.AlertType.ERROR, getException().getMessage()).show();
            }
        };
        new Thread(task).start();
    }

    private HorizontalCellStyleStrategy buildPlainBorderStyleStrategy() {
        WriteCellStyle plainBorderStyle = new WriteCellStyle();
        plainBorderStyle.setBorderLeft(BorderStyle.THIN);
        plainBorderStyle.setBorderRight(BorderStyle.THIN);
        plainBorderStyle.setBorderTop(BorderStyle.THIN);
        plainBorderStyle.setBorderBottom(BorderStyle.THIN);
        plainBorderStyle.setFillPatternType(FillPatternType.NO_FILL);
        plainBorderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        WriteFont plainFont = new WriteFont();
        plainFont.setBold(false);
        plainBorderStyle.setWriteFont(plainFont);

        return new HorizontalCellStyleStrategy(plainBorderStyle, plainBorderStyle);
    }

    private String formatExportValue(Object value) throws SQLException {
        if (value == null) return "";
        if (value instanceof Clob clob) {
            long length = clob.length();
            if (length <= 0) return "";
            int readableLength = (int) Math.min(length, Integer.MAX_VALUE);
            return clob.getSubString(1, readableLength);
        }
        if (value instanceof Blob blob) {
            return "[BLOB " + blob.length() + " bytes]";
        }
        if (value instanceof byte[] bytes) {
            return "[BINARY " + bytes.length + " bytes]";
        }
        return value.toString();
    }

    private String uniqueSheetName(String rawName, List<String> usedNames, int fallbackIndex) {
        String baseName = sanitizeSheetName(rawName);
        if (baseName.isBlank()) {
            baseName = "Sheet" + fallbackIndex;
        }

        String name = trimSheetName(baseName);
        int seq = 2;
        while (usedNames.contains(name)) {
            String suffix = " (" + seq++ + ")";
            name = trimSheetName(baseName, suffix.length()) + suffix;
        }
        usedNames.add(name);
        return name;
    }

    private String sanitizeSheetName(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/?*\\[\\]:]", "_").trim();
    }

    private String trimSheetName(String name) {
        return trimSheetName(name, 0);
    }

    private String trimSheetName(String name, int reservedLength) {
        int maxLength = Math.max(1, EXCEL_SHEET_NAME_MAX_LENGTH - reservedLength);
        return name.length() <= maxLength ? name : name.substring(0, maxLength);
    }

    // ================== 连接管理 Modal ==================

    private void showConnectionManagerDialog() {
        var modalPane = (ModalPane) sqlArea.getScene().lookup("#main-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException("ModalPane not found in Scene.");
        }

        ListView<DbConnectionProfile> list = new ListView<>();
        list.setItems(profiles);
        list.getStyleClass().add("connection-list");
        list.setPlaceholder(new Label("暂无连接"));
        VBox.setVgrow(list, Priority.ALWAYS);

        GridPane form = new GridPane();
        form.getStyleClass().add("connection-form");
        form.setHgap(12);
        form.setVgap(12);

        TextField nameField = new TextField();
        ComboBox<String> typeField = new ComboBox<>(FXCollections.observableArrayList("MySQL", "PostgreSQL"));
        TextField hostField = new TextField();
        TextField portField = new TextField();
        TextField userField = new TextField();
        PasswordField passField = new PasswordField();
        Label hintLabel = new Label("选择左侧连接进行编辑，或新建一个连接。");
        hintLabel.getStyleClass().add("status-label");
        Label testStatusLabel = new Label();
        testStatusLabel.getStyleClass().add("status-label");
        testStatusLabel.setWrapText(true);

        nameField.setPromptText("例如: 本地 MySQL");
        hostField.setPromptText("localhost");
        portField.setPromptText("3306 / 5432");
        userField.setPromptText("root / postgres");
        passField.setPromptText("数据库密码");

        form.addRow(0, new Label("连接名称:"), nameField);
        form.addRow(1, new Label("数据库类型:"), typeField);
        form.addRow(2, new Label("主机:"), hostField);
        form.addRow(3, new Label("端口:"), portField);
        form.addRow(4, new Label("用户名:"), userField);
        form.addRow(5, new Label("密码:"), passField);
        form.add(testStatusLabel, 1, 6);

        Button btnNew = new Button("新建");
        btnNew.getStyleClass().add(Styles.ACCENT);
        Button btnDelete = new Button("删除");
        btnDelete.getStyleClass().add(Styles.DANGER);
        Button btnTest = new Button("测试连接");
        Button btnUse = new Button("确定");
        Button btnClose = new Button("关闭");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, new Label("连接管理"), headerSpacer, btnNew);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("connection-manager-header");

        VBox leftPane = new VBox(10, header, list);
        leftPane.getStyleClass().add("connection-manager-left");
        leftPane.setPrefWidth(260);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(10, btnDelete, footerSpacer, btnTest, btnUse, btnClose);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Region formSpacer = new Region();
        VBox.setVgrow(formSpacer, Priority.ALWAYS);
        VBox rightPane = new VBox(14, hintLabel, form, formSpacer, footer);
        rightPane.getStyleClass().add("connection-manager-right");
        rightPane.setPrefWidth(460);

        HBox content = new HBox(leftPane, rightPane);
        content.getStyleClass().addAll("sql2excel", "connection-manager");
        content.setPrefSize(760, 460);

        final boolean[] loading = {false};

        Runnable refreshFormDisabled = () -> {
            boolean disabled = list.getSelectionModel().getSelectedItem() == null;
            form.setDisable(disabled);
            btnDelete.setDisable(disabled);
            btnTest.setDisable(disabled);
            btnUse.setDisable(disabled);
        };

        Runnable writeFormToSelection = () -> {
            if (loading[0]) return;
            DbConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            selected.setName(nameField.getText());
            selected.setDbType(typeField.getValue());
            selected.setHost(hostField.getText());
            selected.setPort(portField.getText());
            selected.setUsername(userField.getText());
            selected.setPassword(passField.getText());
            list.refresh();
            saveValues();
        };

        list.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            loading[0] = true;
            if (p != null) {
                nameField.setText(p.getName());
                typeField.setValue(p.getDbType() != null ? p.getDbType() : "MySQL");
                hostField.setText(p.getHost());
                portField.setText(p.getPort());
                userField.setText(p.getUsername());
                passField.setText(p.getPassword());
                hintLabel.setText("正在编辑: " + p.getName());
                testStatusLabel.setText("");
                testStatusLabel.getStyleClass().setAll("status-label");
            } else {
                nameField.clear();
                typeField.getSelectionModel().clearSelection();
                hostField.clear();
                portField.clear();
                userField.clear();
                passField.clear();
                hintLabel.setText("选择左侧连接进行编辑，或新建一个连接。");
                testStatusLabel.setText("");
                testStatusLabel.getStyleClass().setAll("status-label");
            }
            loading[0] = false;
            refreshFormDisabled.run();
        });

        nameField.textProperty().addListener((obs, old, val) -> writeFormToSelection.run());
        hostField.textProperty().addListener((obs, old, val) -> writeFormToSelection.run());
        portField.textProperty().addListener((obs, old, val) -> writeFormToSelection.run());
        userField.textProperty().addListener((obs, old, val) -> writeFormToSelection.run());
        passField.textProperty().addListener((obs, old, val) -> writeFormToSelection.run());
        typeField.valueProperty().addListener((obs, old, val) -> {
            if (!loading[0] && val != null) {
                String currentPort = portField.getText();
                if (currentPort == null || currentPort.isBlank()
                        || "3306".equals(currentPort) || "5432".equals(currentPort)) {
                    portField.setText(defaultPort(val));
                }
            }
            writeFormToSelection.run();
        });

        btnNew.setOnAction(e -> {
            DbConnectionProfile profile = new DbConnectionProfile(
                    uniqueConnectionName(), "MySQL", "localhost", "3306", "root", "");
            profiles.add(profile);
            list.getSelectionModel().select(profile);
            list.scrollTo(profile);
            nameField.requestFocus();
            nameField.selectAll();
            saveValues();
        });

        btnDelete.setOnAction(e -> {
            DbConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            int index = list.getSelectionModel().getSelectedIndex();
            profiles.remove(selected);
            if (selected == profileCombo.getValue()) {
                profileCombo.getSelectionModel().clearSelection();
                databaseCombo.getItems().clear();
                databaseCombo.setValue(null);
                resetConnStatus();
            }
            if (!profiles.isEmpty()) {
                list.getSelectionModel().select(Math.min(index, profiles.size() - 1));
            }
            saveValues();
        });

        btnTest.setOnAction(e -> {
            DbConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            testStatusLabel.setText("正在测试连接...");
            testStatusLabel.getStyleClass().setAll("status-label", "status-connecting");
            btnTest.setDisable(true);
            Task<Void> testTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try (Connection ignored = createConnection(selected, null)) {
                        return null;
                    }
                }

                @Override
                protected void succeeded() {
                    testStatusLabel.setText("连接成功");
                    testStatusLabel.getStyleClass().setAll("status-label", "status-connected");
                    btnTest.setDisable(false);
                }

                @Override
                protected void failed() {
                    testStatusLabel.setText("连接失败: " + getException().getMessage());
                    testStatusLabel.getStyleClass().setAll("status-label", "status-error");
                    btnTest.setDisable(false);
                }
            };
            new Thread(testTask).start();
        });

        btnUse.setOnAction(e -> {
            DbConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            profileCombo.getSelectionModel().select(selected);
            modalPane.hide();
        });

        btnClose.setOnAction(e -> modalPane.hide());

        if (profiles.isEmpty()) {
            btnNew.fire();
        } else {
            DbConnectionProfile current = profileCombo.getValue();
            if (current != null && profiles.contains(current)) {
                list.getSelectionModel().select(current);
            } else {
                list.getSelectionModel().selectFirst();
            }
        }
        refreshFormDisabled.run();

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(content);
        modalBox.setMaxSize(800, 500);
        modalBox.setOnClose(e -> modalPane.hide());

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
    }

    private String uniqueConnectionName() {
        String base = "新连接";
        int seq = 1;
        String name = base;
        while (true) {
            String candidate = name;
            if (profiles.stream().noneMatch(profile -> candidate.equals(profile.getName()))) {
                return candidate;
            }
            seq++;
            name = base + " " + seq;
        }
    }

    private String defaultPort(String dbType) {
        return "PostgreSQL".equals(dbType) ? "5432" : "3306";
    }

    // ================== 持久化 ==================

    @Override
    protected String getViewKey() {
        return "tool.data.sql2excel_v3";
    }

    @Override
    protected void restoreValues(SqlToExcelPersistentState state) {
        if (state == null) return;

        isRestoring = true;
        try {
            if (state.getProfiles() != null) profiles.setAll(state.getProfiles());

            // 恢复上次选中的连接
            if (state.getLastSelectedProfileName() != null) {
                for (DbConnectionProfile p : profiles) {
                    if (state.getLastSelectedProfileName().equals(p.getName())) {
                        profileCombo.getSelectionModel().select(p);
                        break;
                    }
                }
            }

            // 恢复上次选中的数据库
            if (state.getLastSelectedDatabase() != null) {
                databaseCombo.setValue(state.getLastSelectedDatabase());
            }

            // 恢复 SQL 文本框内容
            if (state.getSavedSqlContent() != null) {
                sqlArea.setText(state.getSavedSqlContent());
            }
        } finally {
            isRestoring = false;
        }
    }

    @Override
    protected void initDefaultValues() {
        // 首次使用无需特殊初始化
    }

    @Override
    protected SqlToExcelPersistentState captureValues() {
        SqlToExcelPersistentState state = new SqlToExcelPersistentState();
        state.setProfiles(new ArrayList<>(profiles));

        DbConnectionProfile selectedProfile = profileCombo.getValue();
        state.setLastSelectedProfileName(selectedProfile != null ? selectedProfile.getName() : null);
        state.setLastSelectedDatabase(databaseCombo.getValue());

        state.setSavedSqlContent(sqlArea.getText());
        return state;
    }

    // ================== 内部类 ==================

    /**
     * 解析后的 SQL 任务：包含 Sheet 名称与可执行 SQL
     */
    public static class ParsedSqlTask {
        private final String sheetName;
        private final String sql;

        public ParsedSqlTask(String sheetName, String sql) {
            this.sheetName = sheetName;
            this.sql = sql;
        }

        public String getSheetName() { return sheetName; }
        public String getSql() { return sql; }
    }

    private record PreviewResult(String sheetName, List<String> headers, List<List<String>> rows) {
    }
}
