package com.minyu.jtoolkit.module.excel2sql;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.Data;

@Data
public class ExcelToSqlPersistentState implements PersistentState {
    private String lastFilePath;
    private String tableName;
    private boolean skipHeader;
    private String sqlMode;
}