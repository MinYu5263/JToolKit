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

    private final ObservableList<FileTreePersistentState.HistoryItem> historyData = FXCollections.observableArrayList();

    private boolean isRestoring = false;

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
        pathField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                Optional<FileTreePersistentState.HistoryItem> match = historyData.stream()
                        .filter(h -> h.getPath().equals(newValue))
                        .findFirst();

                isRestoring = true;
                try {
                    if (match.isPresent()) {
                        restoreItemConfig(match.get());
                    } else {
                        resetToDefaultConfig();
                    }
                } finally {
                    isRestoring = false;
                }

                onGenerate();
            }
        });

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

        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));

        noLimitSwitch.selectedProperty().addListener((observable, oldValue, isNoLimit) -> {
            depthSpinner.setDisable(isNoLimit);
            triggerUpdate();
        });
        noLimitSwitch.setSelected(true);

        depthSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!noLimitSwitch.isSelected()) triggerUpdate();
        });

        editIgnoreBtn.setOnAction(e -> openIgnoreEditor());
        historyBtn.setOnAction(e -> showHistoryDialog());

        dirsOnlySwitch.selectedProperty().addListener(o -> triggerUpdate());
        ignoreField.textProperty().addListener(o -> triggerUpdate());
    }

    private void triggerUpdate() {
        if (isRestoring) return;
        if (pathField.getText() != null && !pathField.getText().isBlank()) {
            onGenerate();
        }
    }

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
                saveValues();
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

        Optional<FileTreePersistentState.HistoryItem> existing = historyData.stream()
                .filter(h -> h.getPath().equals(path))
                .findFirst();

        if (existing.isPresent()) {
            FileTreePersistentState.HistoryItem item = existing.get();
            item.setStyle(style.name());
            item.setMaxDepth(maxDepth);
            item.setDirectoriesOnly(dirsOnly);
            item.setIgnorePattern(ignorePattern);
        } else {
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

        String currentPath = pathField.getText();
        if (currentPath != null && !currentPath.isBlank()) {
            Optional<FileTreePersistentState.HistoryItem> currentItem = historyData.stream()
                    .filter(h -> h.getPath().equals(currentPath))
                    .findFirst();

            if (currentItem.isPresent()) {
                FileTreePersistentState.HistoryItem item = currentItem.get();
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

                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem deleteItem = new MenuItem("删除此记录");
                    deleteItem.setOnAction(event -> {
                        if (item.getPath().equals(pathField.getText())) {
                            resetUI();
                        }
                        historyData.remove(item);
                    });
                    contextMenu.getItems().add(deleteItem);
                    setContextMenu(contextMenu);

                    setOnMouseClicked(event -> {
                        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                            pathField.setText(item.getPath());
                        }
                    });
                }
            }
        });

        if (!historyData.isEmpty()) {
            listView.getSelectionModel().select(0);
            listView.scrollTo(0);
        }

        Button clearAllBtn = new Button("清空所有历史");
        clearAllBtn.getStyleClass().add(Styles.DANGER);
        clearAllBtn.setMaxWidth(Double.MAX_VALUE);
        clearAllBtn.setOnAction(e -> {
            showClearAllConfirmation(modalPane);
        });

        Label label = new Label("历史记录");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10));
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

        isRestoring = true;
        try {
            try {
                if (item.getStyle() != null) {
                    styleCombo.setValue(TreeStyle.valueOf(item.getStyle()));
                } else {
                    styleCombo.setValue(TreeStyle.ASCII);
                }
            } catch (Exception e) {
                styleCombo.setValue(TreeStyle.ASCII);
            }

            ignoreField.setText(item.getIgnorePattern() != null ? item.getIgnorePattern() : "");

            dirsOnlySwitch.setSelected(item.isDirectoriesOnly());

            if (item.getMaxDepth() == Integer.MAX_VALUE || item.getMaxDepth() == 0) {
                noLimitSwitch.setSelected(true);
            } else {
                noLimitSwitch.setSelected(false);
                depthSpinner.getValueFactory().setValue(item.getMaxDepth());
            }
        } finally {
            isRestoring = false;
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

    private void resetUI() {
        isRestoring = true;
        try {
            pathField.setText("");
            resultArea.setText("");

            noLimitSwitch.setSelected(true);
            dirsOnlySwitch.setSelected(false);
            styleCombo.getSelectionModel().select(TreeStyle.ASCII);
            ignoreField.setText("");
        } finally {
            isRestoring = false;
        }
    }

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
