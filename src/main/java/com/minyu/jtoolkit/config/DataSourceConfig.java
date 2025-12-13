package com.minyu.jtoolkit.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.File;

/**
 * DataSourceConfig
 */
@Configuration
public class DataSourceConfig {
    private final String dbPath = System.getProperty("user.dir") + "/jtoolkit_data.db";

    @Bean
    public DataSource dataSource() {
        // 1. 确保目录存在 (防报错)
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 2. 显式配置 DataSource
        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        System.out.println(">>> 正在初始化 SQLite DataSource, URL: " + jdbcUrl);

        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url(jdbcUrl)
                .build();
    }
}
