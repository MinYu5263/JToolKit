package com.minyu.jtoolkit.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minyu.jtoolkit.core.model.ViewState;
import com.minyu.jtoolkit.system.entity.ViewData;

/**
 * ViewDataService
 */
public interface ViewDataService extends IService<ViewData> {
    void saveState(String key, ViewState viewState);

    <T> T loadState(String key, Class<T> clazz);
}
