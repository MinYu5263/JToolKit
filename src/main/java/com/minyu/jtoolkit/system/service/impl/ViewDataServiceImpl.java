package com.minyu.jtoolkit.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minyu.jtoolkit.core.model.ViewState;
import com.minyu.jtoolkit.system.entity.ViewData;
import com.minyu.jtoolkit.system.mapper.ViewDataMapper;
import com.minyu.jtoolkit.system.service.ViewDataService;
import org.springframework.stereotype.Service;

/**
 * ViewDataServiceImpl
 */
@Service
public class ViewDataServiceImpl extends ServiceImpl<ViewDataMapper, ViewData> implements ViewDataService {
    public void saveState(String key, ViewState viewData) {
        String jsonString = JSON.toJSONString(viewData);
        ViewData viewState = new ViewData();
        viewState.setViewKey(key);
        viewState.setViewState(jsonString);
        this.saveOrUpdate(viewState);
    }

    public <T> T loadState(String key, Class<T> clazz) {
        ViewData viewState = this.getById(key);
        if (viewState == null) {
            return null;
        }
        return JSON.parseObject(viewState.getViewState(), clazz);
    }
}
