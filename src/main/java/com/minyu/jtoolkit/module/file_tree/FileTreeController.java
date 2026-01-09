package com.minyu.jtoolkit.module.file_tree;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.core.component.ConfigGroup;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.core.component.PathTextField;
import com.minyu.jtoolkit.module.BaseController;
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
    // === UI 组件 (对应新的 FXML) ===
    @FXML
    private PathTextField pathField;
    @FXML
    private Button historyBtn; // 历史记录按钮
    // 配置区域
    @FXML
    private ComboBox<TreeStyle> styleCombo;
    @FXML
    private ConfigGroup depthGroup;
    @FXML
    private ToggleSwitch noLimitSwitch; // 无限制开关
    @FXML
    private Spinner<Integer> depthSpinner;
    @FXML
    private ToggleSwitch dirsOnlySwitch; // 只显示文件夹开关
    // 忽略配置
    @FXML
    private TextField ignoreField;
    @FXML
    private Button editIgnoreBtn; // 打开列表按钮
    // 输出区域
    @FXML
    private EnhancedTextArea resultArea;

    @FXML
    public void initView() {
        pathField.textProperty().addListener((observable, oldValue, newValue) -> {
            onGenerate();
        });

        // 2. 初始化样式选择
        styleCombo.setItems(FXCollections.observableArrayList(TreeStyle.values()));
        styleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(TreeStyle object) {
                return switch (object) {
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
        // 样式改变时自动重新生成(如果有路径的话)
        styleCombo.valueProperty().addListener((o, old, val) -> {
            if (pathField.getText() != null && !pathField.getText().isEmpty()) onGenerate();
        });

        // 3. 初始化深度设置
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));

        noLimitSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            depthSpinner.setDisable(!noLimitSwitch.isSelected());
            depthGroup.setExpanded(noLimitSwitch.isSelected());
        });
        // 也可以选择在无限制开启时折叠 ConfigGroup 的内部 (取决于 ConfigGroup 实现)
        // noLimitSwitch.selectedProperty().addListener((obs, old, val) -> depthGroup.setExpanded(!val));

        // 4. 忽略列表按钮交互
        editIgnoreBtn.setOnAction(e -> openIgnoreEditor());

        // 5. 历史记录按钮交互
        historyBtn.setOnAction(e -> showHistoryDialog());

        // 6. 注册自动保存
        super.observeChanges(historyData, ignoreField.textProperty(),
                noLimitSwitch.selectedProperty(), dirsOnlySwitch.selectedProperty());
    }

    // ================== 核心交互 ==================

    /**
     * 生成逻辑 (通常由 PathTextField 的回车或外部按钮触发，或者你可以加一个"生成"按钮)
     * 这里假设路径变化或点击某个按钮调用此方法
     */
    @FXML
    public void onGenerate() {
        String path = pathField.getText(); // 假设 PathTextField 有 getText()
        if (path == null || path.isBlank()) {
            resultArea.setText("请先选择文件夹路径");
            return;
        }
        File rootDir = new File(path);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            resultArea.setText("路径不存在或不是文件夹");
            return;
        }

        // 1. 添加到历史记录
        addToHistory(rootDir);

        // 2. 获取配置参数
        int maxDepth = noLimitSwitch.isSelected() ? Integer.MAX_VALUE : depthSpinner.getValue();
        boolean dirsOnly = dirsOnlySwitch.isSelected();
        List<String> ignores = parseIgnores(ignoreField.getText());
        TreeStyle style = styleCombo.getValue();

        // 3. 异步执行
        resultArea.setText("正在扫描文件树..."); // 假设 EnhancedTextArea 有 setText

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                StringBuilder sb = new StringBuilder();
                // 写入根节点
                if (style == TreeStyle.MARKDOWN) sb.append("- ");
                sb.append(rootDir.getName()).append("\n");

                // 递归生成
                generateRecursive(rootDir, "", 0, maxDepth, dirsOnly, ignores, style, sb);
                return sb.toString();
            }

            @Override
            protected void succeeded() {
                resultArea.setText(getValue());
                saveValues();
            }

            @Override
            protected void failed() {
                resultArea.setText("生成失败: " + getException().getMessage());
                getException().printStackTrace();
            }
        };

        new Thread(task).start();
    }


    private void showHistoryDialog() {
        var modalPane = (ModalPane) pathField.getScene().lookup("#content-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException(
                    "ModalPane not found. Check FXML id='content-modal-pane'"
            );
        }

        ListView<FileTreePersistentState.HistoryItem> listView = new ListView<>();
        HBox.setHgrow(listView, Priority.ALWAYS);
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            pathField.setText(newValue.getPath());
        });

        Label label = new Label("历史记录");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10));
        vBox.getChildren().addAll(label, listView);

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(vBox);
        modalBox.setMaxWidth(270);
        modalBox.setOnClose(e -> modalPane.hide());

        // 让子节点撑满整个容器
        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);

        modalPane.setAlignment(Pos.CENTER_RIGHT);
        modalPane.usePredefinedTransitionFactories(Side.RIGHT);
        modalPane.show(modalBox);
    }

    private void openIgnoreEditor() {
        // var modalPane = (ModalPane) pathField.getScene().lookup("#content-modal-pane");
        var modalPane = (ModalPane) pathField.getScene().lookup("#main-modal-pane");
        if (modalPane == null) {
            throw new IllegalStateException(
                    "ModalPane not found. Check FXML id='content-modal-pane'"
            );
        }

        Tile tile = new Tile();
        tile.setTitle("编辑忽略列表");
        tile.setDescription("每行输入一个忽略规则 (支持正则)");

        TextArea textArea = new TextArea();
        VBox.setVgrow(textArea, Priority.ALWAYS);
        String currentText = ignoreField.getText();
        if (currentText != null && !currentText.isBlank()) {
            textArea.setText(currentText.replace(",", "\n"));
        }

        Button btnCancel = new Button("取消");
        btnCancel.setOnAction(e -> modalPane.hide());

        Button btnOk = new Button("确定");
        btnOk.getStyleClass().add(Styles.ACCENT); // 强调色
        btnOk.setOnAction(e -> {
            // [逻辑] 保存数据：将换行转回逗号分隔
            String newText = Arrays.stream(textArea.getText().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            ignoreField.setText(newText);
            modalPane.hide();
        });

        HBox footer = new HBox(10, btnCancel, btnOk);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10));
        vBox.getChildren().addAll(tile, textArea, footer);

        ModalBox modalBox = new ModalBox();
        modalBox.addContent(vBox);
        modalBox.setMaxSize(500, 400);
        modalBox.setOnClose(e -> modalPane.hide());

        // 让子节点撑满整个容器
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
        // 处理方括号 [] 包裹的情况，也兼容直接逗号分隔
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
                String icon = "";
                if (style == TreeStyle.EMOJI) icon = file.isDirectory() ? "📁 " : "📄 ";
                line = prefix + connector + icon + file.getName();
            } else {
                line = prefix + "  - " + file.getName();
            }

            sb.append(line).append("\n");

            if (file.isDirectory()) {
                String nextPrefix = "";
                if (style == TreeStyle.ASCII || style == TreeStyle.EMOJI) {
                    nextPrefix = prefix + (isLast ? "    " : "│   ");
                } else {
                    nextPrefix = prefix + "  ";
                }
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

    private void addToHistory(File dir) {
        String path = dir.getAbsolutePath();
        boolean exists = historyData.stream().anyMatch(h -> h.getPath().equals(path));
        if (!exists) {
            historyData.add(0, new FileTreePersistentState.HistoryItem(path, dir.getName()));
            if (historyData.size() > 20) historyData.remove(historyData.size() - 1);
        }
    }

    @Override
    protected String getViewKey() {
        return "file_tree_generator";
    }

    // ================== BaseController 实现 ==================

    @Override
    protected void restoreValues(FileTreePersistentState state) {
        if (state == null) return;

        if (state.getHistory() != null) historyData.setAll(state.getHistory());
        if (state.getLastPath() != null) pathField.setText(state.getLastPath()); // 假设 PathTextField 有 setText
        if (state.getIgnorePattern() != null) ignoreField.setText(state.getIgnorePattern());

        dirsOnlySwitch.setSelected(state.isDirectoriesOnly());

        // 恢复深度
        if (state.getMaxDepth() == Integer.MAX_VALUE) {
            noLimitSwitch.setSelected(true);
        } else {
            noLimitSwitch.setSelected(false);
            depthSpinner.getValueFactory().setValue(state.getMaxDepth());
        }

        // 恢复样式
        try {
            if (state.getStyle() != null) {
                styleCombo.setValue(TreeStyle.valueOf(state.getStyle()));
            }
        } catch (IllegalArgumentException e) {
            styleCombo.setValue(TreeStyle.ASCII);
        }
    }

    @Override
    protected FileTreePersistentState captureValues() {
        FileTreePersistentState state = new FileTreePersistentState();
        state.setHistory(new ArrayList<>(historyData));
        state.setLastPath(pathField.getText());
        state.setIgnorePattern(ignoreField.getText());
        state.setDirectoriesOnly(dirsOnlySwitch.isSelected());
        state.setMaxDepth(noLimitSwitch.isSelected() ? Integer.MAX_VALUE : depthSpinner.getValue());
        state.setStyle(styleCombo.getValue().name());
        return state;
    }

    enum TreeStyle {ASCII, EMOJI, MARKDOWN}
}