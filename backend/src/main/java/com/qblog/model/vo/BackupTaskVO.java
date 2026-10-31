package com.qblog.model.vo;

import lombok.Data;

/**
 * 备份任务状态 VO（创建备份为异步任务，前端轮询获取结果）
 */
@Data
public class BackupTaskVO {

    /**
     * 备份 ID
     */
    private String id;

    /**
     * 任务状态：RUNNING-执行中，DONE-完成，FAILED-失败
     */
    private String status;

    /**
     * 失败原因（仅 FAILED 时有值）
     */
    private String error;

    /**
     * 备份信息（仅 DONE 时有值）
     */
    private BackupVO backup;
}
