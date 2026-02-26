package com.qblog.model.dto;

import lombok.Data;

/**
 * 备份设置 DTO
 */
@Data
public class BackupSettingsDTO {

    /**
     * 是否启用自动备份
     */
    private Boolean enabled = true;

    /**
     * 备份频率：hourly-每小时，daily-每天，weekly-每周
     */
    private String frequency = "daily";

    /**
     * 备份时间（小时），用于 daily 和 weekly 频率
     */
    private Integer hour = 2;

    /**
     * 备份时间（分钟），用于 daily 和 weekly 频率
     */
    private Integer minute = 0;

    /**
     * 每周备份的星期几，用于 weekly 频率 (0=周日，1-6=周一至周六)
     */
    private Integer dayOfWeek = 0;

    /**
     * 保留备份数量
     */
    private Integer keepCount = 7;

    /**
     * 备份类型：database-仅数据库，full-完整备份
     */
    private String backupType = "database";
}
