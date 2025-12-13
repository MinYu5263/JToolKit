package com.minyu.jtoolkit.core.init;

import com.minyu.jtoolkit.system.entity.AppMenu;
import com.minyu.jtoolkit.system.mapper.MenuMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * DbInitializer
 */
@Component
@RequiredArgsConstructor
public class DbInitializer {
    private final JdbcTemplate jdbcTemplate;
    private final MenuMapper menuMapper;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @PostConstruct
    public void init() {

        String cleanPath = dbUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(cleanPath);
        File parentDir = dbFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
            System.out.println(">> 自动创建数据库目录: " + parentDir.getAbsolutePath());
        }
        String createTableSql = """
                    CREATE TABLE IF NOT EXISTS app_menu (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        parent_id INT DEFAULT NULL,
                        name VARCHAR(50) NOT NULL,
                        icon VARCHAR(50),
                        fxml_path VARCHAR(100),
                        sort_order INT DEFAULT 0
                    );
                """;
        jdbcTemplate.execute(createTableSql);

        if (menuMapper.selectCount(null) == 0) {
            initData(null, "General", "fth-grid", null, 1);
            initData(null, "Tools", "feather-tool", null, 2);
            initData(1, "Theme", null, "fxml/theme/ThemeView.fxml", 1);
            initData(2, "Json Parser", null, "fxml/json/JsonView.fxml", 1);
            System.out.println(">> 初始化默认菜单数据完成");
        }
    }

    private void initData(Integer parentId, String name, String icon, String path, int sort) {
        AppMenu menu = new AppMenu();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setIcon(icon);
        menu.setFxmlPath(path);
        menu.setSortOrder(sort);
        menuMapper.insert(menu);
    }
}
