package com.minyu.jtoolkit.module;

import com.minyu.jtoolkit.core.model.PersistentState;
import com.minyu.jtoolkit.system.service.StorageService;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.fxml.Initializable;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 视图控制器基类，规范生命周期：初始化视图 -> 加载数据 -> 注册监听
 *
 * @param <T> 视图状态实体类
 */
public abstract class BaseController<T extends PersistentState> implements Initializable {
    // 自动保存延迟时间（毫秒）
    protected static final long AUTO_SAVE_DELAY_MS = 1000;
    private final PauseTransition autoSaveTimer;
    private final Class<T> stateType;
    private StorageService storageService;

    @SuppressWarnings("unchecked")
    public BaseController() {
        this.stateType = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), BaseController.class);
        autoSaveTimer = new PauseTransition(Duration.millis(AUTO_SAVE_DELAY_MS));
        autoSaveTimer.setOnFinished(e -> this.saveValues());
    }

    @Autowired
    public void setViewStateService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Autowired
    public void setJsonFileViewDataService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. 初始化视图组件（设置CellFactory、Converter等）
        initView();

        // 2. 加载数据（有存档则恢复，否则初始化默认值）
        loadStateOrDefaults();

        // 3. 注册自动保存监听
        registerAutoSave();
    }

    /**
     * 初始化视图属性（子类实现：处理与数据无关的UI设置）
     */
    protected void initView() {

    }

    /**
     * 恢复历史值（子类实现：将读取的state设置到UI组件）
     *
     * @param state 历史保存的状态对象
     */
    protected abstract void restoreValues(T state);

    /**
     * 初始化默认值（无历史存档时调用，子类按需重写）
     */
    protected void initDefaultValues() {
    }

    /**
     * 获取需自动保存的属性列表（子类实现：返回需监听的属性列表，无需自动保存可返回空）
     *
     * @return 观察对象列表，如List.of(textField.textProperty())
     */
    protected List<Observable> getObservables() {
        return List.of();
    }

    /**
     * 从UI提取值（子类实现：封装当前UI值为State对象）
     *
     * @return 状态对象
     */
    protected abstract T captureValues();

    /**
     * 注册需监听的组件，变更时触发自动保存
     */
    protected void observeChanges(Observable... observables) {
        for (Observable obs : observables) {
            obs.addListener(observable -> autoSaveTimer.playFromStart());
        }
    }

    /**
     * 获取视图状态在存储中的唯一标识
     */
    protected abstract String getViewKey();

    /**
     * 获取状态类Class（用于反序列化）
     */
    protected Class<T> getStorageType() {
        return null;
    }

    ;

    /**
     * 加载视图状态并恢复UI
     */
    protected void loadStateOrDefaults() {
        String key = getViewKey();
        T state = storageService.load(key, this.stateType);
        if (state != null) {
            restoreValues(state);
        } else {
            initDefaultValues();
        }
    }

    /**
     * 注册自动保存
     */
    private void registerAutoSave() {
        List<Observable> observables = getObservables();
        if (observables != null && !observables.isEmpty()) {
            for (Observable obs : observables) {
                obs.addListener(observable -> autoSaveTimer.playFromStart());
            }
        }
    }

    /**
     * 保存当前UI状态
     */
    protected void saveValues() {
        T state = captureValues();
        if (state != null) {
            storageService.save(getViewKey(), state);
        }
    }
}