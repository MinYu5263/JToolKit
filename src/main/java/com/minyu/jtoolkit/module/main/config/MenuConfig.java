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
                ))

                /* new MenuCategory("Other", "feather-box", List.of(
                        new MenuPage("About", "fxml/about/AboutView.fxml")
                )) */
        );
    }
}
