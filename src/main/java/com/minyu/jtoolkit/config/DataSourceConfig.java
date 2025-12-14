package com.minyu.jtoolkit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.JDBC;

import javax.sql.DataSource;
import java.io.File;

/**
 * DataSourceConfig
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String userHome = System.getProperty("user.home");

        String appDir = userHome + File.separator + ".jtoolkit";
        String dbName = "data.db";

        File dir = new File(appDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Initialized data directory: {}", dir.getAbsolutePath());
            }
        }

        String dbUrl = "jdbc:sqlite:" + appDir + File.separator + dbName;

        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(JDBC.class.getName());
        dataSourceBuilder.url(dbUrl);

        return dataSourceBuilder.build();
    }
}
