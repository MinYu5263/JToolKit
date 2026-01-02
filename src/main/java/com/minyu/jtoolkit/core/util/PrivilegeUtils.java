package com.minyu.jtoolkit.core.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Optional;

/**
 * 系统提权工具类
 */
@Slf4j
public class PrivilegeUtils {
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
     * 以管理员身份重启当前应用
     * 兼容模式：
     * 1. 如果是打包后的 EXE 运行 -> 直接重启 EXE
     * 2. 如果是 Jar 包运行 -> java -jar xxx.jar
     */
    public static void relaunchAsAdmin() {
        try {
            // 获取当前进程的执行命令 (Java 9+ API)
            // 结果可能是 "C:\Program Files\JToolKit\JToolKit.exe" 或者 "C:\Java\bin\javaw.exe"
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
                    throw new RuntimeException("当前环境（IDE或非标准Jar）不支持自动重启提权，请手动以管理员运行。");
                }
            } else {
                log.info("Detected Native Launcher: {}", executablePath);
            }

            log.info("Relaunching: {} {}", runCommand, args);

            WinDef.INT_PTR result = Shell32.INSTANCE.ShellExecute(
                    null,
                    "runas",
                    runCommand,
                    args,
                    null,
                    1
            );

            int ret = result.intValue();
            if (ret > 32) {
                System.exit(0);
            } else {
                log.warn("Restart failed or cancelled by user. Error code: " + ret);
            }

        } catch (Exception e) {
            log.error("Failed to relaunch as admin", e);
            // 这里可以抛出异常让 Controller 捕获并弹窗提示用户手动操作
            throw new RuntimeException("无法自动重启，请尝试手动右键以管理员身份运行程序。", e);
        }
    }

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
            // 如果上面都没找到，返回 null，让外层抛出明确的异常（IDE 环境无法这样重启）
            return null;

        } catch (Exception e) {
            log.error("Error resolving jar path", e);
            return null;
        }
    }
}
