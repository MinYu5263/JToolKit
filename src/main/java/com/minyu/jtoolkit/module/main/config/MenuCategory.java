package com.minyu.jtoolkit.module.main.config;

import java.util.List;

/**
 * 一级菜单
 */
public record MenuCategory(String title, String iconLiteral, List<MenuPage> pages) {
}
