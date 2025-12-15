package com.minyu.jtoolkit.module;

import com.minyu.jtoolkit.system.service.ViewStateService;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.util.Duration;

/**
 * 视图控制器基类，处理视图状态的加载、保存及自动保存逻辑，定义状态操作抽象方法
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
     * 注册需监听的组件，变更时触发自动保存
     */
    protected void observeChanges(Observable... observables) {
        for (Observable obs : observables) {
            obs.addListener(observable -> autoSaveTimer.playFromStart());
        }
    }

    /**
     * 获取视图状态在数据库中的唯一标识
     */
    protected abstract String getStorageKey();

    /**
     * 获取状态类的Class对象，用于反序列化
     */
    protected abstract Class<T> getStateType();

    /**
     * 从状态数据恢复UI组件
     */
    protected abstract void restoreUI(T state);

    /**
     * 从UI组件提取数据生成状态对象
     */
    protected abstract T captureUI();

    /**
     * 加载视图状态并恢复UI
     */
    protected void loadState() {
        String key = getStorageKey();
        T state = viewStateService.loadState(key, getStateType());
        if (state != null) {
            restoreUI(state);
        }
    }

    /**
     * 保存当前UI状态
     */
    protected void saveState() {
        T state = captureUI();
        if (state != null) {
            viewStateService.saveState(getStorageKey(), state);
        }
    }
}
