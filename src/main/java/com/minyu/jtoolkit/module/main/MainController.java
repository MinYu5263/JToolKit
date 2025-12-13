package com.minyu.jtoolkit.module.main;

import atlantafx.base.controls.ModalPane;
import com.minyu.jtoolkit.core.service.ViewLoader;
import com.minyu.jtoolkit.module.main.layout.MainModel;
import com.minyu.jtoolkit.module.main.layout.Sidebar;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    @FXML
    public void initialize() {
        initMenuTree();
    }

    private void initMenuTree() {
        model.initMenu();

        Sidebar sidebar = new Sidebar(model);

        mainLayout.setLeft(sidebar);

        model.selectedPageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                loadView(newVal);
            }
        });
    }

    private void loadView(String fxmlPath) {
        try {
            contentArea.getChildren().clear();
            Parent view = viewLoader.load(fxmlPath);
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
            contentArea.getChildren().add(new Label("无法加载页面: " + fxmlPath));
        }
    }
}