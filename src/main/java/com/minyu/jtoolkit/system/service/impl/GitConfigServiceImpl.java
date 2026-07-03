package com.minyu.jtoolkit.system.service.impl;

import com.minyu.jtoolkit.system.service.GitConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Git 配置服务实现类
 */
@Slf4j
@Service
public class GitConfigServiceImpl implements GitConfigService {

    private static final String SSH_DIR = System.getProperty("user.home") + File.separator + ".ssh";

    @Override
    public String getConfig(String key, File repoDir) {
        if (repoDir != null) {
            // 局部配置：git config --get <key>
            return runCommand(repoDir, "git", "config", "--get", key);
        } else {
            // 全局配置：git config --global --get <key>
            return runCommand(null, "git", "config", "--global", "--get", key);
        }
    }

    @Override
    public void setConfig(String key, String value, File repoDir) {
        if (repoDir != null) {
            // 局部配置：git config <key> <value>
            runCommand(repoDir, "git", "config", key, value);
        } else {
            // 全局配置：git config --global <key> <value>
            runCommand(null, "git", "config", "--global", key, value);
        }
    }

    @Override
    public void unsetConfig(String key, File repoDir) {
        if (repoDir != null) {
            runCommand(repoDir, "git", "config", "--unset", key);
        } else {
            runCommand(null, "git", "config", "--global", "--unset", key);
        }
    }

    @Override
    public Map<String, String> getAllConfigs(File repoDir) {
        String output;
        if (repoDir != null) {
            // 局部：git config --list
            output = runCommand(repoDir, "git", "config", "--list");
        } else {
            // 全局：git config --global --list
            output = runCommand(null, "git", "config", "--global", "--list");
        }

        Map<String, String> map = new LinkedHashMap<>(); // 保持 Git 输出的顺序
        if (!output.isBlank()) {
            // 输出格式通常是 key=value，按行分割
            output.lines().forEach(line -> {
                int splitIndex = line.indexOf('=');
                if (splitIndex > 0) {
                    String key = line.substring(0, splitIndex).trim();
                    String value = line.substring(splitIndex + 1).trim();
                    // Git list 可能会有重复 key（多值），这里简化处理：覆盖（取最后一个生效值）
                    map.put(key, value);
                }
            });
        }
        return map;
    }

    @Override
    public List<File> listPublicKeys() {
        File dir = new File(SSH_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".pub"));
        // 按文件名排序，看起来整齐点
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            return List.of(files);
        }
        return Collections.emptyList();
    }

    @Override
    public String readPublicKey(File file) {
        try {
            return Files.readString(file.toPath()).trim();
        } catch (Exception e) {
            log.error("Failed to read key file", e);
            return "无法读取文件内容: " + e.getMessage();
        }
    }

    @Override
    public void generateSshKey(String filename, String keyType, String email) throws Exception {
        File dir = new File(SSH_DIR);
        if (!dir.exists()) dir.mkdirs();

        File privateKeyFile = new File(dir, filename);
        if (privateKeyFile.exists()) {
            throw new RuntimeException("文件已存在: " + filename);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "ssh-keygen", "-t", keyType, "-C", email,
                "-f", privateKeyFile.getAbsolutePath(), "-N", ""
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("生成失败:\n" + output);
        }
    }

    @Override
    public void renameSshKey(File pubKeyFile, String newName) throws Exception {
        String pubPath = pubKeyFile.getAbsolutePath();
        // 去掉 .pub 后缀得到私钥路径
        String privatePath = pubPath.replaceAll("\\.pub$", "");

        File newPubFile = new File(pubKeyFile.getParent(), newName + ".pub");
        File newPrivateFile = new File(pubKeyFile.getParent(), newName);

        if (newPubFile.exists() || newPrivateFile.exists()) {
            throw new RuntimeException("目标文件已存在: " + newName);
        }

        if (!new File(privatePath).renameTo(newPrivateFile)) {
            throw new RuntimeException("重命名私钥失败");
        }
        if (!pubKeyFile.renameTo(newPubFile)) {
            // 回滚
            newPrivateFile.renameTo(new File(privatePath));
            throw new RuntimeException("重命名公钥失败");
        }
    }

    /**
     * 核心方法：执行系统命令
     */
    private String runCommand(File workingDir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);

            // 关键：切换工作目录，实现对特定 Git 仓库的操作
            if (workingDir != null) {
                if (!workingDir.exists()) {
                    log.warn("Directory not exists: {}", workingDir);
                    return "";
                }
                pb.directory(workingDir);
            }

            // 合并错误流到标准流
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 注意：Windows Git Bash 有时默认输出 GBK，如果乱码请改为 Charset.forName("GBK")
            Charset charset = StandardCharsets.UTF_8;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
            }

            // 等待命令结束，设置超时防止卡死
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return sb.toString().trim();
            } else {
                return "";
            }

        } catch (Exception e) {
            log.error("Git command execution failed: {}", String.join(" ", command), e);
            return "";
        }
    }
}
