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

    private String lastPath;
    private List<HistoryItem> history = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String path;
        private String name;

        private String style;
        private int maxDepth;
        private boolean directoriesOnly;
        private String ignorePattern;

        @Override
        public String toString() {
            return name;
        }
    }
}