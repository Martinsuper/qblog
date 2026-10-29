package com.qblog.service.impl;

import com.qblog.mapper.ArticleMapper;
import com.qblog.mapper.ArticleTagMapper;
import com.qblog.mapper.CategoryMapper;
import com.qblog.mapper.TagMapper;
import com.qblog.mapper.UserMapper;
import com.qblog.model.dto.BackupSettingsDTO;
import com.qblog.model.vo.BackupVO;
import com.qblog.model.vo.BackupSettingsVO;
import com.qblog.service.BackupService;
import com.qblog.service.CacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.qblog.util.JsonBackupExporter;
import com.qblog.util.JsonBackupImporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Properties;

/**
 * 备份服务实现
 * 使用 JSON 格式进行数据备份和恢复
 */
@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    /**
     * 备份文件存储目录
     */
    private static final String BACKUP_DIR = "backups";

    /**
     * 备份设置文件
     */
    private static final String SETTINGS_FILE = "backup-settings.properties";

    /**
     * 备份文件前缀
     */
    private static final String BACKUP_PREFIX = "qblog-backup-";

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;

    @Value("${backup.directory:backups}")
    private String backupDirectory = "backups";

    @Autowired
    private CacheService cacheService;

    private Path backupDirPath;
    private Path settingsFilePath;

    @Autowired
    public BackupServiceImpl(UserMapper userMapper,
                             CategoryMapper categoryMapper,
                             TagMapper tagMapper,
                             ArticleMapper articleMapper,
                             ArticleTagMapper articleTagMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
    }

    @PostConstruct
    public void init() {
        // 初始化备份目录
        backupDirPath = Paths.get(backupDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(backupDirPath);
            // 实际探测写入权限；启动时失败而不是直到需要恢复数据才发现备份失效。
            Path probe = Files.createTempFile(backupDirPath, ".writable-", ".tmp");
            Files.delete(probe);
        } catch (IOException e) {
            throw new IllegalStateException("备份目录不可写：" + backupDirPath, e);
        }

        // 初始化设置文件路径
        settingsFilePath = backupDirPath.resolve(SETTINGS_FILE);
        if (!Files.exists(settingsFilePath)) {
            createDefaultSettings();
        }
    }

    /**
     * 创建默认设置
     */
    private void createDefaultSettings() {
        Properties props = new Properties();
        props.setProperty("enabled", "true");
        props.setProperty("frequency", "daily");
        props.setProperty("hour", "2");
        props.setProperty("minute", "0");
        props.setProperty("dayOfWeek", "0");
        props.setProperty("keepCount", "7");
        props.setProperty("backupType", "json");

        try (OutputStream os = Files.newOutputStream(settingsFilePath)) {
            props.store(os, "Backup Settings");
            log.info("创建默认备份设置");
        } catch (IOException e) {
            log.error("创建默认备份设置失败", e);
        }
    }

    @Override
    public BackupVO createBackup(String type, String description) {
        log.info("开始创建 JSON 备份，描述：{}", description);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String zipFilename = BACKUP_PREFIX + timestamp + ".zip";
        Path zipPath = backupDirPath.resolve(zipFilename);

        try {
            // 使用 JSON 导出器创建备份
            JsonBackupExporter exporter = new JsonBackupExporter(
                userMapper, categoryMapper, tagMapper, articleMapper, articleTagMapper
            );
            exporter.exportBackup(zipPath, description);

            // 创建元数据文件
            writeMetadata(BACKUP_PREFIX + timestamp, description, zipPath);

            log.info("JSON 备份完成：{}", zipFilename);

            // 清理过期备份
            cleanupOldBackups();

            return getBackupDetail(BACKUP_PREFIX + timestamp);
        } catch (IOException e) {
            log.error("创建备份失败", e);
            throw new RuntimeException("创建备份失败：" + e.getMessage(), e);
        }
    }

    /**
     * 写入元数据文件
     */
    private void writeMetadata(String baseFilename, String description, Path backupFile) throws IOException {
        Properties meta = new Properties();
        meta.setProperty("type", "json");
        meta.setProperty("description", description != null ? description : "");
        meta.setProperty("filename", backupFile.getFileName().toString());
        meta.setProperty("size", String.valueOf(Files.size(backupFile)));
        meta.setProperty("createTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Path metaPath = backupDirPath.resolve(baseFilename + ".meta");
        try (OutputStream os = Files.newOutputStream(metaPath)) {
            meta.store(os, "Backup Metadata");
        }
    }

    @Override
    public List<BackupVO> getBackupList() {
        log.debug("获取备份列表");

        List<BackupVO> backups = new ArrayList<>();

        try (var paths = Files.list(backupDirPath)) {
            paths
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    return filename.endsWith(".zip") && filename.startsWith(BACKUP_PREFIX);
                })
                .forEach(path -> {
                    try {
                        String filename = path.getFileName().toString();
                        String backupId = filename.substring(0, filename.length() - 4);

                        BackupVO vo = new BackupVO();
                        vo.setId(backupId);
                        vo.setFilename(filename);
                        vo.setSize(Files.size(path));
                        vo.setFormattedSize(formatFileSize(Files.size(path)));
                        vo.setType("json");
                        vo.setCreateTime(parseCreateTimeFromBackupId(backupId));

                        // 读取元数据
                        Path metaPath = resolveBackupPath(backupId, ".meta");
                        if (Files.exists(metaPath)) {
                            Properties meta = new Properties();
                            try (InputStream is = Files.newInputStream(metaPath)) {
                                meta.load(is);
                            }
                            vo.setDescription(meta.getProperty("description", ""));
                        }

                        backups.add(vo);
                    } catch (IOException e) {
                        log.error("读取备份文件信息失败：{}", path, e);
                    }
                });

            // 按创建时间倒序排序
            backups.sort(Comparator.comparing(BackupVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        } catch (IOException e) {
            log.error("获取备份列表失败", e);
        }

        return backups;
    }

    /**
     * 从备份 ID 解析创建时间
     */
    private LocalDateTime parseCreateTimeFromBackupId(String backupId) {
        try {
            // 格式: qblog-backup-20260227-210546
            // 拆分: [qblog, backup, 20260227, 210546]
            String[] parts = backupId.split("-");
            if (parts.length >= 4) {
                String datePart = parts[2]; // 20260227
                String timePart = parts[3]; // 210546

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String dateTimeStr = datePart + timePart;
                return LocalDateTime.parse(dateTimeStr, formatter);
            }
        } catch (Exception e) {
            log.warn("解析备份时间失败：{}", backupId, e);
        }
        return null;
    }

    @Override
    public BackupVO getBackupDetail(String backupId) {
        log.debug("获取备份详情：{}", backupId);

        try {
            Path backupPath = resolveBackupPath(backupId, ".zip");

            if (!Files.exists(backupPath)) {
                throw new RuntimeException("备份文件不存在：" + backupId);
            }

            String filename = backupPath.getFileName().toString();
            BackupVO vo = new BackupVO();
            vo.setId(backupId);
            vo.setFilename(filename);
            vo.setSize(Files.size(backupPath));
            vo.setFormattedSize(formatFileSize(Files.size(backupPath)));
            vo.setType("json");
            vo.setCreateTime(parseCreateTimeFromBackupId(backupId));

            // 读取元数据
            Path metaPath = resolveBackupPath(backupId, ".meta");
            if (Files.exists(metaPath)) {
                Properties meta = new Properties();
                try (InputStream is = Files.newInputStream(metaPath)) {
                    meta.load(is);
                }
                vo.setDescription(meta.getProperty("description", ""));
            }

            return vo;
        } catch (IOException e) {
            log.error("获取备份详情失败：{}", backupId, e);
            throw new RuntimeException("获取备份详情失败：" + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadBackup(String backupId) throws IOException {
        log.info("下载备份：{}", backupId);

        Path backupPath = resolveBackupPath(backupId, ".zip");

        if (!Files.exists(backupPath)) {
            throw new RuntimeException("备份文件不存在：" + backupId);
        }

        return Files.readAllBytes(backupPath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreBackup(String backupId) {
        log.info("开始恢复备份：{}", backupId);

        try {
            // 获取备份详情
            BackupVO backup = getBackupDetail(backupId);
            Path backupPath = backupDirPath.resolve(backup.getFilename());

            if (!Files.exists(backupPath)) {
                throw new RuntimeException("备份文件不存在：" + backupId);
            }

            // 使用 JSON 导入器恢复数据
            JsonBackupImporter importer = new JsonBackupImporter(
                userMapper, categoryMapper, tagMapper, articleMapper, articleTagMapper
            );
            Map<String, Integer> counts = importer.importBackup(backupPath);
            invalidateRestoredCaches();

            log.info("备份恢复完成：{}, 导入记录数：{}", backupId, counts);
        } catch (IOException e) {
            log.error("恢复备份失败：{}", backupId, e);
            throw new RuntimeException("恢复备份失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBackup(String backupId) {
        log.info("删除备份：{}", backupId);

        try {
            // 删除 ZIP 文件
            Path zipPath = resolveBackupPath(backupId, ".zip");
            Files.deleteIfExists(zipPath);

            // 删除元数据文件
            Path metaPath = resolveBackupPath(backupId, ".meta");
            Files.deleteIfExists(metaPath);

            log.info("备份删除完成：{}", backupId);
        } catch (IOException e) {
            log.error("删除备份失败：{}", backupId, e);
            throw new RuntimeException("删除备份失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackupVO importBackup(MultipartFile file) {
        log.info("导入备份文件：{}", file.getOriginalFilename());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 验证文件扩展名 - 仅支持 .zip (JSON 备份格式)
        if (!originalFilename.endsWith(".zip")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .zip 格式的 JSON 备份文件");
        }

        // 验证文件大小（最大 100MB）
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小超过限制（100MB）");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String filename = BACKUP_PREFIX + timestamp + ".zip";

        Path targetPath = backupDirPath.resolve(filename);

        try {
            file.transferTo(targetPath);

            // 验证是否为有效的 JSON 备份文件
            if (!isValidJsonBackup(targetPath)) {
                Files.deleteIfExists(targetPath);
                throw new IllegalArgumentException("无效的 JSON 备份文件格式");
            }

            // 写入元数据
            Properties meta = new Properties();
            meta.setProperty("type", "json");
            meta.setProperty("description", "导入的备份");
            meta.setProperty("originalFilename", originalFilename);
            meta.setProperty("importTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            Path metaPath = backupDirPath.resolve(BACKUP_PREFIX + timestamp + ".meta");
            try (OutputStream os = Files.newOutputStream(metaPath)) {
                meta.store(os, "Backup Metadata");
            }

            // 立即恢复数据到数据库
            log.info("开始从导入的备份恢复数据...");
            JsonBackupImporter importer = new JsonBackupImporter(
                userMapper, categoryMapper, tagMapper, articleMapper, articleTagMapper
            );
            Map<String, Integer> counts = importer.importBackup(targetPath);
            invalidateRestoredCaches();
            log.info("备份导入并恢复完成：{}, 恢复记录数：{}", filename, counts);

            return getBackupDetail(BACKUP_PREFIX + timestamp);
        } catch (IOException e) {
            log.error("导入备份失败", e);
            throw new RuntimeException("导入备份失败：" + e.getMessage(), e);
        }
    }

    /**
     * 验证是否为有效的 JSON 备份文件
     */
    private boolean isValidJsonBackup(Path zipPath) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipPath.toFile())) {
            Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            java.util.zip.ZipEntry entry;
            boolean hasManifest = false;
            boolean hasDataDir = false;

            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                String name = entry.getName();
                if ("manifest.json".equals(name)) {
                    hasManifest = true;
                }
                if (name.startsWith("data/") && name.endsWith(".json")) {
                    hasDataDir = true;
                }
            }

            return hasManifest && hasDataDir;
        } catch (IOException e) {
            log.warn("验证备份文件格式失败", e);
            return false;
        }
    }

    @Override
    public BackupSettingsVO getSettings() {
        log.debug("获取备份设置");

        Properties props = loadSettings();
        BackupSettingsVO vo = new BackupSettingsVO();

        vo.setEnabled(Boolean.parseBoolean(props.getProperty("enabled", "true")));
        vo.setFrequency(props.getProperty("frequency", "daily"));
        vo.setHour(Integer.parseInt(props.getProperty("hour", "2")));
        vo.setMinute(Integer.parseInt(props.getProperty("minute", "0")));
        vo.setDayOfWeek(Integer.parseInt(props.getProperty("dayOfWeek", "0")));
        vo.setKeepCount(Integer.parseInt(props.getProperty("keepCount", "7")));
        vo.setBackupType(props.getProperty("backupType", "json"));

        // 计算 cron 表达式
        vo.setCronExpression(calculateCronExpression(vo));

        return vo;
    }

    @Override
    public void updateSettings(BackupSettingsDTO settings) {
        log.info("更新备份设置");

        Properties props = loadSettings();

        if (settings.getEnabled() != null) {
            props.setProperty("enabled", settings.getEnabled().toString());
        }
        if (settings.getFrequency() != null) {
            props.setProperty("frequency", settings.getFrequency());
        }
        if (settings.getHour() != null) {
            props.setProperty("hour", settings.getHour().toString());
        }
        if (settings.getMinute() != null) {
            props.setProperty("minute", settings.getMinute().toString());
        }
        if (settings.getDayOfWeek() != null) {
            props.setProperty("dayOfWeek", settings.getDayOfWeek().toString());
        }
        if (settings.getKeepCount() != null) {
            props.setProperty("keepCount", settings.getKeepCount().toString());
        }
        if (settings.getBackupType() != null) {
            props.setProperty("backupType", settings.getBackupType());
        }

        saveSettings(props);
        log.info("备份设置更新完成");
    }

    /**
     * 加载设置
     */
    private Properties loadSettings() {
        Properties props = new Properties();
        if (Files.exists(settingsFilePath)) {
            try (InputStream is = Files.newInputStream(settingsFilePath)) {
                props.load(is);
            } catch (IOException e) {
                log.error("加载备份设置失败", e);
            }
        }
        return props;
    }

    /**
     * 保存设置
     */
    private void saveSettings(Properties props) {
        try (OutputStream os = Files.newOutputStream(settingsFilePath)) {
            props.store(os, "Backup Settings");
        } catch (IOException e) {
            log.error("保存备份设置失败", e);
        }
    }

    /**
     * 计算 cron 表达式
     */
    private String calculateCronExpression(BackupSettingsVO settings) {
        String frequency = settings.getFrequency();
        int minute = settings.getMinute();
        int hour = settings.getHour();

        return switch (frequency) {
            case "hourly" -> String.format("0 %d * * * ?", minute);
            case "daily" -> String.format("0 %d %d * * ?", minute, hour);
            case "weekly" -> String.format("0 %d %d ? * %d", minute, hour, settings.getDayOfWeek());
            default -> "0 0 2 * * ?"; // 默认每天凌晨 2 点
        };
    }

    /**
     * 定时备份任务
     */
    @Scheduled(cron = "${backup.cron:0 0 2 * * ?}")
    @Override
    public void scheduledBackup() {
        Properties props = loadSettings();
        boolean enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));

        if (!enabled) {
            log.debug("自动备份已禁用，跳过");
            return;
        }

        log.info("执行定时备份");
        createBackup("json", "定时自动备份");
    }

    @Override
    public void cleanupOldBackups() {
        Properties props = loadSettings();
        int keepCount = Integer.parseInt(props.getProperty("keepCount", "7"));

        log.info("清理过期备份，保留数量：{}", keepCount);

        try (var paths = Files.list(backupDirPath)) {
            List<Path> backups = paths
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    return filename.endsWith(".zip") && filename.startsWith(BACKUP_PREFIX);
                })
                .sorted((p1, p2) -> {
                    try {
                        long t1 = Files.getLastModifiedTime(p1).toMillis();
                        long t2 = Files.getLastModifiedTime(p2).toMillis();
                        return Long.compare(t2, t1); // 倒序排序
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .toList();

            // 删除超出数量的备份
            if (backups.size() > keepCount) {
                for (int i = keepCount; i < backups.size(); i++) {
                    Path path = backups.get(i);
                    String filename = path.getFileName().toString();
                    String backupId = filename.substring(0, filename.lastIndexOf('.'));

                    log.info("删除过期备份：{}", filename);
                    deleteBackup(backupId);
                }
            }
        } catch (IOException e) {
            log.error("清理过期备份失败", e);
        }
    }

    private Path resolveBackupPath(String backupId, String extension) {
        if (backupId == null || !backupId.matches("qblog-backup-[0-9]{8}-[0-9]{6}(?:-[0-9]{3}-[a-f0-9]{8})?")) {
            throw new IllegalArgumentException("非法备份 ID");
        }
        Path path = backupDirPath.resolve(backupId + extension).normalize();
        if (!path.startsWith(backupDirPath) || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("非法备份路径");
        }
        return path;
    }

    private void invalidateRestoredCaches() {
        Runnable invalidate = () -> {
            // 不删除 article:views:* 计数器。
            cacheService.deleteByPattern("article:detail:*");
            cacheService.deleteByPattern("article:list:*");
            cacheService.deleteByPattern("article:hot*");
            cacheService.deleteByPattern("article:latest*");
            cacheService.deleteByPattern("category:*");
            cacheService.deleteByPattern("tag:*");
            cacheService.deleteByPattern("user:*");
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { invalidate.run(); }
            });
        } else {
            invalidate.run();
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}