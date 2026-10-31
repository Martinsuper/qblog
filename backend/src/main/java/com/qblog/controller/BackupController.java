package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.model.dto.BackupSettingsDTO;
import com.qblog.model.vo.BackupTaskVO;
import com.qblog.model.vo.BackupVO;
import com.qblog.model.vo.BackupSettingsVO;
import com.qblog.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 备份控制器
 */
@Tag(name = "备份管理（需管理员）")
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 创建备份（异步任务，立即返回；用返回的 id 轮询任务状态接口）
     */
    @Operation(summary = "创建备份（异步）")
    @PostMapping("/create")
    public Result<BackupVO> createBackup(
            @RequestParam(required = false, defaultValue = "json") String type,
            @RequestParam(required = false) String description) {
        BackupVO backup = backupService.createBackup(type, description);
        return Result.success(backup);
    }

    /**
     * 查询备份任务状态
     */
    @Operation(summary = "查询备份任务状态")
    @GetMapping("/tasks/{id}")
    public Result<BackupTaskVO> getBackupTask(@PathVariable String id) {
        return Result.success(backupService.getBackupTask(id));
    }

    /**
     * 获取备份列表
     */
    @Operation(summary = "获取备份列表")
    @GetMapping("/list")
    public Result<List<BackupVO>> getBackupList() {
        return Result.success(backupService.getBackupList());
    }

    /**
     * 获取备份详情
     */
    @Operation(summary = "获取备份详情")
    @GetMapping("/{id}")
    public Result<BackupVO> getBackupDetail(@PathVariable String id) {
        return Result.success(backupService.getBackupDetail(id));
    }

    /**
     * 下载备份文件（流式输出，避免整包载入内存）
     */
    @Operation(summary = "下载备份文件")
    @GetMapping("/download/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadBackup(@PathVariable String id)
            throws java.io.IOException {
        org.springframework.core.io.Resource resource = backupService.downloadBackup(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition
                .attachment()
                .filename(resource.getFilename())
                .build()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(resource.contentLength())
            .body(resource);
    }

    /**
     * 恢复备份
     */
    @Operation(summary = "恢复备份")
    @PostMapping("/restore/{id}")
    public Result<Void> restoreBackup(@PathVariable String id) {
        backupService.restoreBackup(id);
        return Result.success();
    }

    /**
     * 删除备份
     */
    @Operation(summary = "删除备份")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteBackup(@PathVariable String id) {
        backupService.deleteBackup(id);
        return Result.success();
    }

    /**
     * 导入备份文件
     */
    @Operation(summary = "导入备份文件")
    @PostMapping("/import")
    public Result<BackupVO> importBackup(@RequestParam("file") MultipartFile file) {
        BackupVO backup = backupService.importBackup(file);
        return Result.success(backup);
    }

    /**
     * 获取备份设置
     */
    @Operation(summary = "获取备份设置")
    @GetMapping("/settings")
    public Result<BackupSettingsVO> getSettings() {
        return Result.success(backupService.getSettings());
    }

    /**
     * 更新备份设置
     */
    @Operation(summary = "更新备份设置")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody BackupSettingsDTO settings) {
        backupService.updateSettings(settings);
        return Result.success();
    }
}
