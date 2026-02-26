package com.qblog.model.vo;

import lombok.Data;

/**
 * 备份设置 VO
 */
@Data
public class BackupSettingsVO {

    /**
     * 是否启用自动备份
     */
    private Boolean enabled;

    /**
     * 备份频率：hourly-每小时，daily-每天，weekly-每周
     */
    private String frequency;

    /**
     * 备份时间（小时）
     */
    private Integer hour;

    /**
     * 备份时间（分钟）
     */
    private Integer minute;

    /**
     * 每周备份的星期几
     */
    private Integer dayOfWeek;

    /**
     * 保留备份数量
     */
    private Integer keepCount;

    /**
     * 备份类型
     */
    private String backupType;

    /**
     * 下次备份时间
     */
    private String nextBackupTime;

    /**
     * 当前 cron 表达式
     */
    private String cronExpression;
}
