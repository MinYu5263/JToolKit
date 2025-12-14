package com.minyu.jtoolkit.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minyu.jtoolkit.system.entity.ViewState;
import com.minyu.jtoolkit.system.mapper.ViewStateMapper;
import com.minyu.jtoolkit.system.service.ViewStateService;
import org.springframework.stereotype.Service;

/**
 * ViewStateServiceImpl
 */
@Service
public class ViewStateServiceImpl extends ServiceImpl<ViewStateMapper, ViewState> implements ViewStateService {
    public void saveState(String key, Object stateObj) {
        String jsonString = JSON.toJSONString(stateObj);
        ViewState viewState = new ViewState();
        viewState.setViewKey(key);
        viewState.setViewData(jsonString);
        this.saveOrUpdate(viewState);
    }

    public <T> T loadState(String key, Class<T> clazz) {
        ViewState viewState = this.getById(key);
        if (viewState == null) {
            return null;
        }
        return JSON.parseObject(viewState.getViewData(), clazz);
    }
}
