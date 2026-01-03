package com.minyu.jtoolkit.module.git_config;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.GitConfigService;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GitConfigController extends BaseController<GitConfigPersistentState> {

    // === UI 组件 ===
    @FXML
    private ListView<GitScopeItem> scopeListView;
    @FXML
    private Button btnRemoveRepo;
    @FXML
    private Label currentScopeLabel;
    @FXML
    private Label currentPathLabel;

    // 常用设置 Tab
    @FXML
    private TextField fieldName;
    @FXML
    private TextField fieldEmail;

    // ToggleSwitch 替换了 ToggleButton
    @FXML
    private ToggleSwitch toggleProxy;
    @FXML
    private GridPane proxyConfigPane;
    @FXML
    private ChoiceBox<String> choiceProtocol;
    @FXML
    private TextField fieldProxyHost;

    // 所有配置 Tab
    @FXML private CustomTextField searchConfigField;
    @FXML private TableView<ConfigItem> allConfigTable;
    @FXML private TableColumn<ConfigItem, String> colKey;
    @FXML private TableColumn<ConfigItem, String> colValue;

    // === 数据 ===
    private final ObservableList<GitScopeItem> scopeList = FXCollections.observableArrayList();
    private final ObservableList<ConfigItem> allConfigList = FXCollections.observableArrayList();
    private FilteredList<ConfigItem> filteredConfigList;

    private final GitConfigService gitService;

    public GitConfigController(GitConfigService gitService) {
        this.gitService = gitService;
    }

    public void initView() {
        initScopeList();
        initForms();
        initAllConfigTable();
    }

    private void initScopeList() {
        scopeListView.setItems(scopeList);
        scopeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GitScopeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.displayName());
                    if (item.isGlobal()) {
                        setGraphic(new FontIcon(Material2MZ.PUBLIC));
                    } else {
                        setGraphic(new FontIcon(Material2AL.FOLDER));
                    }
                }
            }
        });

        scopeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadConfigFor(newVal);
                btnRemoveRepo.setDisable(newVal.isGlobal());
            }
        });

        if (scopeList.isEmpty()) {
            scopeList.add(new GitScopeItem("全局配置 (Global)", null, true));
        }

        Platform.runLater(() -> {
            if (!scopeList.isEmpty()) scopeListView.getSelectionModel().selectFirst();
        });
    }

    private void initForms() {
        choiceProtocol.setItems(FXCollections.observableArrayList("http", "https", "socks5"));
        choiceProtocol.setValue("http");

        // 使用 ToggleSwitch 的监听逻辑
        toggleProxy.selectedProperty().addListener((obs, oldVal, newVal) -> {
            toggleProxy.setText(newVal ? "启用" : "禁用");
            proxyConfigPane.setDisable(!newVal);
        });
    }

    private void initAllConfigTable() {
        filteredConfigList = new FilteredList<>(allConfigList, p -> true);
        searchConfigField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredConfigList.setPredicate(item -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return item.key().toLowerCase().contains(lower) || item.value().toLowerCase().contains(lower);
            });
        });

        allConfigTable.setItems(filteredConfigList);
        colKey.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().key()));
        colValue.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().value()));

        allConfigTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) onEditRawConfig();
        });
    }

    // === 业务逻辑 ===

    @FXML
    public void onOpenSSHDialog() {
        new SshKeyManagerDialog(gitService).showAndWait();
    }

    private void loadConfigFor(GitScopeItem item) {
        currentScopeLabel.setText(item.displayName());
        currentPathLabel.setText(item.isGlobal() ? "Global Configuration" : item.path().getAbsolutePath());
        File dir = item.path();

        // 1. 加载常用表单
        fieldName.setText(gitService.getConfig("user.name", dir));
        fieldEmail.setText(gitService.getConfig("user.email", dir));

        String proxy = gitService.getConfig("http.proxy", dir);
        if (proxy != null && !proxy.isBlank()) {
            toggleProxy.setSelected(true);
            if (proxy.startsWith("socks5://")) {
                choiceProtocol.setValue("socks5");
                fieldProxyHost.setText(proxy.replace("socks5://", ""));
            } else if (proxy.startsWith("https://")) {
                choiceProtocol.setValue("https");
                fieldProxyHost.setText(proxy.replace("https://", ""));
            } else {
                choiceProtocol.setValue("http");
                fieldProxyHost.setText(proxy.replace("http://", ""));
            }
        } else {
            toggleProxy.setSelected(false);
        }

        // 2. 加载全量配置
        refreshAllConfigTable(dir);
    }

    @FXML
    public void onRefreshAllConfig() {
        GitScopeItem item = scopeListView.getSelectionModel().getSelectedItem();
        if (item != null) refreshAllConfigTable(item.path());
    }

    private void refreshAllConfigTable(File dir) {
        allConfigList.clear();
        Map<String, String> configs = gitService.getAllConfigs(dir);
        configs.forEach((k, v) -> allConfigList.add(new ConfigItem(k, v)));
    }

    @FXML
    public void onSaveConfig() {
        GitScopeItem item = scopeListView.getSelectionModel().getSelectedItem();
        if (item == null) return;
        File dir = item.path();

        try {
            setConfigHelper("user.name", fieldName.getText(), dir);
            setConfigHelper("user.email", fieldEmail.getText(), dir);

            if (toggleProxy.isSelected()) {
                String fullProxy = choiceProtocol.getValue() + "://" + fieldProxyHost.getText();
                gitService.setConfig("http.proxy", fullProxy, dir);
                gitService.setConfig("https.proxy", fullProxy, dir);
            } else {
                gitService.unsetConfig("http.proxy", dir);
                gitService.unsetConfig("https.proxy", dir);
            }

            // 保存完顺便刷新全量列表
            refreshAllConfigTable(dir);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Git 配置已更新！");
            alert.initOwner(fieldName.getScene().getWindow());
            alert.show();
        } catch (Exception e) {
            log.error("Save config failed", e);
        }
    }

    private void setConfigHelper(String key, String value, File dir) {
        if (value == null || value.isBlank()) {
            gitService.unsetConfig(key, dir);
        } else {
            gitService.setConfig(key, value, dir);
        }
    }

    @FXML
    public void onAddRepo() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("选择本地 Git 仓库目录");
        File dir = dc.showDialog(scopeListView.getScene().getWindow());
        if (dir != null) {
            File gitDir = new File(dir, ".git");
            if (!gitDir.exists()) {
                new Alert(Alert.AlertType.WARNING, "该目录不是 Git 仓库").show();
                return;
            }
            GitScopeItem newItem = new GitScopeItem(dir.getName(), dir, false);
            // 查重
            if (scopeList.stream().noneMatch(i -> !i.isGlobal() && i.path().equals(dir))) {
                scopeList.add(newItem);
                scopeListView.getSelectionModel().select(newItem);
            }
        }
    }

    @FXML
    public void onRemoveRepo() {
        GitScopeItem item = scopeListView.getSelectionModel().getSelectedItem();
        if (item != null && !item.isGlobal()) {
            scopeList.remove(item);
            if (!scopeList.isEmpty()) scopeListView.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void onAddRawConfig() {
        showEditDialog("添加配置", "", "").ifPresent(pair -> {
            GitScopeItem item = scopeListView.getSelectionModel().getSelectedItem();
            if (item != null && !pair.key.isBlank()) {
                gitService.setConfig(pair.key, pair.value, item.path());
                refreshAllConfigTable(item.path());
            }
        });
    }

    public void onEditRawConfig() {
        ConfigItem selected = allConfigTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showEditDialog("编辑配置", selected.key(), selected.value()).ifPresent(pair -> {
            GitScopeItem item = scopeListView.getSelectionModel().getSelectedItem();
            if (item != null) {
                gitService.setConfig(pair.key, pair.value, item.path());
                refreshAllConfigTable(item.path());
            }
        });
    }

    // 简易编辑弹窗
    private Optional<ConfigPair> showEditDialog(String title, String key, String value) {
        Dialog<ConfigPair> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        TextField keyField = new TextField(key);
        keyField.setPromptText("Key (e.g. core.editor)");
        TextField valField = new TextField(value);
        valField.setPromptText("Value");
        GridPane.setHgrow(valField, Priority.ALWAYS);

        grid.add(new Label("Key:"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? new ConfigPair(keyField.getText(), valField.getText()) : null);
        return dialog.showAndWait();
    }

    // === 数据恢复逻辑 ===

    @Override
    protected String getViewKey() {
        return "git_config";
    }

    @Override
    protected Class<GitConfigPersistentState> getStorageType() {
        return GitConfigPersistentState.class;
    }

    @Override
    protected void restoreValues(GitConfigPersistentState state) {
        scopeList.clear();
        // 恢复数据时，也必须先加 Global
        scopeList.add(new GitScopeItem("全局配置 (Global)", null, true));

        if (state.getLocalRepoPaths() != null) {
            for (String path : state.getLocalRepoPaths()) {
                File f = new File(path);
                if (f.exists() && new File(f, ".git").exists()) {
                    scopeList.add(new GitScopeItem(f.getName(), f, false));
                }
            }
        }
        // 恢复后默认选中第一个
        Platform.runLater(() -> scopeListView.getSelectionModel().selectFirst());
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(scopeList);
    }

    @Override
    protected GitConfigPersistentState captureValues() {
        GitConfigPersistentState state = new GitConfigPersistentState();
        List<String> paths = scopeList.stream()
                .filter(i -> !i.isGlobal())
                .map(i -> i.path().getAbsolutePath())
                .toList();
        state.setLocalRepoPaths(paths);
        return state;
    }

    // 内部类
    public record GitScopeItem(String displayName, File path, boolean isGlobal) {
    }

    public record ConfigItem(String key, String value) {
    }

    private record ConfigPair(String key, String value) {
    }
}