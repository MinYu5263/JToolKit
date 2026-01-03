package com.minyu.jtoolkit.module.main.layout;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainModel {

    // 当前选中的页面 FXML 路径
    private final ReadOnlyObjectWrapper<String> selectedPage = new ReadOnlyObjectWrapper<>();
    // 导航树数据
    private final ReadOnlyObjectWrapper<TreeItem<Nav>> navTree = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyListWrapper<Nav> footerList = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    private final List<SearchResult> searchIndex = new ArrayList<>();

    private final SimpleStringProperty searchShortcutText = new SimpleStringProperty("");

    public ReadOnlyObjectProperty<String> selectedPageProperty() {
        return selectedPage.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<TreeItem<Nav>> navTreeProperty() {
        return navTree.getReadOnlyProperty();
    }

    public ReadOnlyListProperty<Nav> footerListProperty() {
        return footerList.getReadOnlyProperty();
    }

    public SimpleStringProperty searchShortcutTextProperty() {
        return searchShortcutText;
    }

    /**
     * 导航到指定页面
     *
     * @param fxmlPath FXML 文件路径
     */
    public void navigate(String fxmlPath) {
        selectedPage.set(Objects.requireNonNull(fxmlPath));
    }

    /**
     * 初始化菜单数据
     * 直接从 NavConfig 获取静态配置，构建 TreeItem 结构
     */
    public void initMenu() {
        TreeItem<Nav> sideRoot = createSideTree();
        this.navTree.set(sideRoot);

        ObservableList<Nav> footerRoot = createFooterList();
        this.footerList.set(footerRoot);

        refreshSearchIndex(sideRoot);
    }

    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        var results = new ArrayList<SearchResult>();
        var lowerQuery = query.toLowerCase();

        for (SearchResult item : searchIndex) {
            if (item.title().toLowerCase().contains(lowerQuery)) {
                results.add(item);
            }
        }
        return results;
    }

    private TreeItem<Nav> createSideTree() {
        var root = new TreeItem<>(Nav.ROOT);

        TreeItem<Nav> generators = group("生成器", Material2AL.DEVELOPER_BOARD);
        generators.getChildren().setAll(List.of(
                item("Cron 生成器", "fxml/cron/CronView.fxml"),
                item("文件树生成", "fxml/file_tree/FileTreeView.fxml"),
                item("密码生成器", "fxml/password/PasswordView.fxml")
        ));
        generators.setExpanded(true);

        TreeItem<Nav> converter = group("转换工具", Material2AL.FLIP_CAMERA_ANDROID);
        converter.getChildren().setAll(List.of(
                item("Excel转SQL", "fxml/excel2sql/ExcelToSqlView.fxml"),
                item("SQL转Ecel", "fxml/sql2excel/SqlToExcelView.fxml"),
                item("yaml与properties互转", "fxml/yaml_props/YamlPropsView.fxml"),
                item("数字进制", "fxml/radix/RadixView.fxml")
        ));
        converter.setExpanded(true);

        TreeItem<Nav> text = group("文本工具", Material2AL.DEVELOPER_BOARD);
        text.getChildren().setAll(List.of(
                item("JSON 格式化", "fxml/json/JsonView.fxml"),
                item("正则表达式测试", "fxml/regex/RegexView.fxml"),
                item("文本分析与实用工具", "fxml/text_analyzer/TextAnalyzerView.fxml")
        ));
        text.setExpanded(true);

        TreeItem<Nav> sysTools = group("系统工具", Material2AL.ACCOUNT_TREE);
        sysTools.getChildren().setAll(List.of(
                item("Git配置助手", "fxml/git_config/GitConfigView.fxml"),
                item("环境变量助手", "fxml/env_vars/EnvVarView.fxml")
        ));
        sysTools.setExpanded(true);

        root.getChildren().setAll(List.of(
                generators,
                converter,
                text,
                sysTools
        ));
        return root;
    }

    public ObservableList<Nav> createFooterList() {
        return FXCollections.observableArrayList(
                new Nav("设置", Material2MZ.SETTINGS, "fxml/settings/SettingsView.fxml"),
                new Nav("关于", Material2AL.INFO, "fxml/about/AboutView.fxml")
        );
    }

    private TreeItem<Nav> group(String title, Ikon ikon) {
        return new TreeItem<>(new Nav(title, ikon, null));
    }

    private TreeItem<Nav> item(String title, String fxmlPath) {
        return new TreeItem<>(new Nav(title, null, fxmlPath));
    }

    private void refreshSearchIndex(TreeItem<Nav> root) {
        searchIndex.clear();
        for (TreeItem<Nav> groupItem : root.getChildren()) {
            String groupTitle = groupItem.getValue().title();

            for (TreeItem<Nav> leafItem : groupItem.getChildren()) {
                Nav nav = leafItem.getValue();
                if (nav.fxmlPath() != null) {
                    searchIndex.add(new SearchResult(
                            nav.title(),
                            groupTitle,
                            nav.fxmlPath()
                    ));
                }
            }
        }
    }
}