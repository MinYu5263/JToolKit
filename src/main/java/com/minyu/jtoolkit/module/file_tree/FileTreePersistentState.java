package com.minyu.jtoolkit.module.file_tree;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTreePersistentState implements PersistentState {

    // 历史记录列表
    private List<HistoryItem> history = new ArrayList<>();

    // 上次生成的配置状态
    private String lastPath;
    private int maxDepth;           // -1 表示无限制
    private boolean directoriesOnly;
    private String ignorePattern;   // 忽略规则，逗号分隔
    private String style;           // ASCII, EMOJI, MARKDOWN

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String path;   // 真实路径
        private String name;   // 显示别名 (默认是文件夹名，可重命名)

        // 重写 toString 用于 ListView 显示
        @Override
        public String toString() {
            return name;
        }
    }
}