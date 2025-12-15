package com.minyu.jtoolkit.module.main.config;

import java.util.List;

/**
 * MenuConfig
 */
public class MenuConfig {
    public static List<MenuCategory> getMenus() {
        return List.of(
                new MenuCategory("General", "mdal-grid_on", List.of(
                        new MenuPage("Theme", "fxml/theme/ThemeView.fxml")
                        // new MenuPage("Settings", "fxml/settings/SettingsView.fxml")
                )),
                new MenuCategory("生成器", "mdal-developer_board", List.of(
                        new MenuPage("Cron 生成器", "fxml/cron/CronView.fxml"),
                        new MenuPage("文件树生成", "fxml/file_tree/FileTreeView.fxml")
                )),
                new MenuCategory("转换工具", "mdal-flip_camera_android", List.of(
                        new MenuPage("Excel转SQL", "fxml/excel2sql/ExcelToSqlView.fxml"),
                        new MenuPage("SQL转Ecel", "fxml/file_tree/SqlToExcelView.fxml")
                )),

                new MenuCategory("文本工具", "mdmz-text_snippet", List.of(
                        new MenuPage("JSON 格式化", "fxml/json/JsonView.fxml"),
                        // new MenuPage("Base64", "fxml/dev/Base64View.fxml"),
                        new MenuPage("正则表达式测试", "fxml/regex/RegexView.fxml")
                )),

                 new MenuCategory("系统工具", "mdal-account_tree", List.of(
                        new MenuPage("环境变量助手", "fxml/env_vars/EnvVarView.fxml")
                ))
        );
    }
}
