package com.minyu.jtoolkit.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minyu.jtoolkit.system.entity.AppMenu;
import com.minyu.jtoolkit.system.mapper.MenuMapper;
import com.minyu.jtoolkit.system.service.MenuService;
import org.springframework.stereotype.Service;

/**
 * MenuServiceImpl
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, AppMenu> implements MenuService {
}
