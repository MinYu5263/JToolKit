package com.minyu.jtoolkit.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ViewData
 */
@Data
@TableName("view_data")
public class ViewData {
    @TableId
    private String viewKey;
    private String viewState;
    private LocalDateTime updated_at;
}
