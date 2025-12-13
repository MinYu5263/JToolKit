package com.minyu.jtoolkit.module.main.layout;

import com.minyu.jtoolkit.module.main.config.MenuCategory;
import com.minyu.jtoolkit.module.main.config.MenuConfig;
import com.minyu.jtoolkit.module.main.config.MenuPage;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainModel {

    // 当前选中的页面 FXML 路径
    private final ReadOnlyObjectWrapper<String> selectedPage = new ReadOnlyObjectWrapper<>();
    // 导航树数据
    private final ReadOnlyObjectWrapper<TreeItem<Nav>> navTree = new ReadOnlyObjectWrapper<>();

    public ReadOnlyObjectProperty<String> selectedPageProperty() {
        return selectedPage.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<TreeItem<Nav>> navTreeProperty() {
        return navTree.getReadOnlyProperty();
    }

    /**
     * 导航到指定页面
     * @param fxmlPath FXML 文件路径
     */
    public void navigate(String fxmlPath) {
        selectedPage.set(Objects.requireNonNull(fxmlPath));
    }

    /**
     * 初始化菜单数据
     * 直接从 MenuConfig 获取静态配置，构建 TreeItem 结构
     */
    public void initMenu() {
        // 创建隐形的根节点
        TreeItem<Nav> root = new TreeItem<>(Nav.ROOT);
        root.setExpanded(true);

        // 1. 获取静态配置
        List<MenuCategory> categories = MenuConfig.getMenus();

        // 2. 遍历构建树
        for (MenuCategory category : categories) {
            // --- 创建父节点 (一级菜单：分组) ---
            // 注意：这里直接传入 iconLiteral 字符串，不再创建 FontIcon 对象
            Nav groupNav = new Nav(
                    category.title(),
                    category.iconLiteral(),
                    null // 分组没有 FXML 路径
            );

            TreeItem<Nav> groupItem = new TreeItem<>(groupNav);
            groupItem.setExpanded(true);

            // --- 创建子节点 (二级菜单：页面) ---
            for (MenuPage page : category.pages()) {
                Nav pageNav = new Nav(
                        page.title(),
                        null, // 子菜单强制无图标
                        page.fxmlPath()
                );

                TreeItem<Nav> childItem = new TreeItem<>(pageNav);
                groupItem.getChildren().add(childItem);
            }

            root.getChildren().add(groupItem);
        }

        // 更新 Property，通知 View 进行渲染
        this.navTree.set(root);
    }

    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        var results = new ArrayList<SearchResult>();
        var lowerQuery = query.toLowerCase();

        // 遍历配置中的所有菜单
        for (MenuCategory category : MenuConfig.getMenus()) {
            for (MenuPage page : category.pages()) {
                // 简单的包含匹配（忽略大小写）
                if (page.title().toLowerCase().contains(lowerQuery)) {
                    results.add(new SearchResult(
                            page.title(),
                            category.title(),
                            page.fxmlPath()
                    ));
                }
            }
        }
        return results;
    }
}