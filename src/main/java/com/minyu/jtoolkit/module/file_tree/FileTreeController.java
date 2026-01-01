package com.minyu.jtoolkit.module.file_tree;

import com.minyu.jtoolkit.module.BaseController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.DirectoryChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileTreeController extends BaseController<FileTreePersistentState> {

    // === UI 组件 ===
    @FXML private ListView<FileTreePersistentState.HistoryItem> historyListView;
    @FXML private TextField pathField;
    @FXML private TextArea resultArea;
    @FXML private Label statusLabel;

    // 配置组件
    @FXML private ToggleGroup styleGroup;
    @FXML private RadioButton rbAscii, rbEmoji, rbMarkdown;
    @FXML private CheckBox chkNoLimit;
    @FXML private Spinner<Integer> depthSpinner;
    @FXML private CheckBox chkDirsOnly;
    @FXML private TextField ignoreField;

    // 数据源
    private final ObservableList<FileTreePersistentState.HistoryItem> historyData = FXCollections.observableArrayList();

    @FXML
    public void initView() {
        // 1. 初始化控件
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));
        depthSpinner.disableProperty().bind(chkNoLimit.selectedProperty());

        historyListView.setItems(historyData);
        historyListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                pathField.setText(newVal.getPath());
            }
        });

        // 2. 恢复状态


        // 3. 注册自动保存
        super.observeChanges(historyData, ignoreField.textProperty(), chkNoLimit.selectedProperty());
    }

    // ================== 核心交互 ==================

    @FXML
    public void onBrowse() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("选择目标文件夹");
        File dir = dc.showDialog(pathField.getScene().getWindow());
        if (dir != null) {
            pathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    public void onGenerate() {
        String path = pathField.getText();
        if (path == null || path.isBlank()) {
            statusLabel.setText("请先选择文件夹路径");
            return;
        }
        File rootDir = new File(path);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            statusLabel.setText("路径不存在或不是文件夹");
            return;
        }

        // 1. 添加到历史记录 (如果不存在)
        addToHistory(rootDir);

        // 2. 获取配置参数
        int maxDepth = chkNoLimit.isSelected() ? Integer.MAX_VALUE : depthSpinner.getValue();
        boolean dirsOnly = chkDirsOnly.isSelected();
        List<String> ignores = Arrays.stream(ignoreField.getText().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        TreeStyle style = getSelectedStyle();

        // 3. 异步执行生成任务
        statusLabel.setText("生成中...");
        resultArea.setText("正在扫描文件树，请稍候...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                StringBuilder sb = new StringBuilder();
                // 写入根节点
                sb.append(style == TreeStyle.MARKDOWN ? "- " : "").append(rootDir.getName()).append("\n");
                // 递归生成
                generateRecursive(rootDir, "", 0, maxDepth, dirsOnly, ignores, style, sb);
                return sb.toString();
            }

            @Override
            protected void succeeded() {
                resultArea.setText(getValue());
                statusLabel.setText("生成完成");
                saveValues(); // 成功后保存一次历史记录
            }

            @Override
            protected void failed() {
                resultArea.setText("生成失败: " + getException().getMessage());
                statusLabel.setText("发生错误");
                getException().printStackTrace();
            }
        };

        new Thread(task).start();
    }

    // ================== 递归核心算法 ==================

    private void generateRecursive(File folder, String prefix, int currentDepth, int maxDepth,
                                   boolean dirsOnly, List<String> ignores, TreeStyle style, StringBuilder sb) {
        if (currentDepth >= maxDepth) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        // 过滤和排序
        List<File> fileList = Arrays.stream(files)
                .filter(f -> !isIgnored(f.getName(), ignores))
                .filter(f -> !dirsOnly || f.isDirectory())
                .sorted(Comparator.comparing(File::isDirectory).reversed() // 文件夹排前面
                        .thenComparing(File::getName))
                .collect(Collectors.toList());

        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            boolean isLast = (i == fileList.size() - 1);

            // 构建当前行的前缀
            String line;
            if (style == TreeStyle.ASCII || style == TreeStyle.EMOJI) {
                String connector = isLast ? "└── " : "├── ";
                String icon = "";
                if (style == TreeStyle.EMOJI) {
                    icon = file.isDirectory() ? "📁 " : "📄 ";
                }
                line = prefix + connector + icon + file.getName();
            } else {
                // Markdown 风格
                line = prefix + "  - " + file.getName();
            }

            sb.append(line).append("\n");

            // 递归处理子文件夹
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
            // 简单包含匹配，也可以升级为正则或glob
            if (name.equals(ignore) || name.matches(ignore.replace(".", "\\.").replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }

    // ================== 历史记录管理 ==================

    private void addToHistory(File dir) {
        String path = dir.getAbsolutePath();
        // 检查是否已存在
        boolean exists = historyData.stream().anyMatch(h -> h.getPath().equals(path));
        if (!exists) {
            historyData.add(0, new FileTreePersistentState.HistoryItem(path, dir.getName()));
            if (historyData.size() > 20) historyData.remove(historyData.size() - 1); // 限制最近20条
        }
    }

    @FXML
    public void onRenameHistory() {
        var item = historyListView.getSelectionModel().getSelectedItem();
        if (item == null) return;

        TextInputDialog dialog = new TextInputDialog(item.getName());
        dialog.setTitle("重命名");
        dialog.setHeaderText("为路径设置一个别名:");
        dialog.setContentText("名称:");
        dialog.showAndWait().ifPresent(name -> {
            item.setName(name);
            historyListView.refresh(); // 刷新 UI 显示
            saveValues();
        });
    }

    @FXML
    public void onDeleteHistory() {
        var item = historyListView.getSelectionModel().getSelectedItem();
        if (item != null) {
            historyData.remove(item);
        }
    }

    @FXML
    public void onClearHistory() {
        historyData.clear();
    }

    @FXML
    public void onCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(resultArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("已复制到剪贴板");
    }

    // ================== 辅助方法 ==================

    private TreeStyle getSelectedStyle() {
        if (rbEmoji.isSelected()) return TreeStyle.EMOJI;
        if (rbMarkdown.isSelected()) return TreeStyle.MARKDOWN;
        return TreeStyle.ASCII;
    }

    enum TreeStyle { ASCII, EMOJI, MARKDOWN }

    // ================== BaseController 实现 ==================

    @Override
    protected String getViewKey() {
        return "tool.file_tree.generator";
    }

    @Override
    protected Class<FileTreePersistentState> getStorageType() {
        return FileTreePersistentState.class;
    }

    @Override
    protected void restoreValues(FileTreePersistentState state) {
        if (state == null) return;

        if (state.getHistory() != null) {
            historyData.setAll(state.getHistory());
        }
        if (state.getLastPath() != null) {
            pathField.setText(state.getLastPath());
        }
        if (state.getIgnorePattern() != null) {
            ignoreField.setText(state.getIgnorePattern());
        }
        chkDirsOnly.setSelected(state.isDirectoriesOnly());

        // 恢复深度设置
        if (state.getMaxDepth() == Integer.MAX_VALUE) {
            chkNoLimit.setSelected(true);
        } else {
            chkNoLimit.setSelected(false);
            depthSpinner.getValueFactory().setValue(state.getMaxDepth());
        }

        // 恢复样式
        if ("EMOJI".equals(state.getStyle())) rbEmoji.setSelected(true);
        else if ("MARKDOWN".equals(state.getStyle())) rbMarkdown.setSelected(true);
        else rbAscii.setSelected(true);
    }

    @Override
    protected FileTreePersistentState captureValues() {
        FileTreePersistentState state = new FileTreePersistentState();
        state.setHistory(new ArrayList<>(historyData));
        state.setLastPath(pathField.getText());
        state.setIgnorePattern(ignoreField.getText());
        state.setDirectoriesOnly(chkDirsOnly.isSelected());
        state.setMaxDepth(chkNoLimit.isSelected() ? Integer.MAX_VALUE : depthSpinner.getValue());
        state.setStyle(getSelectedStyle().name());
        return state;
    }
}