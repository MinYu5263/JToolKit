package com.minyu.jtoolkit.module.env_vars;

import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.core.component.ModalDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 路径变量编辑器对话框
 * 用于处理类似 PATH 这种包含多个值的变量
 */
public class PathEditorDialog extends ModalDialog {

    private final ObservableList<String> pathList;
    private final ListView<String> listView;

    public PathEditorDialog(String variableName, String initialValue, Consumer<String> saveHandler) {
        header.setTitle("编辑环境变量: " + variableName);
        header.setDescription("编辑文本列表，每行代表一个路径。");

        // 1. 解析初始值 (按系统分隔符拆分)
        String separator = File.pathSeparator; // Windows是分号，Mac/Linux是冒号
        List<String> splitData = new ArrayList<>();
        if (initialValue != null && !initialValue.isEmpty()) {
            splitData = List.of(initialValue.split(separator));
        }
        pathList = FXCollections.observableArrayList(splitData);

        // 2. 构建 UI
        listView = new ListView<>(pathList);
        listView.setEditable(true);
        // 允许双击编辑
        listView.setCellFactory(param -> new TextFieldListCell());
        HBox.setHgrow(listView, Priority.ALWAYS);

        // 右侧按钮栏
        VBox rightBar = new VBox(10);
        rightBar.setPadding(new Insets(0, 0, 0, 10));

        Button btnAdd = new Button("新建");
        Button btnBrowse = new Button("浏览");
        Button btnEdit = new Button("编辑");
        Button btnDelete = new Button("删除");
        Separator sep = new Separator();
        Button btnUp = new Button("上移");
        Button btnDown = new Button("下移");

        // 统一样式
        for (Button btn : List.of(btnAdd, btnBrowse, btnEdit, btnDelete, btnUp, btnDown)) {
            btn.setMaxWidth(Double.MAX_VALUE); // 按钮撑满宽度
        }

        rightBar.getChildren().addAll(btnAdd, btnBrowse, btnEdit, btnDelete, sep, btnUp, btnDown);

        HBox listContent = new HBox(listView, rightBar);
        listContent.setPadding(new Insets(10));
        listContent.setPrefSize(600, 400);

        Button btnCancel = new Button("取消");
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> close());

        Button btnOk = new Button("确定");
        btnOk.getStyleClass().add(Styles.ACCENT);
        btnOk.setDefaultButton(true);
        btnOk.setOnAction(e -> {
            saveHandler.accept(pathList.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(separator)));
            close();
        });

        HBox footer = new HBox(10, btnCancel, btnOk);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 10, 10, 10));

        VBox body = new VBox(listContent, footer);
        VBox.setVgrow(listContent, Priority.ALWAYS);
        content.setBody(body);
        content.setPrefSize(680, 480);

        // 3. 事件绑定

        // 新建
        btnAdd.setOnAction(e -> {
            pathList.add("在此处输入新路径");
            listView.getSelectionModel().selectLast();
            listView.edit(pathList.size() - 1);
        });

        // 浏览文件夹
        btnBrowse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("选择路径");
            File file = dc.showDialog(listView.getScene().getWindow());
            if (file != null) {
                pathList.add(file.getAbsolutePath());
                listView.getSelectionModel().selectLast();
            }
        });

        // 编辑选中
        btnEdit.setOnAction(e -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index >= 0) listView.edit(index);
        });

        // 删除选中
        btnDelete.setOnAction(e -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index >= 0) pathList.remove(index);
        });

        // 上移
        btnUp.setOnAction(e -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index > 0) {
                Collections.swap(pathList, index, index - 1);
                listView.getSelectionModel().select(index - 1);
            }
        });

        // 下移
        btnDown.setOnAction(e -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < pathList.size() - 1) {
                Collections.swap(pathList, index, index + 1);
                listView.getSelectionModel().select(index + 1);
            }
        });
    }

    /**
     * 内部类：支持直接编辑的 ListCell
     */
    private static class TextFieldListCell extends ListCell<String> {
        private TextField textField;

        @Override
        public void startEdit() {
            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnAction(evt -> commitEdit(textField.getText()));
            // 失去焦点时提交
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) commitEdit(textField.getText());
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }
}
