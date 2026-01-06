package com.minyu.jtoolkit.module.sql2excel;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.module.sql2excel.SqlToExcelPersistentState.DbConnectionProfile;
import com.minyu.jtoolkit.module.sql2excel.SqlToExcelPersistentState.QueryTaskState;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SqlToExcelController extends BaseController<SqlToExcelPersistentState> {

    // === UI Components ===
    @FXML private ComboBox<DbConnectionProfile> profileCombo;
    @FXML private ComboBox<String> databaseCombo;
    @FXML private Button btnRefreshDb;
    @FXML private Label connStatusLabel;

    @FXML private ListView<QueryTask> taskListView;
    @FXML private VBox editorArea;
    @FXML private TextField sheetNameField;
    @FXML private TextArea sqlArea;
    @FXML private Label previewStatusLabel;
    @FXML private TableView<List<String>> previewTable;

    // === Data Models ===
    private final ObservableList<DbConnectionProfile> profiles = FXCollections.observableArrayList();
    private final ObservableList<QueryTask> tasks = FXCollections.observableArrayList();
    private QueryTask currentEditingTask;

    // 当前活跃的数据库连接 (复用)
    private Connection activeConnection;

    @FXML
    public void initView() {
        // 1. 初始化下拉框
        profileCombo.setItems(profiles);
        profileCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                // 切换连接时，先清空数据库列表，尝试自动连接并获取数据库
                databaseCombo.getItems().clear();
                onRefreshDatabases();
            }
        });

        taskListView.setItems(tasks);
        taskListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            currentEditingTask = newVal;
            if (newVal != null) {
                editorArea.setDisable(false);
                sheetNameField.setText(newVal.getSheetName());
                sqlArea.setText(newVal.getSql());
                previewTable.getItems().clear();
                previewTable.getColumns().clear();
            } else {
                editorArea.setDisable(true);
            }
        });

        // 2. 双向绑定编辑器
        sheetNameField.textProperty().addListener((obs, old, newVal) -> {
            if (currentEditingTask != null) {
                currentEditingTask.setSheetName(newVal);
                taskListView.refresh();
                saveValues();
            }
        });
        sqlArea.textProperty().addListener((obs, old, newVal) -> {
            if (currentEditingTask != null) {
                currentEditingTask.setSql(newVal);
                saveValues();
            }
        });

        // 3. 恢复状态

    }

    // ================== 连接管理 (核心修复点) ==================

    @FXML
    public void onManageConnections() {
        showConnectionManagerDialog();
    }

    /**
     * 刷新数据库列表 (获取 Schema)
     */
    @FXML
    public void onRefreshDatabases() {
        DbConnectionProfile profile = profileCombo.getValue();
        if (profile == null) return;

        connStatusLabel.setText("正在连接服务器");
        connStatusLabel.setStyle("-fx-text-fill: orange;");
        databaseCombo.setDisable(true);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                // 这里只建立基础连接，不指定具体 database，用于获取列表
                try (Connection conn = createConnection(profile, null)) {
                    List<String> dbs = new ArrayList<>();
                    DatabaseMetaData meta = conn.getMetaData();

                    // 获取数据库列表 (Catalogs)
                    try (ResultSet rs = meta.getCatalogs()) {
                        while (rs.next()) {
                            dbs.add(rs.getString("TABLE_CAT"));
                        }
                    }
                    // 如果是 Oracle 或其他特殊数据库，可能需要 getSchemas()
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
                connStatusLabel.setText("服务器连接成功");
                connStatusLabel.setStyle("-fx-text-fill: green;");

                // 如果有上次选中的 DB，尝试恢复
                // (此处逻辑略简单，实际可结合 ViewData 恢复)
            }

            @Override
            protected void failed() {
                databaseCombo.setDisable(false);
                connStatusLabel.setText("连接失败: " + getException().getMessage());
                connStatusLabel.setStyle("-fx-text-fill: red;");
                getException().printStackTrace();
            }
        };
        new Thread(task).start();
    }

    /**
     * 创建数据库连接 (修复了驱动加载问题)
     */
    private Connection createConnection(DbConnectionProfile profile, String database) throws Exception {
        String url = buildJdbcUrl(profile, database);
        Properties props = new Properties();
        props.setProperty("user", profile.getUsername());
        props.setProperty("password", profile.getPassword());

        // 设置连接超时 (关键修复)
        props.setProperty("connectTimeout", "5000"); // 5秒超时
        props.setProperty("socketTimeout", "30000");

        // 显式加载驱动 (防止某些环境 SPI 失效)
        if ("MySQL".equals(profile.getDbType())) {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } else if ("PostgreSQL".equals(profile.getDbType())) {
            Class.forName("org.postgresql.Driver");
        }

        return DriverManager.getConnection(url, props);
    }

    private String buildJdbcUrl(DbConnectionProfile p, String specificDb) {
        String dbName = (specificDb != null && !specificDb.isBlank()) ? specificDb : "";

        // 注意：MySQL 如果不填库名，也可以连接，但不能执行 Select。
        // PostgreSQL 默认通常连接到 'postgres' 库来查询其他库列表

        if ("MySQL".equals(p.getDbType())) {
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    p.getHost(), p.getPort(), dbName);
        } else if ("PostgreSQL".equals(p.getDbType())) {
            if(dbName.isEmpty()) dbName = "postgres"; // PG 需要默认库
            return String.format("jdbc:postgresql://%s:%s/%s", p.getHost(), p.getPort(), dbName);
        }
        return "";
    }

    // ================== 业务功能 (预览/导出) ==================

    @FXML
    public void onPreviewCurrent() {
        if (currentEditingTask == null) return;
        DbConnectionProfile profile = profileCombo.getValue();
        String selectedDb = databaseCombo.getValue(); // 可能是用户手输的，也可能是选的

        if (profile == null) {
            new Alert(Alert.AlertType.WARNING, "请先选择一个连接").show();
            return;
        }
        if (selectedDb == null || selectedDb.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "请选择或输入目标数据库").show();
            return;
        }

        previewStatusLabel.setText("查询中");
        previewTable.getItems().clear();
        previewTable.getColumns().clear();

        Task<Void> task = new Task<>() {
            List<String> headers = new ArrayList<>();
            List<List<String>> data = new ArrayList<>();

            @Override
            protected Void call() throws Exception {
                try (Connection conn = createConnection(profile, selectedDb);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(currentEditingTask.getSql())) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    for (int i = 1; i <= colCount; i++) headers.add(meta.getColumnLabel(i));

                    int count = 0;
                    while (rs.next() && count < 50) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            row.add(val == null ? "" : val.toString());
                        }
                        data.add(row);
                        count++;
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                previewStatusLabel.setText("预览完成 (前" + data.size() + "行)");
                for (int i = 0; i < headers.size(); i++) {
                    final int idx = i;
                    TableColumn<List<String>, String> col = new TableColumn<>(headers.get(i));
                    col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(idx)));
                    previewTable.getColumns().add(col);
                }
                previewTable.setItems(FXCollections.observableArrayList(data));
            }

            @Override
            protected void failed() {
                previewStatusLabel.setText("错误");
                new Alert(Alert.AlertType.ERROR, "SQL 错误:\n" + getException().getMessage()).show();
            }
        };
        new Thread(task).start();
    }

    @FXML
    public void onBatchExport() {
        //  (与之前逻辑基本一致，只需修改获取 Connection 的部分) 
        // 使用 createConnection(profileCombo.getValue(), databaseCombo.getValue())
        // 此处为了节省篇幅，省略重复代码，请参考上一版本 onBatchExport，仅替换获取连接逻辑即可。
        if (tasks.isEmpty()) return;
        DbConnectionProfile profile = profileCombo.getValue();
        String dbName = databaseCombo.getValue();
        if(profile == null || dbName == null) {
            new Alert(Alert.AlertType.WARNING, "请配置完整的连接和数据库").show();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setInitialFileName("export.xlsx");
        File file = fc.showSaveDialog(editorArea.getScene().getWindow());
        if(file == null) return;

        connStatusLabel.setText("导出中");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 关键点：整个批量导出只用一个 Connection
                try (Connection conn = createConnection(profile, dbName);
                     ExcelWriter writer = FastExcel.write(file).build()) {

                    int sheetIdx = 0;
                    for(QueryTask t : tasks) {
                        String sName = t.getSheetName() == null ? "Sheet"+sheetIdx : t.getSheetName();
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(t.getSql())) {

                            // 构建动态表头
                            ResultSetMetaData meta = rs.getMetaData();
                            List<List<String>> head = new ArrayList<>();
                            for(int i=1; i<=meta.getColumnCount(); i++) {
                                List<String> h = new ArrayList<>();
                                h.add(meta.getColumnLabel(i));
                                head.add(h);
                            }

                            // 读取数据
                            List<List<Object>> data = new ArrayList<>();
                            while(rs.next()) {
                                List<Object> row = new ArrayList<>();
                                for(int i=1; i<=meta.getColumnCount(); i++) row.add(rs.getObject(i));
                                data.add(row);
                            }

                            WriteSheet sheet = FastExcel.writerSheet(sheetIdx++, sName).head(head).build();
                            writer.write(data, sheet);
                        }
                    }
                }
                return null;
            }
            @Override protected void succeeded() { connStatusLabel.setText("导出成功"); }
            @Override protected void failed() {
                connStatusLabel.setText("导出失败");
                new Alert(Alert.AlertType.ERROR, getException().getMessage()).show();
            }
        };
        new Thread(task).start();
    }

    // ================== 纯 Java 代码构建连接管理 Dialog ==================

    private void showConnectionManagerDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("连接管理");
        dialog.setHeaderText("配置数据库连接信息");

        // 布局：左侧列表，右侧表单
        SplitPane split = new SplitPane();
        split.setPrefSize(600, 400);

        ListView<DbConnectionProfile> list = new ListView<>();
        list.setItems(profiles); // 直接操作主列表

        // 右侧表单
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(20));

        TextField nameField = new TextField();
        ComboBox<String> typeField = new ComboBox<>(FXCollections.observableArrayList("MySQL", "PostgreSQL"));
        typeField.getSelectionModel().select("MySQL");
        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("3306");
        TextField userField = new TextField("root");
        PasswordField passField = new PasswordField();

        form.addRow(0, new Label("别名:"), nameField);
        form.addRow(1, new Label("类型:"), typeField);
        form.addRow(2, new Label("Host:"), hostField);
        form.addRow(3, new Label("Port:"), portField);
        form.addRow(4, new Label("User:"), userField);
        form.addRow(5, new Label("Pass:"), passField);

        // 按钮栏
        HBox btns = new HBox(10);
        Button btnSave = new Button("保存/更新");
        Button btnDelete = new Button("删除");
        Button btnNew = new Button("新建");
        btns.getChildren().addAll(btnNew, btnSave, btnDelete);
        form.add(btns, 1, 6);

        // 逻辑
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            if (p != null) {
                nameField.setText(p.getName());
                typeField.setValue(p.getDbType());
                hostField.setText(p.getHost());
                portField.setText(p.getPort());
                userField.setText(p.getUsername());
                passField.setText(p.getPassword());
            }
        });

        btnNew.setOnAction(e -> {
            list.getSelectionModel().clearSelection();
            nameField.clear();
            nameField.setPromptText("新连接");
            hostField.setText("localhost"); userField.clear(); passField.clear();
        });

        btnSave.setOnAction(e -> {
            String name = nameField.getText();
            if (name.isBlank()) return;

            DbConnectionProfile p = list.getSelectionModel().getSelectedItem();
            if (p == null) {
                p = new DbConnectionProfile();
                profiles.add(p);
            }
            p.setName(name);
            p.setDbType(typeField.getValue());
            p.setHost(hostField.getText());
            p.setPort(portField.getText());
            p.setUsername(userField.getText());
            p.setPassword(passField.getText());

            list.refresh();
            profileCombo.setItems(profiles); // 刷新主界面下拉
            saveValues(); // 持久化
        });

        btnDelete.setOnAction(e -> {
            DbConnectionProfile p = list.getSelectionModel().getSelectedItem();
            if (p != null) {
                profiles.remove(p);
                saveValues();
            }
        });

        split.getItems().addAll(list, form);
        split.setDividerPositions(0.35);

        dialog.getDialogPane().setContent(split);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ================== BaseController 任务管理 ==================
    @FXML public void onAddTask() {
        tasks.add(new QueryTask("Sheet" + (tasks.size() + 1), "SELECT * FROM "));
        saveValues();
    }
    @FXML public void onRemoveTask() {
        QueryTask t = taskListView.getSelectionModel().getSelectedItem();
        if (t != null) {
            tasks.remove(t);
            saveValues();
        }
    }

    // ================== 持久化 ==================
    @Override
    protected String getViewKey() {
        return "tool.data.sql2excel_v2";
    }

    @Override
    protected Class<SqlToExcelPersistentState> getStorageType() {
        return SqlToExcelPersistentState.class;
    }

    @Override
    protected void restoreValues(SqlToExcelPersistentState state) {
        if(state == null) return;
        if(state.getProfiles() != null) profiles.setAll(state.getProfiles());
        if(state.getTasks() != null) {
            for(QueryTaskState s : state.getTasks()) tasks.add(new QueryTask(s.getSheetName(), s.getSql()));
        }
    }

    @Override
    protected SqlToExcelPersistentState captureValues() {
        SqlToExcelPersistentState state = new SqlToExcelPersistentState();
        state.setProfiles(new ArrayList<>(profiles));
        List<QueryTaskState> ts = tasks.stream().map(t -> new QueryTaskState(t.getSheetName(), t.getSql())).collect(Collectors.toList());
        state.setTasks(ts);
        return state;
    }

    @Data
    public static class QueryTask {
        private String sheetName;
        private String sql;
        public QueryTask(String n, String s) { this.sheetName = n; this.sql = s; }
        @Override public String toString() { return sheetName; }
    }
}