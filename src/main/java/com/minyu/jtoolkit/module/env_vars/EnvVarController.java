package com.minyu.jtoolkit.module.env_vars;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.core.util.AppLifecycleUtils;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.EnvVarService;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Lazy(false)
@Component
public class EnvVarController extends BaseController<EnvVarPersistentState> {

    @FXML
    private CustomTextField userSearchField;
    @FXML
    private CustomTextField sysSearchField;

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

    private final ObservableList<EnvVarItem> userList = FXCollections.observableArrayList();
    private final ObservableList<EnvVarItem> sysList = FXCollections.observableArrayList();

    private FilteredList<EnvVarItem> filteredUserList;
    private FilteredList<EnvVarItem> filteredSysList;

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
        filteredUserList = new FilteredList<>(userList, p -> true);
        filteredSysList = new FilteredList<>(sysList, p -> true);

        userKeyCol.setCellValueFactory(cell -> cell.getValue().keyProperty());
        userValueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());

        userTable.setItems(filteredUserList);

        sysKeyCol.setCellValueFactory(cell -> cell.getValue().keyProperty());
        sysValueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        sysTable.setItems(filteredSysList);

        userTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !btnEditUser.isDisabled()) onEditUserVar();
        });
        sysTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !btnEditSys.isDisabled()) onEditSysVar();
        });
    }

    private void initSearchFilter() {
        userSearchField.textProperty().addListener((obs, old, val) ->
                filteredUserList.setPredicate(item -> filterLogic(item, val)));
        sysSearchField.textProperty().addListener((obs, old, val) ->
                filteredSysList.setPredicate(item -> filterLogic(item, val)));
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
        boolean userWritable = envVarService.isUserWritable();
        boolean systemWritable = envVarService.isSystemWritable();

        btnAddUser.setDisable(!userWritable);
        btnEditUser.setDisable(!userWritable);
        btnDelUser.setDisable(!userWritable);
        btnAddSys.setDisable(!systemWritable);
        btnEditSys.setDisable(!systemWritable);
        btnDelSys.setDisable(!systemWritable);
    }

    @FXML
    public void onRefreshAll() {
        onRefreshUser();
        onRefreshSys();
    }

    @FXML
    public void onRefreshUser() {
        refreshList(userList, envVarService.getUserVariables());
    }

    @FXML
    public void onRefreshSys() {
        refreshList(sysList, envVarService.getSystemVariables());
    }

    private void refreshList(ObservableList<EnvVarItem> list, Map<String, String> data) {
        list.clear();
        data.forEach((k, v) -> list.add(new EnvVarItem(k, v)));
    }

    @FXML
    public void onAddUserVar() {
        showEditModal("新建用户变量", null, null, pair -> {
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
        if (AppLifecycleUtils.isAdmin()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("权限请求");
        alert.setHeaderText("此操作需要管理员权限");
        alert.setContentText("修改系统环境变量需要提升权限。\n\n点击“确定”将以管理员身份重启应用。");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            AppLifecycleUtils.restartAsAdmin();
        }

        return false;
    }

    @FXML
    public void onAddSysVar() {
        if (!ensureAdmin()) return;
        showEditModal("新建系统变量", null, null, pair -> {
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

    private record VarPair(String key, String value) {
    }

    private void showEditModal(String title, String key, String value, Consumer<VarPair> saveHandler) {
        ModalPane modalPane = getMainModalPane();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 10, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        TextField keyField = new TextField(key);
        keyField.setPromptText("例如：JAVA_HOME");

        TextField valueField = new TextField(value);
        valueField.setPromptText("变量值路径");

        Button btnBrowseDir = new Button("浏览目录(D)");
        Button btnBrowseFile = new Button("浏览文件(F)");

        btnBrowseDir.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("选择目录");
            if (valueField.getText() != null && !valueField.getText().isBlank()) {
                File f = new File(valueField.getText());
                if (f.exists() && f.isDirectory()) dc.setInitialDirectory(f);
            }
            File selected = dc.showDialog(valueField.getScene().getWindow());
            if (selected != null) {
                valueField.setText(selected.getAbsolutePath());
            }
        });

        btnBrowseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择文件");
            if (valueField.getText() != null && !valueField.getText().isBlank()) {
                File f = new File(valueField.getText());
                if (f.exists()) {
                    if (f.isDirectory()) fc.setInitialDirectory(f);
                    else fc.setInitialDirectory(f.getParentFile());
                }
            }
            File selected = fc.showOpenDialog(valueField.getScene().getWindow());
            if (selected != null) {
                valueField.setText(selected.getAbsolutePath());
            }
        });

        HBox buttonBox = new HBox(10, btnBrowseDir, btnBrowseFile);

        grid.add(new Label("变量名(N):"), 0, 0);
        grid.add(keyField, 1, 0);

        grid.add(new Label("变量值(V):"), 0, 1);
        grid.add(valueField, 1, 1);

        grid.add(buttonBox, 1, 2);

        Button btnCancel = new Button("取消");
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> modalPane.hide());

        Button btnSave = new Button("保存");
        btnSave.getStyleClass().add(Styles.ACCENT);
        btnSave.setDefaultButton(true);
        btnSave.setOnAction(e -> {
            saveHandler.accept(new VarPair(keyField.getText(), valueField.getText()));
            modalPane.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnSave);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(Styles.TITLE_4);

        VBox body = new VBox(14, titleLabel, grid, footer);
        body.setPadding(new Insets(16));

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(body);
        modalBox.setMaxSize(600, 260);

        AnchorPane.setTopAnchor(body, 0.0);
        AnchorPane.setBottomAnchor(body, 0.0);
        AnchorPane.setLeftAnchor(body, 0.0);
        AnchorPane.setRightAnchor(body, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);

        javafx.application.Platform.runLater(() -> {
            if (key == null || key.isEmpty()) {
                keyField.requestFocus();
            } else {
                valueField.requestFocus();
                valueField.selectAll();
            }
        });
    }

    private boolean showConfirmDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(userSearchField.getScene().getWindow());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(userSearchField.getScene().getWindow());
        alert.showAndWait();
    }

    private void smartEdit(String title, String key, String value, boolean isSystem) {
        boolean isList = value != null && value.contains(java.io.File.pathSeparator);

        if (isList) {
            PathEditorDialog dialog = new PathEditorDialog(key, value, newValue -> {
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
            dialog.show(userSearchField.getScene());
        } else {
            showEditModal(title, key, value, pair -> {
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

    private ModalPane getMainModalPane() {
        var modalPane = (ModalPane) userSearchField.getScene().lookup("#main-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException("ModalPane not found in Scene.");
        }
        return modalPane;
    }

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
