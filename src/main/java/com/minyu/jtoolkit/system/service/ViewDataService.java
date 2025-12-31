package com.minyu.jtoolkit.system.service;

import com.minyu.jtoolkit.core.model.ViewState;

/**
 * ViewDataService
 */
public interface ViewDataService {
    void saveState(String key, ViewState viewState);

    <T> T loadState(String key, Class<T> clazz);
}
