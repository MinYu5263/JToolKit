package com.minyu.jtoolkit.module.git_config;

import com.minyu.jtoolkit.system.service.GitConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;

/**
 * SshKeyManagerDialog
 */
public class SshKeyManagerDialog extends Dialog<Void> {
    private final GitConfigService gitService; // 持有服务引用
    private final ListView<File> keyList = new ListView<>();
    private final TextArea keyContentArea = new TextArea();
    private final Label keyPathLabel = new Label("选择左侧密钥查看详情");

    // 构造函数接收 service
    public SshKeyManagerDialog(GitConfigService gitService) {
        this.gitService = gitService;

        setTitle("SSH 密钥管理");
        setHeaderText("管理 ~/.ssh 目录下的密钥。配置到 GitHub/GitLab 时请复制公钥内容。");
        getDialogPane().setPrefSize(700, 450);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // === 布局 ===
        SplitPane splitPane = new SplitPane();

        // 左侧：列表 + 新建按钮
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));

        keyList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(new FontIcon(Material2MZ.VPN_KEY)); // 图标
                }
            }
        });
        keyList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> loadKeyContent(newVal));
        VBox.setVgrow(keyList, Priority.ALWAYS);

        Button btnGenerate = new Button("生成新密钥");
        btnGenerate.setMaxWidth(Double.MAX_VALUE);
        btnGenerate.setOnAction(e -> showGenerateDialog());

        leftPane.getChildren().addAll(new Label("本地公钥列表:"), keyList, btnGenerate);

        // 右侧：内容详情 + 复制按钮
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));

        keyContentArea.setEditable(false);
        keyContentArea.setWrapText(true);
        keyContentArea.setPromptText("公钥内容将显示在这里");
        VBox.setVgrow(keyContentArea, Priority.ALWAYS);

        Button btnCopy = new Button("复制到剪贴板");
        btnCopy.getStyleClass().add("accent");
        btnCopy.setOnAction(e -> copyToClipboard());

        HBox rightActions = new HBox(10, btnCopy);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        rightPane.getChildren().addAll(keyPathLabel, keyContentArea, rightActions);

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.35);

        getDialogPane().setContent(splitPane);
        refreshList();
    }

    private void refreshList() {
        keyList.getItems().setAll(gitService.listPublicKeys());
    }

    private void loadKeyContent(File file) {
        if (file == null) {
            keyPathLabel.setText("");
            keyContentArea.clear();
            return;
        }
        keyPathLabel.setText(file.getAbsolutePath());
        keyContentArea.setText(gitService.readPublicKey(file));
    }

    private void copyToClipboard() {
        String content = keyContentArea.getText();
        if (content != null && !content.isEmpty()) {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content);
            Clipboard.getSystemClipboard().setContent(clipboardContent);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "已复制到剪贴板！");
            alert.initOwner(getDialogPane().getScene().getWindow());
            alert.show();
        }
    }

    private void showGenerateDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("生成 SSH 密钥");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        TextField nameField = new TextField("id_rsa_new");
        TextField emailField = new TextField("");
        emailField.setPromptText("your_email@example.com");

        grid.add(new Label("文件名:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("邮箱/备注:"), 0, 1);
        grid.add(emailField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                try {
                    // 调用 gitService.generateSshKey()
                    gitService.generateSshKey(nameField.getText(), emailField.getText());
                    refreshList();
                    new Alert(Alert.AlertType.INFORMATION, "密钥生成成功！").show();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "生成失败: " + e.getMessage()).show();
                }
            }
        });
    }
}
