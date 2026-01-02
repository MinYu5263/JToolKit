package com.minyu.jtoolkit.core.config;

import com.minyu.jtoolkit.system.service.EnvVarService;
import com.minyu.jtoolkit.system.service.impl.PosixEnvService;
import com.minyu.jtoolkit.system.service.impl.WindowsEnvService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EnvVarConfig
 */
@Configuration
public class EnvVarConfig {
    @Bean
    public EnvVarService envVarService() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsEnvService();
        } else {
            return new PosixEnvService();
        }
    }
}
