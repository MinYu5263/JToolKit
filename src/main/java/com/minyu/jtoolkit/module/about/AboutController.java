package com.minyu.jtoolkit.module.about;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;
import java.util.Objects;

@Component
public class AboutController {

    @FXML private VBox root;
    @FXML private ImageView appLogo;
    @FXML private Label versionLabel;

    @FXML
    public void initialize() {
        try {
            appLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_128.png"))));
            appLogo.setFitWidth(64);
            appLogo.setFitHeight(64);
        } catch (Exception ignored) {}

        versionLabel.setText("版本：1.0-SNAPSHOT");
    }

    @FXML
    private void openGithub() {
        openLink("https://github.com/MinYu5263/JToolKit");
    }

    private void openLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
