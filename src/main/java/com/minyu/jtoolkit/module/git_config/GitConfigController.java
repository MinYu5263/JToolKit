package com.minyu.jtoolkit.module.git_config;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.core.component.ConfigCard;
import com.minyu.jtoolkit.core.component.EnhancedTextField;
import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.GitConfigService;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GitConfigController extends BaseController<GitConfigPersistentState> {

    // === UI 组件 ===
    @FXML
    private ComboBox<GitScopeItem> scopeComboBox;
    @FXML
    private Button btnRemoveRepo;

    // 用户信息
    @FXML
    private EnhancedTextField fieldName;
    @FXML
    private EnhancedTextField fieldEmail;

    // 网络代理
    @FXML
    private ToggleSwitch toggleProxy;
    @FXML
    private ConfigCard proxyProtocolCard;
    @FXML
    private ConfigCard proxyHostCard;
    @FXML
    private ComboBox<String> choiceProtocol;
    @FXML
    private EnhancedTextField fieldProxyHost;

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
        scopeComboBox.setItems(scopeList);
        scopeComboBox.setCellFactory(lv -> new ListCell<>() {
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
        scopeComboBox.setButtonCell(new ListCell<>() {
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

        scopeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadConfigFor(newVal);
                btnRemoveRepo.setDisable(newVal.isGlobal());
            }
        });

        if (scopeList.isEmpty()) {
            scopeList.add(new GitScopeItem("全局配置 (Global)", null, true));
        }

        Platform.runLater(() -> {
            if (!scopeList.isEmpty()) scopeComboBox.getSelectionModel().selectFirst();
        });
    }

    private void initForms() {
        choiceProtocol.setItems(FXCollections.observableArrayList("http", "https", "socks5"));
        choiceProtocol.setValue("http");

        // ToggleSwitch 控制代理配置卡片显示/隐藏
        toggleProxy.selectedProperty().addListener((obs, oldVal, newVal) -> {
            toggleProxy.setText(newVal ? "启用" : "禁用");
            proxyProtocolCard.setVisible(newVal);
            proxyProtocolCard.setManaged(newVal);
            proxyHostCard.setVisible(newVal);
            proxyHostCard.setManaged(newVal);
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
        showSshKeyManager();
    }

    private void loadConfigFor(GitScopeItem item) {
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
        GitScopeItem item = scopeComboBox.getSelectionModel().getSelectedItem();
        if (item != null) refreshAllConfigTable(item.path());
    }

    private void refreshAllConfigTable(File dir) {
        allConfigList.clear();
        Map<String, String> configs = gitService.getAllConfigs(dir);
        configs.forEach((k, v) -> allConfigList.add(new ConfigItem(k, v)));
    }

    @FXML
    public void onSaveConfig() {
        GitScopeItem item = scopeComboBox.getSelectionModel().getSelectedItem();
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
        File dir = dc.showDialog(scopeComboBox.getScene().getWindow());
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
                scopeComboBox.getSelectionModel().select(newItem);
            }
        }
    }

    @FXML
    public void onRemoveRepo() {
        GitScopeItem item = scopeComboBox.getSelectionModel().getSelectedItem();
        if (item != null && !item.isGlobal()) {
            scopeList.remove(item);
            if (!scopeList.isEmpty()) scopeComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void onAddRawConfig() {
        showConfigEditModal("添加配置", "", "");
    }

    public void onEditRawConfig() {
        ConfigItem selected = allConfigTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showConfigEditModal("编辑配置", selected.key(), selected.value());
    }

    private void showConfigEditModal(String title, String key, String value) {
        GitScopeItem item = scopeComboBox.getSelectionModel().getSelectedItem();
        if (item == null) return;

        var modalPane = (ModalPane) fieldName.getScene().lookup("#main-modal-pane");
        if (modalPane == null) return;

        TextField keyField = new TextField(key);
        keyField.setPromptText("Key (e.g. core.editor)");
        TextField valField = new TextField(value);
        valField.setPromptText("Value");
        GridPane.setHgrow(valField, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Key:"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valField, 1, 1);

        Button btnCancel = new Button("取消");
        btnCancel.setOnAction(e -> modalPane.hide());

        Button btnOk = new Button("确定");
        btnOk.getStyleClass().add(Styles.ACCENT);
        btnOk.setOnAction(e -> {
            String k = keyField.getText();
            String v = valField.getText();
            if (k != null && !k.isBlank()) {
                gitService.setConfig(k, v, item.path());
                refreshAllConfigTable(item.path());
            }
            modalPane.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnOk);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(10, new Label(title), grid, footer);
        content.setPadding(new Insets(10));

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(content);
        modalBox.setMaxSize(400, 250);

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
    }

    // === SSH 密钥管理 Modal ===

    private void showSshKeyManager() {
        var modalPane = (ModalPane) fieldName.getScene().lookup("#main-modal-pane");
        if (modalPane == null) return;

        ListView<File> keyList = new ListView<>();
        TextArea keyContentArea = new TextArea();
        Label keyPathLabel = new Label("选择左侧密钥查看详情");

        keyList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(new FontIcon(Material2MZ.VPN_KEY));
                }
            }
        });
        keyList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                keyPathLabel.setText(newVal.getAbsolutePath());
                keyContentArea.setText(gitService.readPublicKey(newVal));
            } else {
                keyPathLabel.setText("");
                keyContentArea.clear();
            }
        });
        keyList.getItems().setAll(gitService.listPublicKeys());
        VBox.setVgrow(keyList, Priority.ALWAYS);

        Button btnGenerate = new Button("生成新密钥");
        btnGenerate.setMaxWidth(Double.MAX_VALUE);
        btnGenerate.setOnAction(e -> showGenerateKeyDialog(keyList));

        Button btnRename = new Button("重命名");
        btnRename.setMaxWidth(Double.MAX_VALUE);
        btnRename.setDisable(true);
        btnRename.setOnAction(e -> {
            File selected = keyList.getSelectionModel().getSelectedItem();
            if (selected != null) showRenameKeyDialog(keyList, selected);
        });
        keyList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, val) -> btnRename.setDisable(val == null));

        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));
        leftPane.getChildren().addAll(new Label("本地公钥列表:"), keyList, btnGenerate, btnRename);

        keyContentArea.setEditable(false);
        keyContentArea.setWrapText(true);
        keyContentArea.setPromptText("公钥内容将显示在这里");
        VBox.setVgrow(keyContentArea, Priority.ALWAYS);

        Button btnCopy = new Button("复制到剪贴板");
        btnCopy.getStyleClass().add(Styles.ACCENT);
        btnCopy.setOnAction(e -> {
            String content = keyContentArea.getText();
            if (content != null && !content.isEmpty()) {
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(content);
                Clipboard.getSystemClipboard().setContent(clipboardContent);
                keyPathLabel.setText("已复制到剪贴板！");
            }
        });

        Button btnClose = new Button("关闭");
        btnClose.setOnAction(e -> modalPane.hide());

        HBox rightActions = new HBox(10, btnCopy, btnClose);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));
        rightPane.getChildren().addAll(keyPathLabel, keyContentArea, rightActions);

        SplitPane splitPane = new SplitPane(leftPane, rightPane);
        splitPane.setDividerPositions(0.35);

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(splitPane);
        modalBox.setMaxSize(700, 450);

        AnchorPane.setTopAnchor(splitPane, 0.0);
        AnchorPane.setBottomAnchor(splitPane, 0.0);
        AnchorPane.setLeftAnchor(splitPane, 0.0);
        AnchorPane.setRightAnchor(splitPane, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
    }

    private void showGenerateKeyDialog(ListView<File> keyList) {
        var modalPane = (ModalPane) fieldName.getScene().lookup("#main-modal-pane-alert");
        if (modalPane == null) return;

        ComboBox<String> keyTypeCombo = new ComboBox<>(
                FXCollections.observableArrayList("ed25519", "rsa", "ecdsa"));
        keyTypeCombo.setValue("ed25519");
        keyTypeCombo.setPrefWidth(150.0);

        TextField nameField = new TextField("id_ed25519");
        TextField emailField = new TextField("");
        emailField.setPromptText("your_email@example.com");

        // 切换密钥类型时自动更新文件名建议
        keyTypeCombo.valueProperty().addListener((obs, old, newType) -> {
            if (newType != null) {
                nameField.setText("id_" + newType);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("密钥类型:"), 0, 0);
        grid.add(keyTypeCombo, 1, 0);
        grid.add(new Label("文件名:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("邮箱/备注:"), 0, 2);
        grid.add(emailField, 1, 2);

        Button btnCancel = new Button("取消");
        btnCancel.setOnAction(e -> modalPane.hide());

        Button btnOk = new Button("确定");
        btnOk.getStyleClass().add(Styles.ACCENT);
        btnOk.setOnAction(e -> {
            try {
                gitService.generateSshKey(nameField.getText(), keyTypeCombo.getValue(), emailField.getText());
                keyList.getItems().setAll(gitService.listPublicKeys());
            } catch (Exception ex) {
                // ignore
            }
            modalPane.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnOk);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(10, new Label("生成 SSH 密钥"), grid, footer);
        content.setPadding(new Insets(10));

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(content);
        modalBox.setMaxSize(420, 250);

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
    }

    private void showRenameKeyDialog(ListView<File> keyList, File pubKeyFile) {
        var modalPane = (ModalPane) fieldName.getScene().lookup("#main-modal-pane-alert");
        if (modalPane == null) return;

        // 从文件名去掉 .pub 得到基础名
        String currentName = pubKeyFile.getName().replaceAll("\\.pub$", "");

        Label infoLabel = new Label("将同时重命名私钥和公钥:");
        TextField nameField = new TextField(currentName);

        Button btnCancel = new Button("取消");
        btnCancel.setOnAction(e -> modalPane.hide());

        Button btnOk = new Button("确定");
        btnOk.getStyleClass().add(Styles.ACCENT);
        btnOk.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty() || newName.equals(currentName)) {
                modalPane.hide();
                return;
            }
            try {
                gitService.renameSshKey(pubKeyFile, newName);
                keyList.getItems().setAll(gitService.listPublicKeys());
            } catch (Exception ex) {
                // ignore
            }
            modalPane.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnOk);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(10, infoLabel, nameField, footer);
        content.setPadding(new Insets(10));

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(content);
        modalBox.setMaxSize(360, 160);

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
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
        Platform.runLater(() -> scopeComboBox.getSelectionModel().selectFirst());
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

}