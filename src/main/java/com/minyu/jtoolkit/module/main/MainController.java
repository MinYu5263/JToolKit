package com.minyu.jtoolkit.module.main;

import atlantafx.base.controls.ModalPane;
import com.minyu.jtoolkit.core.service.HotKeyManager;
import com.minyu.jtoolkit.core.service.ViewLoader;
import com.minyu.jtoolkit.module.main.layout.MainModel;
import com.minyu.jtoolkit.module.main.layout.Sidebar;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MainController {

    private final ViewLoader viewLoader;

    @FXML
    private StackPane rootPane;
    @FXML
    private BorderPane mainLayout;
    @FXML
    private ModalPane modalPane;
    @FXML
    private StackPane contentArea;

    private final MainModel model = new MainModel();
    private final Map<String, Parent> viewCache = new HashMap<>();
    private final HotKeyManager hotKeyManager;

    @FXML
    public void initialize() {
        initMenuTree();
    }

    private void initMenuTree() {
        model.initMenu();

        Sidebar sidebar = new Sidebar(model);

        mainLayout.setLeft(sidebar);

        model.selectedPageProperty().addListener((obs, oldVal, newVal) -> {
            if (StringUtils.isNotBlank(newVal)) {
                loadView(newVal);
            }
        });

        // 将搜索框的文本属性与搜索快捷见的文本属性进行绑定
        model.searchShortcutTextProperty().bind(
                Bindings.createStringBinding(
                        () -> hotKeyManager.getDisplayText("search"),
                        hotKeyManager.getShortcuts()
                )
        );
        initGlobalShortcuts(sidebar);
    }

    private void loadView(String fxmlPath) {
        try {
            contentArea.getChildren().clear();
            Parent view;
            if (viewCache.containsKey(fxmlPath)) {
                view = viewCache.get(fxmlPath);
            } else {
                view = viewLoader.load(fxmlPath);
                viewCache.put(fxmlPath, view);
            }
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
            viewCache.remove(fxmlPath);
            contentArea.getChildren().add(new Label("无法加载页面: " + fxmlPath));
        }
    }

    private void initGlobalShortcuts(Sidebar sidebar) {
        // 监听 Scene 的按键事件
        mainLayout.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> {
                    if (hotKeyManager.isMatch("search", e)) {
                        e.consume();
                        sidebar.openSearchDialog();
                    }
                });
            }
        });
    }
}