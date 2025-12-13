
package com.minyu.jtoolkit.module.main.layout;

import org.jspecify.annotations.Nullable;

record Nav(String title,
           @Nullable String iconLiteral,
           @Nullable String fxmlPath) {

    public static final Nav ROOT = new Nav("ROOT", null, null);

    public boolean isGroup() {
        return fxmlPath == null;
    }
}
