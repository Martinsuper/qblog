package com.qblog.model.dto;

import lombok.Data;

/**
 * 创建备份请求 DTO
 */
@Data
public class BackupCreateDTO {

    /**
     * 备份类型：database-仅数据库，full-完整备份（含文件）
     */
    private String type = "database";

    /**
     * 备份描述
     */
    private String description;
}
