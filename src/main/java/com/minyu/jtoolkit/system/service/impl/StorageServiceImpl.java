package com.minyu.jtoolkit.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.minyu.jtoolkit.core.model.PersistentState;
import com.minyu.jtoolkit.system.service.StorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * StorageServiceImpl
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {
    private final Map<String, Object> dataCache = new ConcurrentHashMap<>();

    private final Path storagePath = Paths.get(System.getProperty("user.home"), ".jtoolkit", "app_data.json");

    // 单线程调度器，用于执行延时任务
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Storage-Writer-Thread");
        t.setDaemon(true); // 设置为守护线程
        return t;
    });

    private ScheduledFuture<?> pendingWriteTask;
    // 延迟保存时间
    private static final long WRITE_DELAY_MS = 1000;

    @PostConstruct
    public void init() {
        if (Files.exists(storagePath)) {
            try {
                String content = Files.readString(storagePath);
                JSONObject root = JSON.parseObject(content);
                if (root != null) {
                    dataCache.putAll(root);
                }
            } catch (IOException e) {
                log.error("Failed to load app data", e);
            }
        }
    }

    @Override
    public void save(String key, PersistentState state) {
        if (state == null) {
            dataCache.remove(key);
        } else {
            dataCache.put(key, state);
        }

        scheduleWrite();
    }

    @Override
    public <T extends PersistentState> T load(String key, Class<T> clazz) {
        Object obj = dataCache.get(key);
        if (obj == null) {
            return null;
        }
        return JSON.to(clazz, obj);
    }

    @Override
    public void clearAll() {
        dataCache.clear();
        log.warn("All application data has been cleared.");
        scheduleWrite();
    }

    @Override
    public void clearExclude(String... retainedKeys) {
        List<String> keepList = Arrays.asList(retainedKeys);

        dataCache.keySet().removeIf(key -> !keepList.contains(key));

        log.info("Application data cleared (excluding: {}).", keepList);
        scheduleWrite();
    }

    @Override
    public Path getBaseDirectory() {
        return storagePath.getParent();
    }

    /**
     * 防抖调度逻辑
     */
    private synchronized void scheduleWrite() {
        if (pendingWriteTask != null && !pendingWriteTask.isDone()) {
            pendingWriteTask.cancel(false);
        }

        pendingWriteTask = scheduler.schedule(this::flushToDisk, WRITE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 实际写盘逻辑 (运行在后台线程)
     */
    private void flushToDisk() {
        try {
            if (!Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }

            String prettyJson = JSON.toJSONString(dataCache, JSONWriter.Feature.PrettyFormatWith4Space);
            Files.writeString(storagePath, prettyJson);

            // log.debug("Data flushed to disk.");
        } catch (IOException e) {
            log.error("Failed to save app data", e);
        }
    }

    /**
     * 应用关闭时强制写入数据
     */
    @PreDestroy
    public void destroy() {
        log.info("Application shutting down, flushing data to disk...");
        scheduler.shutdown();
        flushToDisk();
    }
}
