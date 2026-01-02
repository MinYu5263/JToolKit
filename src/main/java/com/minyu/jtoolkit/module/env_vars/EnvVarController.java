package com.minyu.jtoolkit.module.env_vars;

import com.minyu.jtoolkit.core.util.PrivilegeUtils;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.EnvVarService;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Lazy(false)
@Component
public class EnvVarController extends BaseController<EnvVarPersistentState> {

    // === UI 组件 ===
    @FXML
    private Label osInfoLabel;
    @FXML
    private TextField searchField; // 新增搜索框

    // 用户变量表格
    @FXML
    private TableView<EnvVarItem> userTable;
    @FXML
    private TableColumn<EnvVarItem, String> userKeyCol;
    @FXML
    private TableColumn<EnvVarItem, String> userValueCol;
    @FXML
    private Button btnAddUser;
    @FXML
    private Button btnEditUser;
    @FXML
    private Button btnDelUser;

    // 系统变量表格
    @FXML
    private TableView<EnvVarItem> sysTable;
    @FXML
    private TableColumn<EnvVarItem, String> sysKeyCol;
    @FXML
    private TableColumn<EnvVarItem, String> sysValueCol;
    @FXML
    private Button btnAddSys;
    @FXML
    private Button btnEditSys;
    @FXML
    private Button btnDelSys;

    // === 数据源 ===
    private final ObservableList<EnvVarItem> userList = FXCollections.observableArrayList();
    private final ObservableList<EnvVarItem> sysList = FXCollections.observableArrayList();

    // 过滤后的视图数据源 (用于搜索)
    private FilteredList<EnvVarItem> filteredUserList;
    private FilteredList<EnvVarItem> filteredSysList;

    // === 服务 ===
    private final EnvVarService envVarService;

    public EnvVarController(EnvVarService envVarService) {
        this.envVarService = envVarService;
    }

    public void initView() {
        initTables();
        initSearchFilter();
        checkPermission();
        onRefreshAll();
    }

    private void initTables() {
        // 1. 初始化 FilteredList，默认不过滤 (predicate = always true)
        filteredUserList = new FilteredList<>(userList, p -> true);
        filteredSysList = new FilteredList<>(sysList, p -> true);

        // 2. 绑定列
        userKeyCol.setCellValueFactory(cell -> cell.getValue().keyProperty());
        userValueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());

        // 3. 将 TableView 绑定到过滤后的列表，而不是原始列表
        userTable.setItems(filteredUserList);

        sysKeyCol.setCellValueFactory(cell -> cell.getValue().keyProperty());
        sysValueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        sysTable.setItems(filteredSysList);

        // 4. 双击编辑事件
        userTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !btnEditUser.isDisabled()) onEditUserVar();
        });
        sysTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !btnEditSys.isDisabled()) onEditSysVar();
        });
    }

    /**
     * 初始化搜索过滤器
     */
    private void initSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // 定义过滤逻辑：如果 key 或 value 包含搜索文本（忽略大小写），则保留
            filteredUserList.setPredicate(item -> filterLogic(item, newValue));
            filteredSysList.setPredicate(item -> filterLogic(item, newValue));
        });
    }

    private boolean filterLogic(EnvVarItem item, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        String lowerCaseFilter = searchText.toLowerCase();

        if (item.getKey().toLowerCase().contains(lowerCaseFilter)) {
            return true;
        }
        return item.getValue().toLowerCase().contains(lowerCaseFilter);
    }

    private void checkPermission() {
        boolean writable = envVarService.isWritable();

        if (!writable) {
            osInfoLabel.setText("(当前系统不支持修改，仅只读)");
            btnAddUser.setDisable(true);
            btnEditUser.setDisable(true);
            btnDelUser.setDisable(true);
            btnAddSys.setDisable(true);
            btnEditSys.setDisable(true);
            btnDelSys.setDisable(true);
        } else {
            osInfoLabel.setText("(当前系统: Windows - 可编辑)");
        }
    }

    @FXML
    public void onRefreshAll() {
        refreshList(userList, envVarService.getUserVariables());
        refreshList(sysList, envVarService.getSystemVariables());
    }

    private void refreshList(ObservableList<EnvVarItem> list, Map<String, String> data) {
        list.clear();
        data.forEach((k, v) -> list.add(new EnvVarItem(k, v)));
    }

    // ================= 用户变量操作 =================

    @FXML
    public void onAddUserVar() {
        showEditDialog("新建用户变量", null, null).ifPresent(pair -> {
            envVarService.setUserVariable(pair.key, pair.value);
            onRefreshAll();
        });
    }

    @FXML
    public void onEditUserVar() {
        EnvVarItem selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        smartEdit("编辑用户变量", selected.getKey(), selected.getValue(), false);
    }

    @FXML
    public void onDeleteUserVar() {
        EnvVarItem selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (showConfirmDialog("确认删除", "确定要删除用户变量 '" + selected.getKey() + "' 吗？")) {
            envVarService.deleteUserVariable(selected.getKey());
            onRefreshAll();
        }
    }

    private boolean ensureAdmin() {
        if (PrivilegeUtils.isAdmin()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("权限请求");
        alert.setHeaderText("此操作需要管理员权限");
        alert.setContentText("修改系统环境变量需要提升权限。\n\n点击“确定”将以管理员身份重启应用。");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            PrivilegeUtils.relaunchAsAdmin();
        }

        return false;
    }

    // ================= 系统变量操作 =================

    @FXML
    public void onAddSysVar() {
        if (!ensureAdmin()) return;
        showEditDialog("新建系统变量", null, null).ifPresent(pair -> {
            try {
                envVarService.setSystemVariable(pair.key, pair.value);
                onRefreshAll();
            } catch (Exception e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        });
    }

    @FXML
    public void onEditSysVar() {
        if (!ensureAdmin()) return;
        EnvVarItem selected = sysTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        // 调用智能编辑
        smartEdit("编辑系统变量", selected.getKey(), selected.getValue(), true);
    }

    @FXML
    public void onDeleteSysVar() {
        if (!ensureAdmin()) return;
        EnvVarItem selected = sysTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (showConfirmDialog("警告：删除系统变量", "确定要删除系统变量 '" + selected.getKey() + "' 吗？\n如果删除了核心系统变量，可能会导致系统故障！")) {
            try {
                envVarService.deleteSystemVariable(selected.getKey());
                onRefreshAll();
            } catch (Exception e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        }
    }

    // ================= 辅助方法与类 =================

    // 简单的数据包装类，方便 TableView 绑定
    public static class EnvVarItem {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;

        public EnvVarItem(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }

        public String getKey() {
            return key.get();
        }

        public String getValue() {
            return value.get();
        }

        public SimpleStringProperty keyProperty() {
            return key;
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }
    }

    // 简单的 Key-Value 数据对
    private record VarPair(String key, String value) {
    }

    // 弹出一个包含两个输入框的 Dialog
    private Optional<VarPair> showEditDialog(String title, String key, String value) {
        Dialog<VarPair> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        // 设置一个合理的默认宽度
        dialog.getDialogPane().setPrefWidth(600);

        ButtonType okButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15); //稍微增加垂直间距
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));

        // --- 关键布局修复：设置列宽约束 ---
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80); // 保证左侧 Label 不会被压缩
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS); // 右侧输入框自动撑满

        grid.getColumnConstraints().addAll(col1, col2);

        // 变量名输入框
        TextField keyField = new TextField(key);
        keyField.setPromptText("例如：JAVA_HOME");

        // 变量值输入框 (改为 TextField 以匹配单行路径的 UI 风格)
        TextField valueField = new TextField(value);
        valueField.setPromptText("变量值路径...");

        // --- 新增：浏览按钮栏 ---
        Button btnBrowseDir = new Button("浏览目录(D)...");
        Button btnBrowseFile = new Button("浏览文件(F)...");

        // 浏览目录逻辑
        btnBrowseDir.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("选择目录");
            // 尝试定位到当前已输入的路径
            if (valueField.getText() != null && !valueField.getText().isBlank()) {
                File f = new File(valueField.getText());
                if (f.exists() && f.isDirectory()) dc.setInitialDirectory(f);
            }
            File selected = dc.showDialog(dialog.getOwner());
            if (selected != null) {
                valueField.setText(selected.getAbsolutePath());
            }
        });

        // 浏览文件逻辑
        btnBrowseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择文件");
            if (valueField.getText() != null && !valueField.getText().isBlank()) {
                File f = new File(valueField.getText());
                // 如果当前是文件，定位到其父目录；如果是目录，定位到该目录
                if (f.exists()) {
                    if (f.isDirectory()) fc.setInitialDirectory(f);
                    else fc.setInitialDirectory(f.getParentFile());
                }
            }
            File selected = fc.showOpenDialog(dialog.getOwner());
            if (selected != null) {
                valueField.setText(selected.getAbsolutePath());
            }
        });

        HBox buttonBox = new HBox(10, btnBrowseDir, btnBrowseFile);

        // 组装 Grid
        // Row 0: 变量名
        grid.add(new Label("变量名(N):"), 0, 0);
        grid.add(keyField, 1, 0);

        // Row 1: 变量值
        grid.add(new Label("变量值(V):"), 0, 1);
        grid.add(valueField, 1, 1);

        // Row 2: 浏览按钮 (放在第1列，即输入框下方)
        grid.add(buttonBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // 焦点控制：如果是新建，焦点在 key；如果是编辑，焦点在 value
        javafx.application.Platform.runLater(() -> {
            if (key == null || key.isEmpty()) {
                keyField.requestFocus();
            } else {
                valueField.requestFocus();
                valueField.selectAll(); // 方便直接替换
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new VarPair(keyField.getText(), valueField.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private boolean showConfirmDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(osInfoLabel.getScene().getWindow());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(osInfoLabel.getScene().getWindow());
        alert.showAndWait();
    }

    /**
     * 智能打开编辑对话框
     * 如果包含分隔符，打开列表编辑器；否则打开普通编辑器
     */
    private void smartEdit(String title, String key, String value, boolean isSystem) {
        // 判断是否包含路径分隔符 (Windows是分号)
        boolean isList = value != null && value.contains(java.io.File.pathSeparator);

        if (isList) {
            // 使用新做的路径编辑器
            PathEditorDialog dialog = new PathEditorDialog(searchField.getScene().getWindow(), key, value);
            dialog.showAndWait().ifPresent(newValue -> {
                // 保存逻辑
                if (isSystem) {
                    try {
                        envVarService.setSystemVariable(key, newValue);
                        onRefreshAll();
                    } catch (Exception e) {
                        showErrorDialog("操作失败", e.getMessage());
                    }
                } else {
                    envVarService.setUserVariable(key, newValue);
                    onRefreshAll();
                }
            });
        } else {
            // 使用原来的简单 Key-Value 编辑器
            showEditDialog(title, key, value).ifPresent(pair -> {
                if (isSystem) {
                    try {
                        envVarService.setSystemVariable(pair.key(), pair.value());
                        onRefreshAll();
                    } catch (Exception e) {
                        showErrorDialog("操作失败", e.getMessage());
                    }
                } else {
                    envVarService.setUserVariable(pair.key(), pair.value());
                    onRefreshAll();
                }
            });
        }
    }

    // === BaseController 实现 ===
    @Override
    protected String getViewKey() {
        return "env_vars_manager";
    }

    @Override
    protected Class<EnvVarPersistentState> getStorageType() {
        return EnvVarPersistentState.class;
    }

    @Override
    protected void restoreValues(EnvVarPersistentState state) {
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of();
    }

    @Override
    protected EnvVarPersistentState captureValues() {
        return new EnvVarPersistentState();
    }
}