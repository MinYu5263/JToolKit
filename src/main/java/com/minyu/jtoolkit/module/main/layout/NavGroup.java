package com.minyu.jtoolkit.module.main.layout;

import java.util.List;

/**
 * 侧边栏一级分组
 */
public record NavGroup(String title, String iconLiteral, List<Nav> items) {
}
