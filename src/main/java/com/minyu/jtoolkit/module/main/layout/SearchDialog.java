package com.minyu.jtoolkit.module.main.layout;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.minyu.jtoolkit.core.component.ModalDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.function.Consumer;

public final class SearchDialog extends ModalDialog {

    private final MainModel model;

    private CustomTextField searchField;
    private ListView<SearchResult> resultList;

    public SearchDialog(MainModel model) {
        super();
        this.model = model;

        setId("search-dialog");
        header.setTitle("Search");

        content.setBody(createContent());
        content.setPrefSize(600, 440);

        init();
    }

    private VBox createContent() {
        var placeholder = new Label("Type to search");
        placeholder.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_MUTED);

        searchField = new CustomTextField();
        searchField.setLeft(new FontIcon(Material2MZ.SEARCH));
        VBox.setVgrow(searchField, Priority.NEVER);

        Consumer<SearchResult> clickHandler = item -> {
            if (item != null) {
                close();
                model.navigate(item.fxmlPath());
            }
        };

        resultList = new ListView<>();
        resultList.setPlaceholder(placeholder);
        resultList.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        resultList.setCellFactory(c -> new ResultListCell(clickHandler));
        VBox.setVgrow(resultList, Priority.ALWAYS);

        return new VBox(10, searchField, resultList);
    }

    private void init() {
        // 监听输入，调用 model 搜索
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                resultList.getItems().clear();
                return;
            }
            resultList.getItems().setAll(model.search(val));
        });

        // 键盘向下选择
        searchField.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.DOWN && !resultList.getItems().isEmpty()) {
                resultList.getSelectionModel().selectFirst();
                resultList.requestFocus();
            }
        });

        // 列表回车选中
        resultList.setOnKeyPressed(e -> {
            var selectionModel = resultList.getSelectionModel();
            if (e.getCode() == KeyCode.ENTER && !selectionModel.isEmpty()) {
                close();
                model.navigate(selectionModel.getSelectedItem().fxmlPath());
            }
        });

        // 快捷键 ESC 关闭 (虽然 ModalBox 自带，但双重保险)
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });
    }

    // 提供给外部调用，用于打开后自动聚焦输入框
    public void begForFocus() {
        searchField.requestFocus();
    }

    // 搜索结果列表单元格样式
    private static final class ResultListCell extends ListCell<SearchResult> {
        private final HBox root;
        private final Label parentLabel;
        private final Label targetLabel;

        public ResultListCell(Consumer<SearchResult> clickHandler) {
            super();

            // 样式定义
            parentLabel = new Label();
            parentLabel.getStyleClass().add(Styles.TEXT_MUTED);

            var separatorIcon = new FontIcon(Material2AL.CHEVRON_RIGHT);
            separatorIcon.setIconSize(12);
            separatorIcon.getStyleClass().add(Styles.TEXT_SUBTLE);

            var returnIcon = new FontIcon(Material2AL.KEYBOARD_RETURN);
            returnIcon.setIconSize(12);
            returnIcon.getStyleClass().add(Styles.TEXT_SUBTLE);

            targetLabel = new Label();
            targetLabel.getStyleClass().add(Styles.TEXT_BOLD);

            root = new HBox(parentLabel, separatorIcon, targetLabel, new Spacer(), returnIcon);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setSpacing(5);
            root.setPadding(new Insets(5, 10, 5, 10));

            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    clickHandler.accept(getItem());
                }
            });
        }

        @Override
        protected void updateItem(SearchResult item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setGraphic(null);
            } else {
                parentLabel.setText(item.parentTitle());
                targetLabel.setText(item.title());
                setGraphic(root);
            }
        }
    }
}