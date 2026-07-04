package com.minyu.jtoolkit.system.service.impl;

import com.minyu.jtoolkit.system.service.EnvVarService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 适用于 Mac 和 Linux 的实现。
 */
public class PosixEnvService implements EnvVarService {

    private static final Pattern EXPORT_PATTERN = Pattern.compile("^\\s*export\\s+([A-Za-z_][A-Za-z0-9_]*)=(.*)\\s*$");
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final Path homeDir;
    private final String shell;
    private final Map<String, String> processEnv;

    public PosixEnvService() {
        this(
                Path.of(System.getProperty("user.home")),
                System.getenv("SHELL"),
                System.getenv()
        );
    }

    PosixEnvService(Path homeDir, String shell, Map<String, String> processEnv) {
        this.homeDir = homeDir;
        this.shell = shell == null ? "" : shell;
        this.processEnv = processEnv;
    }

    @Override
    public Map<String, String> getUserVariables() {
        TreeMap<String, String> result = new TreeMap<>(processEnv);
        result.putAll(readProfileExports(profilePath()));
        return result;
    }

    @Override
    public Map<String, String> getSystemVariables() {
        return Map.of();
    }

    @Override
    public void setUserVariable(String key, String value) {
        validateKey(key);
        Path profile = profilePath();
        String content = readFile(profile);
        content = removeManagedBlock(content, key).stripTrailing();

        String block = startMarker(key) + System.lineSeparator()
                + "export " + key + "=" + shellQuote(value == null ? "" : value) + System.lineSeparator()
                + endMarker(key) + System.lineSeparator();
        String updated = content.isBlank()
                ? block
                : content + System.lineSeparator() + System.lineSeparator() + block;

        writeFile(profile, updated);
    }

    @Override
    public void setSystemVariable(String key, String value) {
        throw new UnsupportedOperationException("MacOS/Linux 系统变量通常需要修改 /etc/profile、/etc/environment 等系统文件，请使用管理员权限手动处理。");
    }

    @Override
    public void deleteUserVariable(String key) {
        validateKey(key);
        Path profile = profilePath();
        String content = readFile(profile);
        writeFile(profile, removeManagedBlock(content, key).stripTrailing() + System.lineSeparator());
    }

    @Override
    public void deleteSystemVariable(String key) {
        throw new UnsupportedOperationException("MacOS/Linux 系统变量通常需要修改 /etc/profile、/etc/environment 等系统文件，请使用管理员权限手动处理。");
    }

    @Override
    public boolean isWritable() {
        return isUserWritable();
    }

    @Override
    public boolean isUserWritable() {
        return true;
    }

    @Override
    public boolean isSystemWritable() {
        return false;
    }

    private Path profilePath() {
        if (shell.endsWith("zsh")) {
            return homeDir.resolve(".zshrc");
        }
        if (shell.endsWith("bash")) {
            return homeDir.resolve(".bash_profile");
        }
        return homeDir.resolve(".profile");
    }

    private Map<String, String> readProfileExports(Path profile) {
        String content = readFile(profile);
        Map<String, String> exports = new LinkedHashMap<>();
        for (String line : content.split("\\R")) {
            Matcher matcher = EXPORT_PATTERN.matcher(line);
            if (matcher.matches()) {
                exports.put(matcher.group(1), unquote(matcher.group(2).trim()));
            }
        }
        return exports;
    }

    private String removeManagedBlock(String content, String key) {
        String start = Pattern.quote(startMarker(key));
        String end = Pattern.quote(endMarker(key));
        return content.replaceAll("(?s)\\R?" + start + "\\R.*?\\R" + end + "\\R?", System.lineSeparator());
    }

    private String startMarker(String key) {
        return "# >>> JToolKit env: " + key + " >>>";
    }

    private String endMarker(String key) {
        return "# <<< JToolKit env: " + key + " <<<";
    }

    private void validateKey(String key) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("变量名只能包含字母、数字、下划线，且不能以数字开头。");
        }
    }

    private String shellQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$").replace("`", "\\`") + "\"";
    }

    private String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\$", "$")
                    .replace("\\`", "`")
                    .replace("\\\\", "\\");
        }
        return value;
    }

    private String readFile(Path file) {
        if (!Files.exists(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("读取环境变量配置文件失败: " + file, e);
        }
    }

    private void writeFile(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写入环境变量配置文件失败: " + file, e);
        }
    }
}
