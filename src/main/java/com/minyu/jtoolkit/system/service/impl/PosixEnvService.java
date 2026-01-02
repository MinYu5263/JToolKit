package com.minyu.jtoolkit.system.service.impl;

import com.minyu.jtoolkit.system.service.EnvVarService;

import java.util.Collections;
import java.util.Map;

/**
 * 适用于 Mac 和 Linux 的实现 (只读)
 */
public class PosixEnvService implements EnvVarService {

    @Override
    public Map<String, String> getUserVariables() {
        return System.getenv();
    }

    @Override
    public Map<String, String> getSystemVariables() {
        return Collections.emptyMap();
    }

    @Override
    public void setUserVariable(String key, String value) {
        throw new UnsupportedOperationException("MacOS/Linux 系统暂不支持通过 UI 修改环境变量，请手动修改 ~/.bash_profile 或 ~/.zshrc");
    }

    @Override
    public void setSystemVariable(String key, String value) {
        throw new UnsupportedOperationException("MacOS/Linux 系统暂不支持通过 UI 修改环境变量");
    }

    @Override
    public void deleteUserVariable(String key) {
        throw new UnsupportedOperationException("MacOS/Linux 系统暂不支持通过 UI 删除环境变量");
    }

    @Override
    public void deleteSystemVariable(String key) {
        throw new UnsupportedOperationException("MacOS/Linux 系统暂不支持通过 UI 删除环境变量");
    }

    @Override
    public boolean isWritable() {
        return false;
    }
}