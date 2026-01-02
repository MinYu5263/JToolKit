package com.minyu.jtoolkit.system.service.impl;

import com.minyu.jtoolkit.system.service.EnvVarService;
import com.sun.jna.platform.win32.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class WindowsEnvService implements EnvVarService {

    private static final String USER_ENV_PATH = "Environment";
    private static final String SYS_ENV_PATH = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";

    @Override
    public Map<String, String> getUserVariables() {
        Map<String, Object> rawMap = Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER, USER_ENV_PATH);

        return convertToStringMap(rawMap);
    }

    @Override
    public Map<String, String> getSystemVariables() {
        Map<String, Object> rawMap = Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE, SYS_ENV_PATH);
        return convertToStringMap(rawMap);
    }

    @Override
    public void setUserVariable(String key, String value) {
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, USER_ENV_PATH, key, value);
        broadcastChange();
    }

    @Override
    public void setSystemVariable(String key, String value) {
        try {
            Advapi32Util.registrySetStringValue(WinReg.HKEY_LOCAL_MACHINE, SYS_ENV_PATH, key, value);
            broadcastChange();
        } catch (Win32Exception e) {
            handlePermissionError(e);
        }
    }

    @Override
    public void deleteUserVariable(String key) {
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, USER_ENV_PATH, key)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, USER_ENV_PATH, key);
            broadcastChange();
        }
    }

    @Override
    public void deleteSystemVariable(String key) {
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, SYS_ENV_PATH, key)) {
                Advapi32Util.registryDeleteValue(WinReg.HKEY_LOCAL_MACHINE, SYS_ENV_PATH, key);
                broadcastChange();
            }
        } catch (Win32Exception e) {
            handlePermissionError(e);
        }
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    /**
     * 发送系统广播，通知 Explorer 和其他进程环境变量已改变
     * 相当于 Windows API: SendMessageTimeout(HWND_BROADCAST, WM_SETTINGCHANGE, 0, "Environment", ...)
     */
    private void broadcastChange() {
        try {
            WinDef.DWORDByReference result = new WinDef.DWORDByReference();
            User32.INSTANCE.SendMessageTimeout(
                    WinUser.HWND_BROADCAST,
                    0x001A,
                    new WinDef.WPARAM(0),
                    new WinDef.LPARAM(0),
                    WinUser.SMTO_ABORTIFHUNG,
                    100,
                    result
            );
            log.info("System broadcast sent.");
        } catch (Exception e) {
            log.error("Failed to broadcast environment change", e);
        }
    }

    private void handlePermissionError(Win32Exception e) {
        // 错误码 5 代表 Access Denied
        if (e.getErrorCode() == 5) {
            throw new RuntimeException("权限不足：修改系统变量需要以【管理员身份】运行此程序。", e);
        }
        throw e;
    }

    private Map<String, String> convertToStringMap(Map<String, Object> rawMap) {
        TreeMap<String, String> result = new TreeMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                result.put(entry.getKey(), value.toString());
            }
        }
        return result;
    }
}
