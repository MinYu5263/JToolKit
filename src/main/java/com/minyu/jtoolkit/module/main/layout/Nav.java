
package com.minyu.jtoolkit.module.main.layout;

import org.kordamp.ikonli.Ikon;

public record Nav(String title, Ikon icon, String fxmlPath) {

    public static final Nav ROOT = new Nav("ROOT", null, null);

    public boolean isGroup() {
        return fxmlPath == null;
    }
}
