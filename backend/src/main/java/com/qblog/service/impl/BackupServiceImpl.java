package com.qblog.service.impl;

import com.qblog.model.dto.BackupSettingsDTO;
import com.qblog.model.vo.BackupVO;
import com.qblog.model.vo.BackupSettingsVO;
import com.qblog.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 备份服务实现
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
     * 数据库备份前缀
     */
    private static final String DB_BACKUP_PREFIX = "qblog-db-";

    /**
     * 完整备份前缀
     */
    private static final String FULL_BACKUP_PREFIX = "qblog-full-";

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    private Path backupDirPath;
    private Path settingsFilePath;

    @PostConstruct
    public void init() {
        // 初始化备份目录
        backupDirPath = Paths.get(BACKUP_DIR);
        if (!Files.exists(backupDirPath)) {
            try {
                Files.createDirectories(backupDirPath);
                log.info("创建备份目录：{}", BACKUP_DIR);
            } catch (IOException e) {
                log.error("创建备份目录失败", e);
            }
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
        props.setProperty("backupType", "database");

        try (OutputStream os = Files.newOutputStream(settingsFilePath)) {
            props.store(os, "Backup Settings");
            log.info("创建默认备份设置");
        } catch (IOException e) {
            log.error("创建默认备份设置失败", e);
        }
    }

    @Override
    public BackupVO createBackup(String type, String description) {
        log.info("开始创建备份，类型：{}, 描述：{}", type, description);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String prefix = "database".equals(type) ? DB_BACKUP_PREFIX : FULL_BACKUP_PREFIX;
        String baseFilename = prefix + timestamp;

        try {
            if ("database".equals(type)) {
                // 创建数据库备份
                String sqlFilename = baseFilename + ".sql";
                Path sqlPath = backupDirPath.resolve(sqlFilename);

                backupDatabase(sqlPath);

                // 创建元数据文件
                writeMetadata(baseFilename, type, description, sqlPath);

                log.info("数据库备份完成：{}", sqlFilename);
            } else if ("full".equals(type)) {
                // 创建完整备份（ZIP）
                String zipFilename = baseFilename + ".zip";
                Path zipPath = backupDirPath.resolve(zipFilename);

                createFullBackup(zipPath, description);

                log.info("完整备份完成：{}", zipFilename);
            } else {
                throw new IllegalArgumentException("不支持的备份类型：" + type);
            }

            // 清理过期备份
            cleanupOldBackups();

            return getBackupDetail(baseFilename);
        } catch (IOException e) {
            log.error("创建备份失败", e);
            throw new RuntimeException("创建备份失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("创建备份被中断", e);
            throw new RuntimeException("创建备份被中断", e);
        }
    }

    /**
     * 备份数据库
     */
    private void backupDatabase(Path outputPath) throws IOException, InterruptedException {
        // 检查 mysqldump 是否可用
        if (!isMysqldumpAvailable()) {
            throw new RuntimeException("mysqldump 命令不可用，请安装 MySQL 客户端工具");
        }

        // 从 JDBC URL 中提取数据库名
        String dbName = extractDatabaseName(databaseUrl);

        // 构建 mysqldump 命令
        List<String> command = new ArrayList<>();
        command.add("mysqldump");
        command.add("--host=localhost");
        command.add("--port=3306");
        command.add("--user=" + databaseUsername);
        command.add("--password=" + databasePassword);
        command.add("--default-character-set=utf8mb4");
        command.add("--set-gtid-purged=OFF");
        command.add("--single-transaction");
        command.add("--quick");
        command.add("--lock-tables=false");
        command.add(dbName);

        log.debug("执行命令：{}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             InputStream is = process.getInputStream();
             InputStream es = process.getErrorStream()) {

            // 读取输出
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            // 等待进程完成
            boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("mysqldump 执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorMessage = new String(es.readAllBytes());
                throw new RuntimeException("mysqldump 执行失败：" + errorMessage);
            }
        }

        // 检查文件是否生成
        if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
            throw new RuntimeException("备份文件未生成或为空");
        }
    }

    /**
     * 检查 mysqldump 是否可用
     */
    private boolean isMysqldumpAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "mysqldump");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("检查 mysqldump 可用性失败", e);
            return false;
        }
    }

    /**
     * 从 JDBC URL 中提取数据库名
     */
    private String extractDatabaseName(String jdbcUrl) {
        // jdbc:mysql://localhost:3306/qblog?...
        int lastSlash = jdbcUrl.lastIndexOf('/');
        int questionMark = jdbcUrl.indexOf('?');
        if (questionMark == -1) {
            questionMark = jdbcUrl.length();
        }
        return jdbcUrl.substring(lastSlash + 1, questionMark);
    }

    /**
     * 创建完整备份（包含数据库和文件）
     */
    private void createFullBackup(Path zipPath, String description) throws IOException, InterruptedException {
        // 先创建数据库备份（临时文件）
        Path tempSqlPath = Files.createTempFile("qblog-db-", ".sql");
        try {
            backupDatabase(tempSqlPath);

            // 创建 ZIP 文件
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                // 添加数据库文件
                addToZip(zos, tempSqlPath.toFile(), "database/" + tempSqlPath.getFileName());

                // 添加上传文件目录（如果存在）
                Path uploadsPath = Paths.get("uploads");
                if (Files.exists(uploadsPath)) {
                    addDirectoryToZip(zos, uploadsPath, "files/");
                }

                // 添加元数据
                ZipEntry metaEntry = new ZipEntry("metadata.json");
                zos.putNextEntry(metaEntry);
                String metadata = createMetadataJson(description);
                zos.write(metadata.getBytes());
                zos.closeEntry();
            }
        } finally {
            Files.deleteIfExists(tempSqlPath);
        }
    }

    /**
     * 添加文件到 ZIP
     */
    private void addToZip(ZipOutputStream zos, File file, String pathInZip) throws IOException {
        ZipEntry entry = new ZipEntry(pathInZip);
        zos.putNextEntry(entry);

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(zos);
        }

        zos.closeEntry();
    }

    /**
     * 添加目录到 ZIP
     */
    private void addDirectoryToZip(ZipOutputStream zos, Path directory, String pathInZip) throws IOException {
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                try {
                    String relativePath = pathInZip + directory.relativize(path).toString().replace('\\', '/');
                    addToZip(zos, path.toFile(), relativePath);
                } catch (IOException e) {
                    log.error("添加文件到 ZIP 失败：{}", path, e);
                }
            });
    }

    /**
     * 创建元数据 JSON
     */
    private String createMetadataJson(String description) {
        return String.format(
            "{\"version\":\"1.0\",\"timestamp\":\"%s\",\"description\":\"%s\",\"database\":\"qblog\"}",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            description != null ? description : ""
        );
    }

    /**
     * 写入元数据文件
     */
    private void writeMetadata(String baseFilename, String type, String description, Path backupFile) throws IOException {
        Properties meta = new Properties();
        meta.setProperty("type", type);
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

        try {
            Files.list(backupDirPath)
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    return filename.endsWith(".sql") || filename.endsWith(".zip");
                })
                .forEach(path -> {
                    try {
                        String filename = path.getFileName().toString();
                        String backupId = filename.endsWith(".sql")
                            ? filename.substring(0, filename.length() - 4)
                            : filename.substring(0, filename.length() - 4);

                        // 跳过元数据文件
                        if (backupId.endsWith(".meta")) {
                            return;
                        }

                        BackupVO vo = new BackupVO();
                        vo.setId(backupId);
                        vo.setFilename(filename);
                        vo.setSize(Files.size(path));
                        vo.setFormattedSize(formatFileSize(Files.size(path)));
                        vo.setType(filename.endsWith(".sql") ? "database" : "full");
                        vo.setCreateTime(LocalDateTime.of(
                            Integer.parseInt(backupId.substring(10, 14)),
                            Integer.parseInt(backupId.substring(14, 16)),
                            Integer.parseInt(backupId.substring(16, 18)),
                            Integer.parseInt(backupId.substring(19, 21)),
                            Integer.parseInt(backupId.substring(21, 23)),
                            Integer.parseInt(backupId.substring(24, 26))
                        ));

                        // 读取元数据
                        Path metaPath = backupDirPath.resolve(backupId + ".meta");
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
            backups.sort(Comparator.comparing(BackupVO::getCreateTime).reversed());
        } catch (IOException e) {
            log.error("获取备份列表失败", e);
        }

        return backups;
    }

    @Override
    public BackupVO getBackupDetail(String backupId) {
        log.debug("获取备份详情：{}", backupId);

        try {
            // 尝试查找 .sql 或 .zip 文件
            Path backupPath = backupDirPath.resolve(backupId + ".sql");
            if (!Files.exists(backupPath)) {
                backupPath = backupDirPath.resolve(backupId + ".zip");
            }

            if (!Files.exists(backupPath)) {
                throw new RuntimeException("备份文件不存在：" + backupId);
            }

            String filename = backupPath.getFileName().toString();
            BackupVO vo = new BackupVO();
            vo.setId(backupId);
            vo.setFilename(filename);
            vo.setSize(Files.size(backupPath));
            vo.setFormattedSize(formatFileSize(Files.size(backupPath)));
            vo.setType(filename.endsWith(".sql") ? "database" : "full");

            // 解析创建时间
            try {
                vo.setCreateTime(LocalDateTime.of(
                    Integer.parseInt(backupId.substring(10, 14)),
                    Integer.parseInt(backupId.substring(14, 16)),
                    Integer.parseInt(backupId.substring(16, 18)),
                    Integer.parseInt(backupId.substring(19, 21)),
                    Integer.parseInt(backupId.substring(21, 23)),
                    Integer.parseInt(backupId.substring(24, 26))
                ));
            } catch (Exception e) {
                log.warn("解析备份时间失败：{}", backupId, e);
            }

            // 读取元数据
            Path metaPath = backupDirPath.resolve(backupId + ".meta");
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

        Path backupPath = backupDirPath.resolve(backupId + ".sql");
        if (!Files.exists(backupPath)) {
            backupPath = backupDirPath.resolve(backupId + ".zip");
        }

        if (!Files.exists(backupPath)) {
            throw new RuntimeException("备份文件不存在：" + backupId);
        }

        return Files.readAllBytes(backupPath);
    }

    @Override
    public void restoreBackup(String backupId) {
        log.info("开始恢复备份：{}", backupId);

        try {
            // 获取备份详情
            BackupVO backup = getBackupDetail(backupId);
            Path backupPath = backupDirPath.resolve(backup.getFilename());

            if (!Files.exists(backupPath)) {
                throw new RuntimeException("备份文件不存在：" + backupId);
            }

            if ("database".equals(backup.getType())) {
                // 恢复数据库
                restoreDatabase(backupPath);
            } else if ("full".equals(backup.getType())) {
                // 恢复完整备份（从 ZIP 解压）
                restoreFullBackup(backupPath);
            }

            log.info("备份恢复完成：{}", backupId);
        } catch (IOException e) {
            log.error("恢复备份失败：{}", backupId, e);
            throw new RuntimeException("恢复备份失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("恢复备份被中断：{}", backupId, e);
            throw new RuntimeException("恢复备份被中断", e);
        }
    }

    /**
     * 恢复数据库
     */
    private void restoreDatabase(Path sqlPath) throws IOException, InterruptedException {
        String dbName = extractDatabaseName(databaseUrl);

        // 构建 mysql 命令
        List<String> command = new ArrayList<>();
        command.add("mysql");
        command.add("--host=localhost");
        command.add("--port=3306");
        command.add("--user=" + databaseUsername);
        command.add("--password=" + databasePassword);
        command.add("--default-character-set=utf8mb4");
        command.add(dbName);

        log.debug("执行命令：{}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        try (FileInputStream fis = new FileInputStream(sqlPath.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis);
             OutputStream os = process.getOutputStream()) {

            // 写入 SQL 文件
            bis.transferTo(os);
        }

        // 等待进程完成
        boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("mysql 恢复执行超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("mysql 恢复失败，退出码：" + exitCode);
        }
    }

    /**
     * 恢复完整备份
     */
    private void restoreFullBackup(Path zipPath) throws IOException {
        Path tempDir = Files.createTempDirectory("qblog-restore-");
        try {
            // 解压 ZIP 文件
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = tempDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            // 恢复数据库
            Path sqlPath = tempDir.resolve("database");
            if (Files.isDirectory(sqlPath)) {
                // 找到 SQL 文件
                try (var stream = Files.list(sqlPath)) {
                    stream.filter(p -> p.toString().endsWith(".sql"))
                        .findFirst()
                        .ifPresent(p -> {
                            try {
                                restoreDatabase(p);
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException("恢复数据库失败", e);
                            }
                        });
                }
            }

            // 恢复文件（如果存在）
            Path filesPath = tempDir.resolve("files");
            if (Files.exists(filesPath)) {
                Path targetUploads = Paths.get("uploads");
                copyDirectory(filesPath, targetUploads);
            }
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * 复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath).toString());
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.error("复制文件失败：{}", sourcePath, e);
                }
            });
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    @Override
    public void deleteBackup(String backupId) {
        log.info("删除备份：{}", backupId);

        try {
            // 删除 SQL 或 ZIP 文件
            Path sqlPath = backupDirPath.resolve(backupId + ".sql");
            Path zipPath = backupDirPath.resolve(backupId + ".zip");

            Files.deleteIfExists(sqlPath);
            Files.deleteIfExists(zipPath);

            // 删除元数据文件
            Path metaPath = backupDirPath.resolve(backupId + ".meta");
            Files.deleteIfExists(metaPath);

            log.info("备份删除完成：{}", backupId);
        } catch (IOException e) {
            log.error("删除备份失败：{}", backupId, e);
            throw new RuntimeException("删除备份失败：" + e.getMessage(), e);
        }
    }

    @Override
    public BackupVO importBackup(MultipartFile file) {
        log.info("导入备份文件：{}", file.getOriginalFilename());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 验证文件扩展名
        if (!originalFilename.endsWith(".sql") && !originalFilename.endsWith(".zip")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .sql 和 .zip");
        }

        // 验证文件大小（最大 100MB）
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小超过限制（100MB）");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String prefix = originalFilename.endsWith(".sql") ? DB_BACKUP_PREFIX : FULL_BACKUP_PREFIX;
        String filename = prefix + timestamp + (originalFilename.endsWith(".sql") ? ".sql" : ".zip");

        Path targetPath = backupDirPath.resolve(filename);

        try {
            file.transferTo(targetPath);

            // 写入元数据
            Properties meta = new Properties();
            meta.setProperty("type", originalFilename.endsWith(".sql") ? "database" : "full");
            meta.setProperty("description", "导入的备份");
            meta.setProperty("originalFilename", originalFilename);
            meta.setProperty("importTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            Path metaPath = backupDirPath.resolve(filename.substring(0, filename.lastIndexOf('.')) + ".meta");
            try (OutputStream os = Files.newOutputStream(metaPath)) {
                meta.store(os, "Backup Metadata");
            }

            log.info("备份导入完成：{}", filename);
            return getBackupDetail(filename.substring(0, filename.lastIndexOf('.')));
        } catch (IOException e) {
            log.error("导入备份失败", e);
            throw new RuntimeException("导入备份失败：" + e.getMessage(), e);
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
        vo.setBackupType(props.getProperty("backupType", "database"));

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
        String backupType = props.getProperty("backupType", "database");
        createBackup(backupType, "定时自动备份");
    }

    @Override
    public void cleanupOldBackups() {
        Properties props = loadSettings();
        int keepCount = Integer.parseInt(props.getProperty("keepCount", "7"));

        log.info("清理过期备份，保留数量：{}", keepCount);

        try {
            List<Path> backups = Files.list(backupDirPath)
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    return filename.endsWith(".sql") || filename.endsWith(".zip");
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
