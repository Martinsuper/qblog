package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.model.dto.BackupSettingsDTO;
import com.qblog.model.vo.BackupVO;
import com.qblog.model.vo.BackupSettingsVO;
import com.qblog.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 备份控制器
 */
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 创建备份
     */
    @PostMapping("/create")
    public Result<BackupVO> createBackup(
            @RequestParam(required = false, defaultValue = "database") String type,
            @RequestParam(required = false) String description) {
        BackupVO backup = backupService.createBackup(type, description);
        return Result.success(backup);
    }

    /**
     * 获取备份列表
     */
    @GetMapping("/list")
    public Result<List<BackupVO>> getBackupList() {
        return Result.success(backupService.getBackupList());
    }

    /**
     * 获取备份详情
     */
    @GetMapping("/{id}")
    public Result<BackupVO> getBackupDetail(@PathVariable String id) {
        return Result.success(backupService.getBackupDetail(id));
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadBackup(@PathVariable String id)
            throws java.io.IOException {
        byte[] data = backupService.downloadBackup(id);

        // 查找备份文件
        java.nio.file.Path backupPath = java.nio.file.Paths.get("backups");
        java.io.File sqlFile = backupPath.resolve(id + ".sql").toFile();
        java.io.File zipFile = backupPath.resolve(id + ".zip").toFile();

        String filename = java.net.URLEncoder.encode(
            sqlFile.exists() ? sqlFile.getName() : zipFile.getName(),
            StandardCharsets.UTF_8.toString()
        );
        MediaType contentType = sqlFile.exists()
            ? MediaType.parseMediaType("application/sql")
            : MediaType.parseMediaType("application/zip");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(data.length)
            .body(new ByteArrayResource(data));
    }

    /**
     * 恢复备份
     */
    @PostMapping("/restore/{id}")
    public Result<Void> restoreBackup(@PathVariable String id) {
        backupService.restoreBackup(id);
        return Result.success();
    }

    /**
     * 删除备份
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteBackup(@PathVariable String id) {
        backupService.deleteBackup(id);
        return Result.success();
    }

    /**
     * 导入备份文件
     */
    @PostMapping("/import")
    public Result<BackupVO> importBackup(@RequestParam("file") MultipartFile file) {
        BackupVO backup = backupService.importBackup(file);
        return Result.success(backup);
    }

    /**
     * 获取备份设置
     */
    @GetMapping("/settings")
    public Result<BackupSettingsVO> getSettings() {
        return Result.success(backupService.getSettings());
    }

    /**
     * 更新备份设置
     */
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody BackupSettingsDTO settings) {
        backupService.updateSettings(settings);
        return Result.success();
    }
}
