package com.minyu.jtoolkit.core.component;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * PathTextField - 路径选择组件
 * 基于 AtlantaFX CustomTextField 封装，支持文件/文件夹选择及拖拽
 */
public class PathTextField extends CustomTextField {

    private final ObjectProperty<PickerMode> pickerMode = new SimpleObjectProperty<>(this, "pickerMode", PickerMode.FILE);
    private final FileChooser fileChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private final Button actionButton = new Button();

    public PathTextField() {
        super();
        this.getStyleClass().add("path-text-field");

        initializeUI();
        setupDragAndDrop();
    }

    private void initializeUI() {
        var icon = new FontIcon(Feather.FOLDER);
        actionButton.setGraphic(icon);

        actionButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

        actionButton.setCursor(Cursor.HAND);
        actionButton.setTooltip(new Tooltip("浏览路径"));
        actionButton.setFocusTraversable(false);
        actionButton.setOnAction(e -> handleBrowse());

        this.setRight(actionButton);
    }

    private void handleBrowse() {
        File initialDir = resolveInitialDirectory();
        File result = null;
        var window = this.getScene().getWindow();

        // 根据当前的 pickerMode 属性决定行为
        switch (getPickerMode()) {
            case FILE -> {
                fileChooser.setTitle("选择文件");
                if (initialDir != null) fileChooser.setInitialDirectory(initialDir);
                result = fileChooser.showOpenDialog(window);
            }
            case DIRECTORY -> {
                directoryChooser.setTitle("选择文件夹");
                if (initialDir != null) directoryChooser.setInitialDirectory(initialDir);
                result = directoryChooser.showDialog(window);
            }
        }

        if (result != null) {
            this.setText(result.getAbsolutePath());
        }

        // 点击按钮获取焦点
        this.requestFocus();
    }

    /**
     * 智能解析当前路径的父级目录
     */
    private File resolveInitialDirectory() {
        String currentPath = this.getText();
        if (currentPath == null || currentPath.isBlank()) {
            return null;
        }
        try {
            File f = new File(currentPath);
            if (f.isDirectory()) return f;
            if (f.isFile()) return f.getParentFile();

            File parent = f.getParentFile();
            if (parent != null && parent.exists()) return parent;
        } catch (Exception ignored) {
        }
        return null;
    }

    private void setupDragAndDrop() {
        this.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (!files.isEmpty()) {
                    File file = files.getFirst();
                    // 如果是文件夹模式但拖入的是文件，自动取父路径
                    if (getPickerMode() == PickerMode.DIRECTORY && file.isFile()) {
                        this.setText(file.getParent());
                    } else {
                        this.setText(file.getAbsolutePath());
                    }
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    public final ObjectProperty<PickerMode> pickerModeProperty() {
        return pickerMode;
    }

    public final PickerMode getPickerMode() {
        return pickerMode.get();
    }

    public final void setPickerMode(PickerMode pickerMode) {
        this.pickerMode.set(pickerMode);
    }

    public final Path getPath() {
        String t = getText();
        return (t == null || t.isBlank()) ? null : Path.of(t);
    }

    public final File getFile() {
        Path p = getPath();
        return p == null ? null : p.toFile();
    }

    public final FileChooser getFileChooser() {
        return fileChooser;
    }

    public final DirectoryChooser getDirectoryChooser() {
        return directoryChooser;
    }

    public enum PickerMode {
        FILE,           // 选择文件
        DIRECTORY       // 选择文件夹
    }
}