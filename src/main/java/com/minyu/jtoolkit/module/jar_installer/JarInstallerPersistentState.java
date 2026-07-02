package com.minyu.jtoolkit.module.jar_installer;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

/**
 * JAR 安装到本地 Maven 仓库 — 持久化状态
 */
@Data
public class JarInstallerPersistentState implements PersistentState {
    private String jarFilePath;
    private String groupId;
    private String artifactId;
    private String version;
    private String pomFilePath;
    private String sourcesFilePath;
    private String javadocFilePath;
}
