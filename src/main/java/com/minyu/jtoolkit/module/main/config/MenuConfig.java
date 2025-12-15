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

                new MenuCategory("Dev Tools", "mdmz-settings", List.of(
                        new MenuPage("JSON Parser", "fxml/json/JsonView.fxml")
                        // new MenuPage("Base64", "fxml/dev/Base64View.fxml"),
                        // new MenuPage("Regex Tester", "fxml/dev/RegexView.fxml")
                )),

                 new MenuCategory("系统工具", "mdal-account_tree", List.of(
                        new MenuPage("环境变量助手", "fxml/env_vars/EnvVarView.fxml")
                ))
        );
    }
}
