package com.minyu.jtoolkit.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ViewState
 */
@Data
@TableName("view_state")
public class ViewState {
    @TableId
    private String viewKey;
    private String viewData;
    private LocalDateTime updated_at;
}
