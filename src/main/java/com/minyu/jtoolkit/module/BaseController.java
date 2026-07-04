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

public abstract class BaseController<T extends PersistentState> implements Initializable {
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
        initView();
        loadStateOrDefaults();
        registerAutoSave();
    }

    protected void initView() {

    }

    protected abstract void restoreValues(T state);

    protected void initDefaultValues() {
    }

    protected List<Observable> getObservables() {
        return List.of();
    }

    protected abstract T captureValues();

    protected abstract String getViewKey();

    protected Class<T> getStorageType() {
        return null;
    }

    protected void loadStateOrDefaults() {
        String key = getViewKey();
        T state = storageService.load(key, this.stateType);
        if (state != null) {
            restoreValues(state);
        } else {
            initDefaultValues();
        }
    }

    private void registerAutoSave() {
        List<Observable> observables = getObservables();
        if (observables != null && !observables.isEmpty()) {
            for (Observable obs : observables) {
                obs.addListener(observable -> autoSaveTimer.playFromStart());
            }
        }
    }

    protected void saveValues() {
        T state = captureValues();
        if (state != null) {
            storageService.save(getViewKey(), state);
        }
    }
}
