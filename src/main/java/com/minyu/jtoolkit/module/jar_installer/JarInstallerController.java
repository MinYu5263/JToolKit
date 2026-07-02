package com.minyu.jtoolkit.module.jar_installer;

import atlantafx.base.theme.Styles;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.core.component.EnhancedTextField;
import com.minyu.jtoolkit.core.component.PathTextField;
import com.minyu.jtoolkit.module.BaseController;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JAR 安装到本地 Maven 仓库工具
 */
@Slf4j
@Component
public class JarInstallerController extends BaseController<JarInstallerPersistentState> {

    @FXML private PathTextField jarFileField;
    @FXML private EnhancedTextField groupIdField;
    @FXML private EnhancedTextField artifactIdField;
    @FXML private EnhancedTextField versionField;
    @FXML private PathTextField pomFileField;
    @FXML private PathTextField sourcesFileField;
    @FXML private PathTextField javadocFileField;
    @FXML private Button installButton;
    @FXML private EnhancedTextArea outputArea;

    @FXML
    public void initView() {
        installButton.getStyleClass().add(Styles.ACCENT);
    }

    @FXML
    private void onInstall() {
        String jarPath = jarFileField.getText();
        String groupId = groupIdField.getText();
        String artifactId = artifactIdField.getText();
        String version = versionField.getText();

        // 校验必填项
        if (jarPath == null || jarPath.isBlank()) {
            appendOutput("[错误] 请选择 JAR 文件");
            return;
        }
        if (!Files.exists(Path.of(jarPath))) {
            appendOutput("[错误] JAR 文件不存在: " + jarPath);
            return;
        }
        if (groupId == null || groupId.isBlank()) {
            appendOutput("[错误] 请填写 Group ID");
            return;
        }
        if (artifactId == null || artifactId.isBlank()) {
            appendOutput("[错误] 请填写 Artifact ID");
            return;
        }
        if (version == null || version.isBlank()) {
            appendOutput("[错误] 请填写 Version");
            return;
        }

        List<String> cmd = buildCommand(jarPath, groupId, artifactId, version);
        appendOutput("> mvn " + String.join(" ", cmd.subList(0, cmd.size())));

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                int exitCode = process.waitFor();
                String output = sb.toString();

                Platform.runLater(() -> {
                    appendOutput(output.trim());
                    if (exitCode == 0) {
                        appendOutput("[成功] " + artifactId + ":" + version + " 已安装到本地 Maven 仓库");
                    } else {
                        appendOutput("[失败] mvn 退出码: " + exitCode);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> appendOutput("[异常] " + e.getMessage()));
            }
        }).start();
    }

    private List<String> buildCommand(String jarPath, String groupId, String artifactId, String version) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String mvn = isWindows ? "mvn.cmd" : "mvn";

        List<String> cmd = new ArrayList<>();
        cmd.add(mvn);
        cmd.add("install:install-file");
        cmd.add("-Dfile=" + jarPath);
        cmd.add("-DgroupId=" + groupId);
        cmd.add("-DartifactId=" + artifactId);
        cmd.add("-Dversion=" + version);
        cmd.add("-Dpackaging=jar");

        appendIfNotEmpty(cmd, "-DpomFile=", pomFileField.getText());
        appendIfNotEmpty(cmd, "-Dsources=", sourcesFileField.getText());
        appendIfNotEmpty(cmd, "-Djavadoc=", javadocFileField.getText());

        return cmd;
    }

    private void appendIfNotEmpty(List<String> cmd, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            cmd.add(prefix + value);
        }
    }

    private void appendOutput(String text) {
        String current = outputArea.getText();
        outputArea.setText((current != null && !current.isEmpty() ? current + "\n" : "") + text);
    }

    // ── 持久化 ──

    @Override
    protected String getViewKey() {
        return "jar_installer";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                jarFileField.textProperty(),
                groupIdField.textProperty(),
                artifactIdField.textProperty(),
                versionField.textProperty(),
                pomFileField.textProperty(),
                sourcesFileField.textProperty(),
                javadocFileField.textProperty()
        );
    }

    @Override
    protected void restoreValues(JarInstallerPersistentState state) {
        if (state == null) return;
        jarFileField.setText(state.getJarFilePath());
        groupIdField.setText(state.getGroupId());
        artifactIdField.setText(state.getArtifactId());
        versionField.setText(state.getVersion());
        pomFileField.setText(state.getPomFilePath());
        sourcesFileField.setText(state.getSourcesFilePath());
        javadocFileField.setText(state.getJavadocFilePath());
    }

    @Override
    protected JarInstallerPersistentState captureValues() {
        JarInstallerPersistentState state = new JarInstallerPersistentState();
        state.setJarFilePath(jarFileField.getText());
        state.setGroupId(groupIdField.getText());
        state.setArtifactId(artifactIdField.getText());
        state.setVersion(versionField.getText());
        state.setPomFilePath(pomFileField.getText());
        state.setSourcesFilePath(sourcesFileField.getText());
        state.setJavadocFilePath(javadocFileField.getText());
        return state;
    }
}
