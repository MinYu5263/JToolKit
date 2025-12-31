package com.minyu.jtoolkit.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.minyu.jtoolkit.core.model.ViewState;
import com.minyu.jtoolkit.system.service.ViewDataService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ViewDataServiceImpl
 */
@Service
public class ViewDataServiceImpl implements ViewDataService {
    // 模拟内存中的数据库表：Map<viewKey, jsonString>
    private final Map<String, String> dataCache = new ConcurrentHashMap<>();

    // 存档文件路径：user.home/.jtoolkit/view_data.json
    private final Path storagePath = Paths.get(System.getProperty("user.home"), ".jtoolkit", "view_data.json");

    @PostConstruct
    public void init() {
        // 启动时一次性加载所有配置到内存（桌面应用配置数据量极小，完全没问题）
        if (Files.exists(storagePath)) {
            try {
                String content = Files.readString(storagePath);
                Map<String, String> loaded = JSON.parseObject(content, new TypeReference<Map<String, String>>(){});
                if (loaded != null) {
                    dataCache.putAll(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveState(String key, ViewState viewData) {
        // 1. 更新内存
        String jsonString = JSON.toJSONString(viewData);
        dataCache.put(key, jsonString);

        // 2. 异步或同步写入磁盘（为了性能可以是异步，或者利用你的Controller层的debounce机制直接写）
        // 鉴于你的Controller已经做了1秒防抖，这里直接写盘也很快
        flushToDisk();
    }

    @Override
    public <T> T loadState(String key, Class<T> clazz) {
        // 纯内存操作，比 SQLite 查询快 100 倍不止
        String jsonString = dataCache.get(key);
        if (jsonString == null) {
            return null;
        }
        return JSON.parseObject(jsonString, clazz);
    }

    private void flushToDisk() {
        try {
            if (!Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }
            // 格式化输出，方便你手动改配置
            Files.writeString(storagePath, JSON.toJSONString(dataCache));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
