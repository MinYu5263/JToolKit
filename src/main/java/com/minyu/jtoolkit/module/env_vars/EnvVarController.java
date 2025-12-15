package com.minyu.jtoolkit.module.env_vars;

import com.minyu.jtoolkit.module.BaseController;
import com.minyu.jtoolkit.system.service.ViewStateService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EnvVarController extends BaseController<EnvVarViewState> {

    // === UI 组件注入 ===
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TableView<Map.Entry<String, String>> systemTable;
    @FXML
    private TableColumn<Map.Entry<String, String>, String> sysKeyCol;
    @FXML
    private TableColumn<Map.Entry<String, String>, String> sysValueCol;

    @FXML
    private TableView<GeneratorItem> generatorTable;
    @FXML
    private TableColumn<GeneratorItem, String> genKeyCol;
    @FXML
    private TableColumn<GeneratorItem, String> genValueCol;
    @FXML
    private ChoiceBox<String> formatChoice;
    @FXML
    private TextArea previewArea;

    // === 数据源 ===
    private final ObservableList<Map.Entry<String, String>> systemEnvList = FXCollections.observableArrayList();
    private final ObservableList<GeneratorItem> generatorList = FXCollections.observableArrayList();

    public EnvVarController(ViewStateService viewStateService) {
        super(viewStateService);
    }

    @FXML
    public void initialize() {
        initSystemViewer();
        initGenerator();

        // 1. 恢复之前保存的草稿
        super.loadState();

        // 2. 注册自动保存
        // 监听 generatorList 的增删，以及 formatChoice 的选择
        // 注意：TableView 单元格编辑的监听在 initGenerator 里单独处理
        super.observeChanges(generatorList, formatChoice.valueProperty());
    }

    // ================== Tab 1: 系统变量查看器逻辑 ==================

    private void initSystemViewer() {
        typeCombo.setItems(FXCollections.observableArrayList("系统环境变量 (OS)", "Java 属性 (JVM)"));
        typeCombo.getSelectionModel().selectFirst();
        typeCombo.setOnAction(e -> onRefresh());

        sysKeyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        sysValueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));

        // 实现搜索过滤功能
        FilteredList<Map.Entry<String, String>> filteredData = new FilteredList<>(systemEnvList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(entry -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return entry.getKey().toLowerCase().contains(lower)
                        || entry.getValue().toLowerCase().contains(lower);
            });
        });
        systemTable.setItems(filteredData);

        onRefresh(); // 首次加载
    }

    @FXML
    public void onRefresh() {
        systemEnvList.clear();
        Map<String, String> dataMap;
        if (typeCombo.getSelectionModel().getSelectedIndex() == 0) {
            dataMap = System.getenv();
        } else {
            // JVM 属性转 Map
            dataMap = System.getProperties().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        }
        systemEnvList.addAll(dataMap.entrySet());
    }

    // ================== Tab 2: 脚本生成器逻辑 ==================

    private void initGenerator() {
        formatChoice.setItems(FXCollections.observableArrayList(
                "Windows CMD (set)",
                "Windows PowerShell ($env:)",
                "Linux/Mac Bash (export)",
                "Docker / .env",
                "YAML (Spring Boot)"
        ));
        formatChoice.getSelectionModel().selectFirst();
        formatChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, newV) -> generatePreview());

        generatorTable.setItems(generatorList);

        // 设置 Key 列可编辑
        genKeyCol.setCellValueFactory(cell -> cell.getValue().keyProperty());
        genKeyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        genKeyCol.setOnEditCommit(e -> {
            e.getRowValue().setKey(e.getNewValue());
            generatePreview();
            super.saveState(); // 编辑单元格后，手动触发保存
        });

        // 设置 Value 列可编辑
        genValueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
        genValueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        genValueCol.setOnEditCommit(e -> {
            e.getRowValue().setValue(e.getNewValue());
            generatePreview();
            super.saveState(); // 编辑单元格后，手动触发保存
        });
    }

    private void generatePreview() {
        String format = formatChoice.getValue();
        if (format == null) return;

        StringBuilder sb = new StringBuilder();
        for (GeneratorItem item : generatorList) {
            String k = item.getKey();
            String v = item.getValue();
            if (k == null || k.isBlank()) continue;
            if (v == null) v = "";

            switch (format) {
                case "Windows CMD (set)" -> sb.append("set ").append(k).append("=").append(v).append("\n");
                case "Windows PowerShell ($env:)" ->
                        sb.append("$env:").append(k).append("=\"").append(v).append("\"\n");
                case "Linux/Mac Bash (export)" -> sb.append("export ").append(k).append("=\"").append(v).append("\"\n");
                case "Docker / .env" -> sb.append(k).append("=").append(v).append("\n");
                case "YAML (Spring Boot)" -> sb.append(k).append(": ").append(v).append("\n");
            }
        }
        previewArea.setText(sb.toString());
    }

    @FXML
    public void onAddRow() {
        generatorList.add(new GeneratorItem("KEY", "Value"));
        generatePreview();
    }

    @FXML
    public void onDeleteRow() {
        GeneratorItem selected = generatorTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            generatorList.remove(selected);
            generatePreview();
        }
    }

    @FXML
    public void onClearRows() {
        generatorList.clear();
        generatePreview();
    }

    @FXML
    public void onCopyResult() {
        ClipboardContent content = new ClipboardContent();
        content.putString(previewArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @Override
    protected String getStorageKey() {
        return "env_vars";
    }

    @Override
    protected Class<EnvVarViewState> getStateType() {
        return EnvVarViewState.class;
    }

    @Override
    protected void restoreUI(EnvVarViewState state) {
        if (state == null) return;

        if (state.getLastSelectedFormat() != null) {
            formatChoice.getSelectionModel().select(state.getLastSelectedFormat());
        }

        if (state.getItems() != null) {
            generatorList.clear();
            for (EnvVarViewState.EnvVarItemState item : state.getItems()) {
                generatorList.add(new GeneratorItem(item.getKey(), item.getValue()));
            }
            generatePreview();
        }
    }

    @Override
    protected EnvVarViewState captureUI() {
        EnvVarViewState state = new EnvVarViewState();
        state.setLastSelectedFormat(formatChoice.getValue());

        List<EnvVarViewState.EnvVarItemState> items = generatorList.stream()
                .map(i -> new EnvVarViewState.EnvVarItemState(i.getKey(), i.getValue()))
                .collect(Collectors.toList());
        state.setItems(items);

        return state;
    }

    // UI 模型：使用 Property 支持表格编辑双向绑定
    public static class GeneratorItem {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;

        public GeneratorItem(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }

        public String getKey() {
            return key.get();
        }

        public void setKey(String k) {
            this.key.set(k);
        }

        public SimpleStringProperty keyProperty() {
            return key;
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String v) {
            this.value.set(v);
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }
    }
}