package com.minyu.jtoolkit.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 侧边导航栏
 */
@Data
@TableName("app_menu")
public class AppMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer parentId;

    private String name;
    private String icon;
    private String fxmlPath;
    private Integer sortOrder;

    public boolean isRoot() {
        return parentId == null;
    }
}
