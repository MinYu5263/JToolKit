package com.minyu.jtoolkit.module.sql2excel;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class SqlToExcelPersistentState implements PersistentState {

    // 保存所有配置过的连接信息
    private List<DbConnectionProfile> profiles = new ArrayList<>();

    // 上次选中的连接名（用于自动恢复）
    private String lastSelectedProfileName;

    // 上次选中的数据库（用于自动恢复）
    private String lastSelectedDatabase;

    // 主 SQL 文本框内容
    private String savedSqlContent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbConnectionProfile {
        private String name;        // 连接别名 (如 "生产库", "本地测试")
        private String dbType;      // MySQL, PostgreSQL
        private String host;
        private String port;
        private String username;
        private String password;

        // 重写 toString 让 ComboBox 直接显示名字
        @Override
        public String toString() {
            return name;
        }
    }
}
