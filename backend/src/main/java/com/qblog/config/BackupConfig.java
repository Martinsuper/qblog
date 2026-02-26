package com.qblog.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Properties;

/**
 * 备份配置
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "backup")
public class BackupConfig implements SchedulingConfigurer {

    /**
     * 定时备份 cron 表达式
     * 默认每天凌晨 2 点执行
     */
    private String cron = "0 0 2 * * ?";

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("备份任务配置加载，cron: {}", cron);
    }
}
