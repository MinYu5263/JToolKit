package com.minyu.jtoolkit.module.file_tree;

import atlantafx.base.controls.Message;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.core.component.PathTextField;
import com.minyu.jtoolkit.module.BaseController;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FileTreeController extends BaseController<FileTreePersistentState> {

    // === 数据源 ===
    private final ObservableList<FileTreePersistentState.HistoryItem> historyData = FXCollections.observableArrayList();

    // === 状态标志 ===
    // 用于防止在恢复历史配置时，UI控件的变化触发自动生成，导致配置被覆盖
    private boolean isRestoring = false;

    // === UI 组件 ===
    @FXML
    private PathTextField pathField;
    @FXML
    private Button historyBtn;
    @FXML
    private ComboBox<TreeStyle> styleCombo;
    @FXML
    private ToggleSwitch noLimitSwitch;
    @FXML
    private Spinner<Integer> depthSpinner;
    @FXML
    private ToggleSwitch dirsOnlySwitch;
    @FXML
    private TextField ignoreField;
    @FXML
    private Button editIgnoreBtn;
    @FXML
    private EnhancedTextArea resultArea;

    @FXML
    public void initView() {
        // 1. 路径变化触发生成
        pathField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                // 1. 尝试在历史记录中查找
                Optional<FileTreePersistentState.HistoryItem> match = historyData.stream()
                        .filter(h -> h.getPath().equals(newValue))
                        .findFirst();

                // 2. 锁定 UI 触发器，防止修改开关时重复触发生成
                isRestoring = true;
                try {
                    if (match.isPresent()) {
                        restoreItemConfig(match.get());
                    } else {
                        resetToDefaultConfig();
                    }
                } finally {
                    isRestoring = false; // 解锁
                }

                // 3. 执行生成
                onGenerate();
            }
        });

        // 2. 初始化样式选择
        styleCombo.setItems(FXCollections.observableArrayList(TreeStyle.values()));
        styleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(TreeStyle object) {
                return object == null ? "" : switch (object) {
                    case ASCII -> "ASCII (标准树形)";
                    case EMOJI -> "图标 (Emoji)";
                    case MARKDOWN -> "Markdown (无序列表)";
                };
            }

            @Override
            public TreeStyle fromString(String string) {
                return null;
            }
        });
        styleCombo.getSelectionModel().select(TreeStyle.ASCII);
        styleCombo.valueProperty().addListener((o, old, val) -> triggerUpdate());

        // 3. 初始化深度设置
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));

        // 联动逻辑：选中"无限制" -> 禁用数字框
        noLimitSwitch.selectedProperty().addListener((observable, oldValue, isNoLimit) -> {
            depthSpinner.setDisable(isNoLimit);
            triggerUpdate();
        });
        // 默认开启无限制
        noLimitSwitch.setSelected(true);

        // 只有在非无限制模式下，调整数字才触发更新
        depthSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!noLimitSwitch.isSelected()) triggerUpdate();
        });

        // 4. 按钮交互
        editIgnoreBtn.setOnAction(e -> openIgnoreEditor());
        historyBtn.setOnAction(e -> showHistoryDialog());

        dirsOnlySwitch.selectedProperty().addListener(o -> triggerUpdate());
        ignoreField.textProperty().addListener(o -> triggerUpdate());
    }

    /**
     * 统一触发更新的方法
     * 如果正在恢复历史配置(isRestoring=true)，则跳过，避免中间状态触发生成导致配置错乱
     */
    private void triggerUpdate() {
        if (isRestoring) return;
        if (pathField.getText() != null && !pathField.getText().isBlank()) {
            onGenerate();
        }
    }

    // ================== 核心交互 ==================

    @FXML
    public void onGenerate() {
        String path = pathField.getText();
        if (path == null || path.isBlank()) {
            return;
        }
        File rootDir = new File(path);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            resultArea.setText("路径不存在或不是文件夹");
            return;
        }

        // 1. 获取当前 UI 配置
        int maxDepth = noLimitSwitch.isSelected() ? Integer.MAX_VALUE : depthSpinner.getValue();
        boolean dirsOnly = dirsOnlySwitch.isSelected();
        String ignorePattern = ignoreField.getText();
        List<String> ignores = parseIgnores(ignorePattern);
        TreeStyle style = styleCombo.getValue();

        addToHistory(rootDir, style, maxDepth, dirsOnly, ignorePattern);

        resultArea.setText("正在扫描文件树...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                StringBuilder sb = new StringBuilder();
                if (style == TreeStyle.MARKDOWN) sb.append("- ");
                sb.append(rootDir.getName()).append("\n");
                generateRecursive(rootDir, "", 0, maxDepth, dirsOnly, ignores, style, sb);
                return sb.toString();
            }

            @Override
            protected void succeeded() {
                resultArea.setText(getValue());
                saveValues(); // 完成后触发状态持久化保存
            }

            @Override
            protected void failed() {
                resultArea.setText("生成失败: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private void addToHistory(File dir, TreeStyle style, int maxDepth, boolean dirsOnly, String ignorePattern) {
        String path = dir.getAbsolutePath();

        // 查找现有记录
        Optional<FileTreePersistentState.HistoryItem> existing = historyData.stream()
                .filter(h -> h.getPath().equals(path))
                .findFirst();

        if (existing.isPresent()) {
            FileTreePersistentState.HistoryItem item = existing.get();
            // 更新配置
            item.setStyle(style.name());
            item.setMaxDepth(maxDepth);
            item.setDirectoriesOnly(dirsOnly);
            item.setIgnorePattern(ignorePattern);
        } else {
            // 新增
            FileTreePersistentState.HistoryItem newItem = new FileTreePersistentState.HistoryItem(
                    path, dir.getName(), style.name(), maxDepth, dirsOnly, ignorePattern
            );
            historyData.addFirst(newItem);
            if (historyData.size() > 20) historyData.removeLast();
        }
    }

    private void showHistoryDialog() {
        var modalPane = (ModalPane) pathField.getScene().lookup("#main-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException(
                    "ModalPane not found in Scene. Make sure MainView.fxml has a ModalPane with id='main-modal-pane'"
            );
        }

        // === 功能点 2：每次打开时，将当前目录置顶 ===
        String currentPath = pathField.getText();
        if (currentPath != null && !currentPath.isBlank()) {
            Optional<FileTreePersistentState.HistoryItem> currentItem = historyData.stream()
                    .filter(h -> h.getPath().equals(currentPath))
                    .findFirst();

            if (currentItem.isPresent()) {
                FileTreePersistentState.HistoryItem item = currentItem.get();
                // 只有当它不在第一位时才移动，避免无意义刷新
                if (historyData.indexOf(item) != 0) {
                    historyData.remove(item);
                    historyData.addFirst(item);
                }
            }
        }

        ListView<FileTreePersistentState.HistoryItem> listView = new ListView<>();
        listView.setItems(historyData);
        HBox.setHgrow(listView, Priority.ALWAYS);
        VBox.setVgrow(listView, Priority.ALWAYS);

        // === CellFactory：处理单条删除 ===
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileTreePersistentState.HistoryItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    setOnMouseClicked(null);
                } else {
                    setText(item.getName());

                    // 右键菜单
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem deleteItem = new MenuItem("删除此记录");
                    deleteItem.setOnAction(event -> {
                        // === 功能点 3：删除当前选中的记录时，清空UI并恢复默认 ===
                        if (item.getPath().equals(pathField.getText())) {
                            resetUI();
                        }
                        historyData.remove(item);
                    });
                    contextMenu.getItems().add(deleteItem);
                    setContextMenu(contextMenu);

                    // 左键点击
                    setOnMouseClicked(event -> {
                        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                            pathField.setText(item.getPath());
                            // 不关闭弹窗
                        }
                    });
                }
            }
        });

        if (!historyData.isEmpty()) {
            listView.getSelectionModel().select(0);
            listView.scrollTo(0);
        }

        // === 功能点 1：底部清空全部按钮 ===
        Button clearAllBtn = new Button("清空所有历史");
        clearAllBtn.getStyleClass().add(Styles.DANGER); // 红色危险样式
        clearAllBtn.setMaxWidth(Double.MAX_VALUE);
        clearAllBtn.setOnAction(e -> {
            // modalPane.hide();
            showClearAllConfirmation(modalPane);
        });

        Label label = new Label("历史记录");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10));
        // 将按钮加到最底部
        vBox.getChildren().addAll(label, listView, clearAllBtn);

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(vBox);
        modalBox.setMaxWidth(300);
        modalBox.setOnClose(e -> modalPane.hide());

        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);

        modalPane.setAlignment(Pos.CENTER_RIGHT);
        modalPane.usePredefinedTransitionFactories(Side.RIGHT);
        modalPane.show(modalBox);
    }

    private void restoreItemConfig(FileTreePersistentState.HistoryItem item) {
        if (item == null) return;

        isRestoring = true; // === 锁定：阻止 triggerUpdate ===
        try {
            // 1. 恢复样式
            try {
                if (item.getStyle() != null) {
                    styleCombo.setValue(TreeStyle.valueOf(item.getStyle()));
                } else {
                    styleCombo.setValue(TreeStyle.ASCII);
                }
            } catch (Exception e) {
                styleCombo.setValue(TreeStyle.ASCII);
            }

            // 2. 恢复忽略规则
            ignoreField.setText(item.getIgnorePattern() != null ? item.getIgnorePattern() : "");

            // 3. 恢复只显示文件夹
            dirsOnlySwitch.setSelected(item.isDirectoriesOnly());

            // 4. 恢复深度 (兼容旧数据 maxDepth可能为0的情况)
            if (item.getMaxDepth() == Integer.MAX_VALUE || item.getMaxDepth() == 0) {
                noLimitSwitch.setSelected(true);
            } else {
                noLimitSwitch.setSelected(false);
                depthSpinner.getValueFactory().setValue(item.getMaxDepth());
            }
        } finally {
            isRestoring = false; // === 解锁 ===
        }
    }

    private void resetToDefaultConfig() {
        noLimitSwitch.setSelected(true);
        dirsOnlySwitch.setSelected(false);
        styleCombo.getSelectionModel().select(TreeStyle.ASCII);
        ignoreField.setText("");
    }

    private void openIgnoreEditor() {
        var modalPane = (ModalPane) pathField.getScene().lookup("#main-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException(
                    "ModalPane not found in Scene. Make sure MainView.fxml has a ModalPane with id='main-modal-pane'"
            );
        }

        Tile tile = new Tile("编辑忽略列表", "每行输入一个忽略规则 (支持正则)");
        TextArea textArea = new TextArea();
        VBox.setVgrow(textArea, Priority.ALWAYS);

        String currentText = ignoreField.getText();
        if (currentText != null) textArea.setText(currentText.replace(",", "\n"));

        Button btnCancel = new Button("取消");
        btnCancel.setOnAction(e -> modalPane.hide());

        Button btnOk = new Button("确定");
        btnOk.getStyleClass().add(Styles.ACCENT);
        btnOk.setOnAction(e -> {
            String newText = Arrays.stream(textArea.getText().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            ignoreField.setText(newText);
            modalPane.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnOk);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox vBox = new VBox(10, tile, textArea, footer);
        vBox.setPadding(new Insets(10));

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(vBox);
        modalBox.setMaxSize(500, 350);
        modalBox.setOnClose(e -> modalPane.hide());

        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
    }

    private List<String> parseIgnores(String text) {
        if (text == null) return Collections.emptyList();
        String cleanText = text.replace("[", "").replace("]", "");
        return Arrays.stream(cleanText.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void generateRecursive(File folder, String prefix, int currentDepth, int maxDepth,
                                   boolean dirsOnly, List<String> ignores, TreeStyle style, StringBuilder sb) {
        if (currentDepth >= maxDepth) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        List<File> fileList = Arrays.stream(files)
                .filter(f -> !isIgnored(f.getName(), ignores))
                .filter(f -> !dirsOnly || f.isDirectory())
                .sorted(Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName))
                .toList();

        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            boolean isLast = (i == fileList.size() - 1);

            String line;
            if (style == TreeStyle.ASCII || style == TreeStyle.EMOJI) {
                String connector = isLast ? "└── " : "├── ";
                String icon = (style == TreeStyle.EMOJI) ? (file.isDirectory() ? "📁 " : "📄 ") : "";
                line = prefix + connector + icon + file.getName();
            } else {
                line = prefix + "  - " + file.getName();
            }
            sb.append(line).append("\n");

            if (file.isDirectory()) {
                String nextPrefix = (style == TreeStyle.ASCII || style == TreeStyle.EMOJI)
                        ? prefix + (isLast ? "    " : "│   ")
                        : prefix + "  ";
                generateRecursive(file, nextPrefix, currentDepth + 1, maxDepth, dirsOnly, ignores, style, sb);
            }
        }
    }

    private boolean isIgnored(String name, List<String> ignores) {
        for (String ignore : ignores) {
            if (name.equals(ignore) || name.matches(ignore.replace(".", "\\.").replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 重置界面：清空路径、清空结果、恢复配置为默认
     */
    private void resetUI() {
        // 1. 暂停监听器 (虽然空路径通常不会触发生成，但为了安全起见)
        isRestoring = true;
        try {
            // 清空输入和输出
            pathField.setText("");
            resultArea.setText("");

            // 恢复默认配置
            noLimitSwitch.setSelected(true); // 默认无限制
            dirsOnlySwitch.setSelected(false);
            styleCombo.getSelectionModel().select(TreeStyle.ASCII);
            ignoreField.setText("");
        } finally {
            isRestoring = false;
        }
    }

    /**
     * 显示清空历史记录的二次确认弹窗
     */
    private void showClearAllConfirmation(ModalPane underlyingModal) {
         var modalPane = (ModalPane) pathField.getScene().lookup("#main-modal-pane-alert");
        if (modalPane == null) {
            throw new IllegalStateException(
                    "ModalPane not found in Scene. Make sure MainView.fxml has a ModalPane with id='main-modal-pane-alert'"
            );
        }

        var warning = new Message(
                "警告",
               "确定要清空所有历史记录吗？ 此操作执行后无法恢复",
                null
        );
        // warning.getStyleClass().add(Styles.WARNING);

        Button btnCancel = new Button("取消");
        Button btnConfirm = new Button("确认清空");

        btnConfirm.getStyleClass().add(Styles.DANGER);

        btnCancel.setOnAction(e -> {
            modalPane.hide();
            showHistoryDialog();
        });

        btnConfirm.setOnAction(e -> {
            historyData.clear();
            resetUI();
            modalPane.hide();
            underlyingModal.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnConfirm);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, warning, footer);
        root.setPadding(new Insets(10));

        ModalBox modalBox = new ModalBox(root);
        modalBox.setMaxSize(350, 150);
        modalBox.setOnClose(e -> modalPane.hide());

        AnchorPane.setTopAnchor(root, 0.0);
        AnchorPane.setBottomAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        AnchorPane.setRightAnchor(root, 0.0);

        modalPane.setAlignment(Pos.CENTER);
        modalPane.usePredefinedTransitionFactories(null);
        modalPane.show(modalBox);
        btnCancel.requestFocus();
    }

    // ================== BaseController 实现 ==================
    @Override
    protected String getViewKey() {
        return "file_tree_generator";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                historyData,
                ignoreField.textProperty(),
                noLimitSwitch.selectedProperty(),
                dirsOnlySwitch.selectedProperty(),
                styleCombo.valueProperty()
        );
    }

    @Override
    protected void restoreValues(FileTreePersistentState state) {
        if (state == null) return;

        if (state.getHistory() != null) {
            historyData.setAll(state.getHistory());
        }

        String lastPath = state.getLastPath();
        if (lastPath != null && !lastPath.isBlank()) {
            Optional<FileTreePersistentState.HistoryItem> match = historyData.stream()
                    .filter(h -> h.getPath().equals(lastPath))
                    .findFirst();

            if (match.isPresent()) {
                restoreItemConfig(match.get());
            } else {
                noLimitSwitch.setSelected(true);
                styleCombo.getSelectionModel().select(TreeStyle.ASCII);
            }

            pathField.setText(lastPath);
        }
    }

    @Override
    protected FileTreePersistentState captureValues() {
        FileTreePersistentState state = new FileTreePersistentState();
        state.setHistory(new ArrayList<>(historyData));
        state.setLastPath(pathField.getText());
        return state;
    }

    enum TreeStyle {ASCII, EMOJI, MARKDOWN}
}