package com.minyu.jtoolkit.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AppSetting
 */
@Data
@TableName("app_setting")
public class AppSetting {
    @TableId
    private String settingKey;

    private String settingValue;

    private String description;
}
