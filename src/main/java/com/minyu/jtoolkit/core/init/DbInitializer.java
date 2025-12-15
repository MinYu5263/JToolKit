package com.minyu.jtoolkit.core.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbInitializer {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {

        String createTableSql = """
                CREATE TABLE IF NOT EXISTS view_state (
                    view_key VARCHAR(255) PRIMARY KEY,
                    view_data TEXT,
                    updated_at TIMESTAMP DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime'))
                );
                """;
        jdbcTemplate.execute(createTableSql);

        // 创建触发器实现自动更新时间
        String createTriggerSql = """
                CREATE TRIGGER IF NOT EXISTS update_view_state_timestamp
                AFTER UPDATE ON view_state
                BEGIN
                    UPDATE view_state SET updated_at = datetime(CURRENT_TIMESTAMP, 'localtime') WHERE view_key = NEW.view_key;
                END;
                """;
        jdbcTemplate.execute(createTriggerSql);
        log.info("Database schema initialization completed.");
    }

}
