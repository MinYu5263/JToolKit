package com.minyu.jtoolkit.system.service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Git 配置服务接口
 */
public interface GitConfigService {
    /**
     * 获取配置值
     *
     * @param key     配置键 (例如 "user.name")
     * @param repoDir 项目目录。如果不传(null)，则获取全局配置(--global)
     * @return 配置值，如果未设置或获取失败返回空字符串
     */
    String getConfig(String key, File repoDir);

    /**
     * 设置配置值
     *
     * @param key     配置键
     * @param value   要设置的值
     * @param repoDir 项目目录。如果不传(null)，则写入全局配置
     */
    void setConfig(String key, String value, File repoDir);

    /**
     * 移除/重置配置
     * (通常用于关闭代理，或者让局部配置重新继承全局配置)
     *
     * @param key     配置键
     * @param repoDir 项目目录。如果不传(null)，则移除全局配置
     */
    void unsetConfig(String key, File repoDir);

    /**
     * 获取该作用域下的所有配置
     */
    Map<String, String> getAllConfigs(File repoDir);

    /**
     * 列出 ~/.ssh 下所有的公钥文件 (.pub)
     */
    List<File> listPublicKeys();

    /**
     * 读取公钥文件内容
     */
    String readPublicKey(File file);

    /**
     * 生成 SSH 密钥
     * @param filename 文件名 (如 id_ed25519)
     * @param keyType  密钥类型 (ed25519, rsa, ecdsa)
     * @param email    邮箱/备注
     */
    void generateSshKey(String filename, String keyType, String email) throws Exception;

    /**
     * 重命名 SSH 密钥文件（同时重命名私钥和 .pub 公钥）
     * @param pubKeyFile 当前公钥文件
     * @param newName    新文件名（不含扩展名）
     */
    void renameSshKey(File pubKeyFile, String newName) throws Exception;
}
