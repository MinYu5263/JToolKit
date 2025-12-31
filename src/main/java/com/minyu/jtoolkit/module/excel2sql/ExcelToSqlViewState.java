package com.minyu.jtoolkit.module.excel2sql;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.Data;

@Data
public class ExcelToSqlViewState implements ViewState {
    private String lastFilePath;
    private String tableName;
    private boolean skipHeader;
    private String sqlMode;
}