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
 * 视图控制器基类
 * <p>规范了 Controller 的生命周期：初始化视图 -> 加载数据(默认/历史) -> 注册监听</p>
 *
 * @param <T> 视图状态实体类 (ViewData)
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
        // 1. 初始化视图组件（设置 CellFactory, Converter, 初始显隐状态等）
        initView();

        // 2. 加载数据（根据是否有存档，分发到 restoreValues 或 initDefaultValues）
        loadStateOrDefaults();

        // 3. 注册自动保存监听
        registerAutoSave();
    }

    /**
     * 【步骤1】初始化视图属性
     * <p>子类实现：处理与数据无关的 UI 设置，如 TableView 的列绑定、ComboBox 的转换器等</p>
     */
    protected void initView() {

    }

    /**
     * 【步骤2-A】恢复历史值
     * <p>子类实现：将从数据库读取的 state 设置到 UI 组件中</p>
     *
     * @param state 历史保存的状态对象
     */
    protected abstract void restoreValues(T state);

    /**
     * 【步骤2-B】初始化默认值
     * <p>钩子方法：当没有历史存档时调用。子类按需重写（例如设置下拉框默认选中第一项）</p>
     */
    protected void initDefaultValues() {
    }

    /**
     * 【步骤3】获取需要自动保存的属性列表
     * <p>子类实现：返回需要监听的属性列表。如果不需要自动保存，返回 null 或空列表</p>
     *
     * @return 观察对象列表，如 List.of(textField.textProperty())
     */
    protected List<Observable> getObservables() {
        return List.of();
    }

    /**
     * 【核心功能】从 UI 提取值
     * <p>子类实现：将当前 UI 组件的值封装为 State 对象</p>
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
     * 获取视图状态在数据库中的唯一标识
     */
    protected abstract String getViewKey();

    /**
     * 获取状态类的Class对象，用于反序列化
     */
    protected abstract Class<T> getStorageType();

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
     * 内部流程：注册自动保存
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
