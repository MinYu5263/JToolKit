package com.minyu.jtoolkit.core.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Optional;

/**
 * 应用生命周期工具类：负责权限检查、自动重启等
 * (原 PrivilegeUtils 重构升级版)
 */
@Slf4j
public class AppLifecycleUtils {

    public interface Shell32Ext extends StdCallLibrary {
        Shell32Ext INSTANCE = Native.load("shell32", Shell32Ext.class);

        // 核心方法：检查是否是管理员
        boolean IsUserAnAdmin();
    }

    /**
     * 判断当前是否拥有管理员权限
     */
    public static boolean isAdmin() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return false;
        }
        try {
            return Shell32Ext.INSTANCE.IsUserAnAdmin();
        } catch (Exception e) {
            log.warn("Failed to check admin status via JNA: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 普通重启应用
     */
    public static void restart() {
        performRestart(false);
    }

    /**
     * 以管理员身份重启应用 (会弹出 UAC 提示)
     */
    public static void restartAsAdmin() {
        performRestart(true);
    }

    /**
     * 执行重启的核心逻辑
     *
     * @param asAdmin 是否请求管理员权限
     */
    private static void performRestart(boolean asAdmin) {
        try {
            // 1. 获取当前进程的执行命令 (Java 9+ API)
            Optional<String> commandOpt = ProcessHandle.current().info().command();

            if (commandOpt.isEmpty()) {
                throw new RuntimeException("无法获取当前进程路径");
            }

            String executablePath = commandOpt.get();
            String args = "";
            String runCommand = executablePath;

            // 判断运行方式
            boolean isRunningAsJava = executablePath.toLowerCase().endsWith("java.exe")
                    || executablePath.toLowerCase().endsWith("javaw.exe");

            if (isRunningAsJava) {
                // === 开发环境 / 命令行 java -jar 模式 ===
                // 需要手动拼接 java -jar xxx.jar
                File currentJar = getCurrentJarPath();

                if (currentJar != null && currentJar.getName().toLowerCase().endsWith(".jar")) {
                    runCommand = executablePath;
                    args = "-jar \"" + currentJar.getAbsolutePath() + "\"";
                } else {
                    throw new RuntimeException("当前环境（IDE或非标准Jar）不支持自动重启，请手动重启。");
                }
            } else {
                // === Native 模式 (如 jpackage 打包后的 exe) ===
                log.info("Detected Native Launcher: {}", executablePath);
            }

            // 2. 决定启动模式
            // "runas" = 以管理员身份运行
            // "open"  = 普通打开 (如果传 null 也是默认打开)
            String verb = asAdmin ? "runas" : "open";

            log.info("Restarting app... Mode: {}, Command: {} {}", verb, runCommand, args);

            // 3. 调用 Windows ShellExecute API 执行启动
            WinDef.INT_PTR result = Shell32.INSTANCE.ShellExecute(
                    null,
                    verb,
                    runCommand,
                    args,
                    null,
                    1 // SW_SHOWNORMAL (正常显示窗口)
            );

            // 4. 检查结果并退出旧进程
            // ShellExecute 返回值大于 32 表示成功
            int ret = result.intValue();
            if (ret > 32) {
                // 启动成功后，关闭当前应用
                // 使用 Platform.exit() 确保 JavaFX 线程退出，配合 System.exit 确保进程终止
                javafx.application.Platform.exit();
                System.exit(0);
            } else {
                log.warn("Restart failed or cancelled by user. Error code: " + ret);
                throw new RuntimeException("重启操作被取消或失败，错误码: " + ret);
            }

        } catch (Exception e) {
            log.error("Failed to restart app", e);
            throw new RuntimeException("无法自动重启: " + e.getMessage(), e);
        }
    }

    /**
     * 获取当前运行的 Jar 包路径
     */
    private static File getCurrentJarPath() {
        try {
            // 策略 1: 检查 java.class.path
            // 在使用 java -jar xxx.jar 运行时，java.class.path 只包含这个 jar 文件
            String classPath = System.getProperty("java.class.path");
            if (classPath != null && !classPath.isEmpty()) {
                // 处理路径分隔符（虽然 -jar 模式下通常没有分隔符）
                String[] paths = classPath.split(File.pathSeparator);
                for (String p : paths) {
                    // 简单的判断：如果是 .jar 结尾且文件存在，那就是它了
                    File f = new File(p);
                    if (f.exists() && f.isFile() && p.toLowerCase().endsWith(".jar")) {
                        return f.getAbsoluteFile();
                    }
                }
            }

            // 策略 2: 检查启动参数 (ProcessHandle)
            // 适用于 java -jar 参数被明确传递的情况
            Optional<String[]> argsOpt = ProcessHandle.current().info().arguments();
            if (argsOpt.isPresent()) {
                String[] args = argsOpt.get();
                for (int i = 0; i < args.length - 1; i++) {
                    if (args[i].equalsIgnoreCase("-jar")) {
                        return new File(args[i + 1]).getAbsoluteFile();
                    }
                }
            }

            // 策略 3: IDE 开发环境兜底
            // 如果上面都没找到，返回 null
            return null;

        } catch (Exception e) {
            log.error("Error resolving jar path", e);
            return null;
        }
    }
}