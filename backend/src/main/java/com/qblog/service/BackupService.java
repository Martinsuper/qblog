package com.qblog.service;

import com.qblog.model.dto.BackupSettingsDTO;
import com.qblog.model.vo.BackupVO;
import com.qblog.model.vo.BackupSettingsVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 备份服务接口
 */
public interface BackupService {

    /**
     * 创建备份
     * @param type 备份类型：database-仅数据库，full-完整备份
     * @param description 备份描述
     * @return 备份信息
     */
    BackupVO createBackup(String type, String description);

    /**
     * 获取备份列表
     * @return 备份列表
     */
    List<BackupVO> getBackupList();

    /**
     * 获取备份详情
     * @param backupId 备份 ID
     * @return 备份信息
     */
    BackupVO getBackupDetail(String backupId);

    /**
     * 下载备份文件
     * @param backupId 备份 ID
     * @return 备份文件字节数组
     * @throws IOException IO 异常
     */
    byte[] downloadBackup(String backupId) throws IOException;

    /**
     * 恢复备份
     * @param backupId 备份 ID
     */
    void restoreBackup(String backupId);

    /**
     * 删除备份
     * @param backupId 备份 ID
     */
    void deleteBackup(String backupId);

    /**
     * 导入备份文件
     * @param file 备份文件
     * @return 备份信息
     */
    BackupVO importBackup(MultipartFile file);

    /**
     * 获取备份设置
     * @return 备份设置
     */
    BackupSettingsVO getSettings();

    /**
     * 更新备份设置
     * @param settings 备份设置
     */
    void updateSettings(BackupSettingsDTO settings);

    /**
     * 执行定时备份
     */
    void scheduledBackup();

    /**
     * 清理过期备份
     */
    void cleanupOldBackups();
}
