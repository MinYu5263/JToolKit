package com.minyu.jtoolkit.system.service;

import com.minyu.jtoolkit.core.model.PersistentState;

/**
 * StorageService
 */
public interface StorageService {
    /**
     * 保存状态
     * @param key 唯一标识
     * @param state 必须是实现了 PersistentState 的对象，防止乱传
     */
    void save(String key, PersistentState state);

    /**
     * 加载状态
     * @param key 唯一标识
     * @param clazz 目标类型，必须实现 PersistentState
     */
    <T extends PersistentState> T load(String key, Class<T> clazz);

    /**
     * 清空所有数据
     */
    void clearAll();

    /**
     * 清空数据，但保留指定的 Key
     * @param retainedKeys 不需要被清除的 key
     */
    void clearExclude(String... retainedKeys);
}
