package com.qblog.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备份信息 VO
 */
@Data
public class BackupVO {

    /**
     * 备份 ID（文件名）
     */
    private String id;

    /**
     * 备份文件名
     */
    private String filename;

    /**
     * 备份类型：database-仅数据库，full-完整备份
     */
    private String type;

    /**
     * 备份大小（字节）
     */
    private Long size;

    /**
     * 备份大小（格式化后）
     */
    private String formattedSize;

    /**
     * 备份描述
     */
    private String description;

    /**
     * 备份创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 是否正在恢复中
     */
    private Boolean restoring = false;
}
