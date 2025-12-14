package com.minyu.jtoolkit.module;

import com.minyu.jtoolkit.system.service.ViewStateService;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.util.Duration;

/**
 * BaseController
 */
public abstract class BaseController<T> {
    private final ViewStateService viewStateService;
    private final PauseTransition autoSaveTimer;

    public BaseController(ViewStateService viewStateService) {
        this.viewStateService = viewStateService;
        autoSaveTimer = new PauseTransition(Duration.millis(1000));
        autoSaveTimer.setOnFinished(e -> {
            // 触发自动保存
            this.saveState();
        });
    }

    /**
     * 注册需要自动保存的组件
     */
    protected void observeChanges(Observable... observables) {
        for (Observable obs : observables) {
            obs.addListener(observable -> autoSaveTimer.playFromStart());
        }
    }

    /**
     * 【必须实现】定义该工具在数据库中的唯一Key
     * 建议格式: "module.feature.scope"
     */
    protected abstract String getStorageKey();

    /**
     * 【必须实现】返回状态类的Class对象，用于JSON反序列化
     */
    protected abstract Class<T> getStateType();

    /**
     * 【必须实现】恢复现场：将 DTO 数据填入 UI 组件
     * 注意：如果数据库没数据，state 可能为 null，需处理默认情况
     */
    protected abstract void restoreUI(T state);

    /**
     * 【必须实现】保存现场：从 UI 组件提取数据生成 DTO
     */
    protected abstract T captureUI();

    protected void loadState() {
        String key = getStorageKey();
        T state = viewStateService.loadState(key, getStateType());
        if (state != null) {
            restoreUI(state);
        }
    }

    protected void saveState() {
        T state = captureUI();
        if (state != null) {
            viewStateService.saveState(getStorageKey(), state);
        }
    }
}
