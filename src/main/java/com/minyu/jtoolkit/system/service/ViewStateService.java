package com.minyu.jtoolkit.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minyu.jtoolkit.system.entity.ViewState;

/**
 * ViewStateService
 */
public interface ViewStateService extends IService<ViewState> {
    void saveState(String key, Object stateObj);

    <T> T loadState(String key, Class<T> clazz);
}
