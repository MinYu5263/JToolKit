package com.minyu.jtoolkit.module.excel2sql;

import lombok.Data;

@Data
public class ExcelToSqlViewState {
    private String lastFilePath;
    private String tableName;
    private boolean skipHeader;
    private String sqlMode;
}