package com.minyu.jtoolkit.core.component;

import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.Getter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * EnhancedInputBase
 * 增强型输入组件的抽象基类
 * T: 具体的控件类型，必须继承自 TextInputControl (如 TextArea, TextField)
 */
public abstract class EnhancedInputBase<T extends TextInputControl> extends VBox {

    // 扩展方法
    // 泛型核心控件
    @Getter
    protected final T inputControl;
    private final Label titleLabel;
    // 属性定义
    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(this, "mode", Mode.ALL);
    // 按钮显示控制 (Internal)
    private final BooleanProperty showCopy = new SimpleBooleanProperty(true);
    private final BooleanProperty showPaste = new SimpleBooleanProperty(true);
    private final BooleanProperty showClear = new SimpleBooleanProperty(true);
    private final BooleanProperty showImport = new SimpleBooleanProperty(true);
    private final BooleanProperty showExport = new SimpleBooleanProperty(true);
    // 按钮组件
    private Button copyButton;
    private Button pasteButton;
    private Button clearButton;
    private Button importButton;
    private Button exportButton;
    private Separator separator;
    public EnhancedInputBase() {
        this.inputControl = createInputControl();
        this.titleLabel = new Label();

        initBaseView();
        setupBaseListeners();
        setupBaseBindings();

        applyMode(Mode.ALL);
    }

    /**
     * 抽象方法：子类必须实现此方法来提供具体的输入控件
     */
    protected abstract T createInputControl();

    private void initBaseView() {
        this.setSpacing(5);
        this.setPadding(new Insets(5));
        this.getStyleClass().add("enhanced-input-base");

        pasteButton = createButton("粘贴", Feather.CLIPBOARD, this::pasteText);
        importButton = createButton("导入", Feather.FILE, this::importFile);
        clearButton = createButton("清空", Feather.X, this::clear);
        exportButton = createButton("导出", Feather.SAVE, this::exportFile);
        copyButton = createButton("复制", Material2OutlinedAL.CONTENT_COPY, this::copyText);
        separator = new Separator(Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 5, 0, 5));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox inputButtons = new HBox(2);
        inputButtons.getChildren().addAll(pasteButton, importButton, clearButton);

        HBox outputButtons = new HBox(2);
        outputButtons.getChildren().addAll(exportButton, copyButton);

        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getChildren().addAll(
                titleLabel,
                spacer,
                inputButtons,
                separator,
                outputButtons
        );

        this.getChildren().addAll(titleBar, inputControl);
    }

    private void setupBaseListeners() {
        titleLabel.textProperty().bind(title);
        mode.addListener((obs, oldVal, newMode) -> applyMode(newMode));
    }

    private void setupBaseBindings() {
        bindButtonVisibility(copyButton, showCopy);
        bindButtonVisibility(pasteButton, showPaste);
        bindButtonVisibility(clearButton, showClear);
        bindButtonVisibility(importButton, showImport);
        bindButtonVisibility(exportButton, showExport);

        // 智能分隔符逻辑
        var leftGroupVisible = showPaste.or(showImport).or(showClear);
        var rightGroupVisible = showCopy.or(showExport);
        separator.visibleProperty().bind(leftGroupVisible.and(rightGroupVisible));
        separator.managedProperty().bind(separator.visibleProperty());
    }

    private void applyMode(Mode newMode) {
        if (newMode == null) return;

        setShowCopy(true);
        setShowPaste(true);
        setShowClear(true);
        setShowImport(true);
        setShowExport(true);

        switch (newMode) {
            case INPUT -> {
                setShowExport(false);
                setShowCopy(false);
            }
            case OUTPUT -> {
                setShowPaste(false);
                setShowImport(false);
                setShowClear(false);
            }
            case NONE -> {
                setShowCopy(false);
                setShowPaste(false);
                setShowClear(false);
                setShowImport(false);
                setShowExport(false);
            }
            case ALL -> {
            }
        }
    }

    private void bindButtonVisibility(Button btn, BooleanProperty prop) {
        btn.visibleProperty().bind(prop);
        btn.managedProperty().bind(prop);
    }

    private Button createButton(String text, org.kordamp.ikonli.Ikon ikon, Runnable action) {
        Button btn = new Button();
        FontIcon fontIcon = new FontIcon(ikon);
        fontIcon.setIconSize(16);
        btn.setGraphic(fontIcon);
        btn.setTooltip(new Tooltip(text));
        btn.setOnAction(e -> action.run());
        btn.getStyleClass().add("tool-button");
        return btn;
    }

    public void copyText() {
        String text = inputControl.getText();
        if (text != null && !text.isEmpty()) {
            Clipboard.getSystemClipboard().setContent(new ClipboardContent() {{
                putString(text);
            }});
        }
    }

    public void pasteText() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            inputControl.replaceSelection(clipboard.getString());
        }
    }

    public void clear() {
        inputControl.clear();
    }

    public void importFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt", "*.log", "*.java", "*.json", "*.xml"));
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file != null) {
            try {
                inputControl.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void exportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存文件");
        fileChooser.setInitialFileName("export.txt");
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) {
            try {
                Files.writeString(file.toPath(), inputControl.getText(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public final String getText() {
        return inputControl.getText();
    }


    // 代理方法(代理原本的输入框控件)

    public final void setText(String text) {
        this.inputControl.setText(text);
    }

    public final StringProperty textProperty() {
        return inputControl.textProperty();
    }

    public final void setTextFormatter(TextFormatter<?> formatter) {
        inputControl.setTextFormatter(formatter);
    }

    public final ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    public final BooleanProperty editableProperty() {
        return inputControl.editableProperty();
    }

    public final boolean isEditable() {
        return inputControl.isEditable();
    }

    public final void setEditable(boolean value) {
        inputControl.setEditable(value);
    }

    public final StringProperty promptTextProperty() {
        return inputControl.promptTextProperty();
    }

    public final String getPromptText() {
        return inputControl.getPromptText();
    }

    public final void setPromptText(String value) {
        inputControl.setPromptText(value);
    }

    // 扩展方法
    public final StringProperty titleProperty() {
        return title;
    }

    public final String getTitle() {
        return title.get();
    }

    public final void setTitle(String title) {
        this.title.set(title);
    }

    public final Mode getMode() {
        return mode.get();
    }

    public final void setMode(Mode mode) {
        this.mode.set(mode);
    }

    public void setShowCopy(boolean v) {
        showCopy.set(v);
    }

    public void setShowPaste(boolean v) {
        showPaste.set(v);
    }

    public void setShowClear(boolean v) {
        showClear.set(v);
    }

    public void setShowImport(boolean v) {
        showImport.set(v);
    }

    public void setShowExport(boolean v) {
        showExport.set(v);
    }

    // 定义模式枚举
    public enum Mode {
        ALL,    // 全功能
        INPUT,  // 纯输入（隐藏导出、复制等）
        OUTPUT, // 纯输出（隐藏粘贴、导入等，只读）
        NONE    // 纯净模式
    }
}