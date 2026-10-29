package com.qblog.config;

import com.qblog.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * 备份调度配置
 * 使用动态 CronTrigger：每次触发时按当前备份设置重新计算 cron，
 * 让设置页里的频率/时间真实生效（@Scheduled 注解只能静态解析，改设置不会生效）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BackupConfig implements SchedulingConfigurer {

    private final BackupService backupService;

    /** 设置缺失/非法时的回退值，可用 BACKUP_CRON 环境变量覆盖 */
    @Value("${backup.cron:0 0 2 * * ?}")
    private String fallbackCron;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            () -> backupService.scheduledBackup(),
            triggerContext -> {
                String cron;
                try {
                    cron = backupService.resolveCronExpression();
                } catch (Exception e) {
                    log.warn("读取备份定时设置失败，回退默认 cron", e);
                    cron = fallbackCron;
                }
                try {
                    return new CronTrigger(cron).nextExecution(triggerContext);
                } catch (IllegalArgumentException e) {
                    log.warn("备份设置中的 cron 表达式非法：{}，回退默认", cron);
                    return new CronTrigger(fallbackCron).nextExecution(triggerContext);
                }
            });
    }
}
