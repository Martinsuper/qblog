package com.qblog.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qblog.entity.Article;
import com.qblog.entity.ArticleTag;
import com.qblog.entity.Category;
import com.qblog.entity.Tag;
import com.qblog.entity.User;
import com.qblog.mapper.ArticleMapper;
import com.qblog.mapper.ArticleTagMapper;
import com.qblog.mapper.CategoryMapper;
import com.qblog.mapper.TagMapper;
import com.qblog.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * JSON 备份导出工具类
 * 将数据库数据导出为 JSON 格式的 ZIP 文件
 */
@Slf4j
public class JsonBackupExporter {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;

    public JsonBackupExporter(UserMapper userMapper,
                              CategoryMapper categoryMapper,
                              TagMapper tagMapper,
                              ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 导出备份到指定路径
     *
     * @param outputPath 输出 ZIP 文件路径
     * @param description 备份描述
     * @return 备份文件路径
     */
    public Path exportBackup(Path outputPath, String description) throws IOException {
        log.info("开始导出 JSON 备份到: {}", outputPath);

        // 创建临时目录
        Path tempDir = Files.createTempDirectory("qblog-backup-");
        try {
            // 创建 data 目录
            Path dataDir = tempDir.resolve("data");
            Files.createDirectories(dataDir);

            // 导出各表数据
            exportTableData(dataDir);

            // 创建 manifest.json
            createManifest(tempDir, description);

            // 创建 files 目录并复制上传文件
            Path filesDir = tempDir.resolve("files");
            copyUploadFiles(filesDir);

            // 打包为 ZIP
            createZipArchive(tempDir, outputPath);

            log.info("JSON 备份导出完成: {}", outputPath);
            return outputPath;
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * 导出所有表数据到 JSON 文件
     */
    private void exportTableData(Path dataDir) throws IOException {
        log.debug("导出表数据...");

        // 导出用户表
        List<User> users = userMapper.selectList(new QueryWrapper<>());
        writeJsonFile(dataDir.resolve("user.json"), users);
        log.debug("导出 user 表: {} 条记录", users.size());

        // 导出分类表
        List<Category> categories = categoryMapper.selectList(new QueryWrapper<>());
        writeJsonFile(dataDir.resolve("category.json"), categories);
        log.debug("导出 category 表: {} 条记录", categories.size());

        // 导出标签表
        List<Tag> tags = tagMapper.selectList(new QueryWrapper<>());
        writeJsonFile(dataDir.resolve("tag.json"), tags);
        log.debug("导出 tag 表: {} 条记录", tags.size());

        // 导出文章表
        List<Article> articles = articleMapper.selectList(new QueryWrapper<>());
        writeJsonFile(dataDir.resolve("article.json"), articles);
        log.debug("导出 article 表: {} 条记录", articles.size());

        // 导出文章标签关联表
        List<ArticleTag> articleTags = articleTagMapper.selectList(new QueryWrapper<>());
        writeJsonFile(dataDir.resolve("article_tag.json"), articleTags);
        log.debug("导出 article_tag 表: {} 条记录", articleTags.size());
    }

    /**
     * 写入 JSON 文件
     */
    private void writeJsonFile(Path filePath, Object data) throws IOException {
        objectMapper.writeValue(filePath.toFile(), data);
    }

    /**
     * 创建 manifest.json 元信息文件
     */
    private void createManifest(Path tempDir, String description) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("version", "2.0");
        manifest.put("format", "json");
        manifest.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        manifest.put("description", description != null ? description : "");
        manifest.put("application", "QBlog");

        // 统计记录数
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("user", (long) userMapper.selectCount(new QueryWrapper<>()));
        counts.put("category", (long) categoryMapper.selectCount(new QueryWrapper<>()));
        counts.put("tag", (long) tagMapper.selectCount(new QueryWrapper<>()));
        counts.put("article", (long) articleMapper.selectCount(new QueryWrapper<>()));
        counts.put("article_tag", (long) articleTagMapper.selectCount(new QueryWrapper<>()));
        manifest.put("counts", counts);

        // 导入顺序（处理依赖关系）
        manifest.put("importOrder", Arrays.asList("user", "category", "tag", "article", "article_tag"));

        writeJsonFile(tempDir.resolve("manifest.json"), manifest);
        log.debug("创建 manifest.json");
    }

    /**
     * 复制上传文件到备份目录
     */
    private void copyUploadFiles(Path filesDir) throws IOException {
        Path uploadsPath = Paths.get("uploads");
        if (!Files.exists(uploadsPath)) {
            log.debug("uploads 目录不存在，跳过文件备份");
            return;
        }

        Path targetUploads = filesDir.resolve("uploads");
        Files.createDirectories(targetUploads);

        Files.walk(uploadsPath)
            .filter(Files::isRegularFile)
            .forEach(sourcePath -> {
                try {
                    Path relativePath = uploadsPath.relativize(sourcePath);
                    Path targetPath = targetUploads.resolve(relativePath.toString());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.warn("复制文件失败: {}", sourcePath, e);
                }
            });

        log.debug("复制上传文件完成");
    }

    /**
     * 创建 ZIP 压缩包
     */
    private void createZipArchive(Path sourceDir, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String relativePath = sourceDir.relativize(file).toString().replace('\\', '/');
                        ZipEntry entry = new ZipEntry(relativePath);
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    /**
     * 递归删除目录
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
}