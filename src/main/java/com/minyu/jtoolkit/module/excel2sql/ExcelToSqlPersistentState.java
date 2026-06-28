package com.minyu.jtoolkit.module.excel2sql;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExcelToSqlPersistentState implements PersistentState {
    private String lastFilePath;
    private String tableName;
    private boolean skipHeader;
    private String sqlMode;
    /** 数据库类型 */
    private String dbDialect;
    /** 每批插入条数 */
    private String batchSize;
    /** 是否启用"与 Sheet 同名" */
    private boolean syncTableName = true;
    /** 最近打开的文件路径列表 */
    private List<String> recentFiles = new ArrayList<>();
    /** 列映射持久化数据（通过表头名称匹配恢复） */
    private List<ExcelToSqlController.ColumnMappingData> columnMappings = new ArrayList<>();
}
