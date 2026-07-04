package com.minyu.jtoolkit.system.service;

import java.util.Map;

/**
 * 环境变量服务接口
 */
public interface EnvVarService {
    /**
     * 获取用户级变量
     */
    Map<String, String> getUserVariables();

    /**
     * 获取系统级变量
     */
    Map<String, String> getSystemVariables();

    /**
     * 设置用户变量
     */
    void setUserVariable(String key, String value);

    /**
     * 设置系统变量 (Windows 需要管理员权限)
     */
    void setSystemVariable(String key, String value);

    /**
     * 删除用户变量
     */
    void deleteUserVariable(String key);

    /**
     * 删除系统变量
     */
    void deleteSystemVariable(String key);

    /**
     * 判断当前实现是否支持写入操作
     */
    boolean isWritable();

    /**
     * 判断是否支持写入用户级变量
     */
    default boolean isUserWritable() {
        return isWritable();
    }

    /**
     * 判断是否支持写入系统级变量
     */
    default boolean isSystemWritable() {
        return isWritable();
    }
}
