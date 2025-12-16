package com.minyu.jtoolkit.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minyu.jtoolkit.system.entity.AppSetting;
import com.minyu.jtoolkit.system.mapper.AppSettingMapper;
import com.minyu.jtoolkit.system.service.AppSettingService;
import org.springframework.stereotype.Service;

/**
 * AppSettingServiceImpl
 */
@Service
public class AppSettingServiceImpl extends ServiceImpl<AppSettingMapper, AppSetting> implements AppSettingService {
}
