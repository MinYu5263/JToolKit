package com.minyu.jtoolkit.module.main.component; // 1. 修改包名

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Spacer;
import atlantafx.base.controls.Tile;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Tweaks;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public abstract class ModalDialog extends ModalBox {

    public static final String MODAL_PANE_ID = "main-modal-pane";

    protected final Card content = new Card();
    protected final Tile header = new Tile();

    public ModalDialog() {
        // 传入 CSS 选择器 ID (#main-modal-pane)，告诉 ModalBox 它属于哪个层
        super("#" + MODAL_PANE_ID);
        createView();
    }

    // 3. 修改 show 方法，根据 ID 查找 ModalPane 并显示自己
    public void show(Scene scene) {
        var modalPane = (ModalPane) scene.lookup("#" + MODAL_PANE_ID);
        if (modalPane == null) {
            throw new IllegalStateException(
                    "ModalPane not found in Scene. Make sure MainView.fxml has a ModalPane with id='" + MODAL_PANE_ID + "'"
            );
        }
        modalPane.show(this);
    }

    protected void createView() {
        content.setHeader(header);
        content.getStyleClass().add(Tweaks.EDGE_TO_EDGE);

        // 保证使用合适的大小
        setMinWidth(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        // 布局设置
        AnchorPane.setTopAnchor(content, 0d);
        AnchorPane.setRightAnchor(content, 0d);
        AnchorPane.setBottomAnchor(content, 0d);
        AnchorPane.setLeftAnchor(content, 0d);

        addContent(content);
        getStyleClass().add("modal-dialog");
    }

    protected HBox createDefaultFooter() {
        var closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("form-action");
        closeBtn.setCancelButton(true);
        closeBtn.setOnAction(e -> close());

        var footer = new HBox(10, new Spacer(), closeBtn);
        footer.getStyleClass().add("footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        VBox.setVgrow(footer, Priority.NEVER);

        return footer;
    }
}