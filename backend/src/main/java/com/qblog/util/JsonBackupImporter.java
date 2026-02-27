package com.qblog.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JSON 备份导入工具类
 * 从 JSON 格式的 ZIP 文件恢复数据库数据
 */
@Slf4j
public class JsonBackupImporter {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;

    public JsonBackupImporter(UserMapper userMapper,
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
    }

    /**
     * 从 ZIP 文件导入备份数据
     *
     * @param zipPath ZIP 文件路径
     * @return 导入结果统计
     */
    public Map<String, Integer> importBackup(Path zipPath) throws IOException {
        log.info("开始导入 JSON 备份: {}", zipPath);

        // 解压到临时目录
        Path tempDir = Files.createTempDirectory("qblog-restore-");
        try {
            // 解压 ZIP 文件
            extractZip(zipPath, tempDir);

            // 验证 manifest.json
            Path manifestPath = tempDir.resolve("manifest.json");
            if (!Files.exists(manifestPath)) {
                throw new IOException("无效的备份文件：缺少 manifest.json");
            }

            // 读取并验证 manifest
            Map<String, Object> manifest = readManifest(manifestPath);
            log.info("备份版本: {}, 时间: {}", manifest.get("version"), manifest.get("timestamp"));

            // 导入数据（按依赖顺序）
            Path dataDir = tempDir.resolve("data");
            Map<String, Integer> counts = new LinkedHashMap<>();

            counts.put("user", importUsers(dataDir));
            counts.put("category", importCategories(dataDir));
            counts.put("tag", importTags(dataDir));
            counts.put("article", importArticles(dataDir));
            counts.put("article_tag", importArticleTags(dataDir));

            // 恢复上传文件
            restoreUploadFiles(tempDir.resolve("files"));

            log.info("JSON 备份导入完成: {}", counts);
            return counts;
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * 解压 ZIP 文件
     */
    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 读取 manifest.json
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readManifest(Path manifestPath) throws IOException {
        return objectMapper.readValue(manifestPath.toFile(), Map.class);
    }

    /**
     * 导入用户数据
     */
    private int importUsers(Path dataDir) throws IOException {
        Path filePath = dataDir.resolve("user.json");
        if (!Files.exists(filePath)) {
            log.debug("user.json 不存在，跳过");
            return 0;
        }

        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, User.class);
        List<User> users = objectMapper.readValue(filePath.toFile(), type);

        int count = 0;
        for (User user : users) {
            // 检查是否存在，存在则更新，不存在则插入
            User existing = userMapper.selectById(user.getId());
            if (existing != null) {
                userMapper.updateById(user);
                log.debug("更新用户: id={}, username={}", user.getId(), user.getUsername());
            } else {
                userMapper.insert(user);
                log.debug("插入用户: id={}, username={}", user.getId(), user.getUsername());
            }
            count++;
        }

        log.info("导入用户数据: {} 条", count);
        return count;
    }

    /**
     * 导入分类数据
     */
    private int importCategories(Path dataDir) throws IOException {
        Path filePath = dataDir.resolve("category.json");
        if (!Files.exists(filePath)) {
            log.debug("category.json 不存在，跳过");
            return 0;
        }

        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, Category.class);
        List<Category> categories = objectMapper.readValue(filePath.toFile(), type);

        int count = 0;
        for (Category category : categories) {
            Category existing = categoryMapper.selectById(category.getId());
            if (existing != null) {
                categoryMapper.updateById(category);
                log.debug("更新分类: id={}, name={}", category.getId(), category.getName());
            } else {
                categoryMapper.insert(category);
                log.debug("插入分类: id={}, name={}", category.getId(), category.getName());
            }
            count++;
        }

        log.info("导入分类数据: {} 条", count);
        return count;
    }

    /**
     * 导入标签数据
     */
    private int importTags(Path dataDir) throws IOException {
        Path filePath = dataDir.resolve("tag.json");
        if (!Files.exists(filePath)) {
            log.debug("tag.json 不存在，跳过");
            return 0;
        }

        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, Tag.class);
        List<Tag> tags = objectMapper.readValue(filePath.toFile(), type);

        int count = 0;
        for (Tag tag : tags) {
            Tag existing = tagMapper.selectById(tag.getId());
            if (existing != null) {
                tagMapper.updateById(tag);
                log.debug("更新标签: id={}, name={}", tag.getId(), tag.getName());
            } else {
                tagMapper.insert(tag);
                log.debug("插入标签: id={}, name={}", tag.getId(), tag.getName());
            }
            count++;
        }

        log.info("导入标签数据: {} 条", count);
        return count;
    }

    /**
     * 导入文章数据
     */
    private int importArticles(Path dataDir) throws IOException {
        Path filePath = dataDir.resolve("article.json");
        if (!Files.exists(filePath)) {
            log.debug("article.json 不存在，跳过");
            return 0;
        }

        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, Article.class);
        List<Article> articles = objectMapper.readValue(filePath.toFile(), type);

        int count = 0;
        for (Article article : articles) {
            Article existing = articleMapper.selectById(article.getId());
            if (existing != null) {
                articleMapper.updateById(article);
                log.debug("更新文章: id={}, title={}", article.getId(), article.getTitle());
            } else {
                articleMapper.insert(article);
                log.debug("插入文章: id={}, title={}", article.getId(), article.getTitle());
            }
            count++;
        }

        log.info("导入文章数据: {} 条", count);
        return count;
    }

    /**
     * 导入文章标签关联数据
     */
    private int importArticleTags(Path dataDir) throws IOException {
        Path filePath = dataDir.resolve("article_tag.json");
        if (!Files.exists(filePath)) {
            log.debug("article_tag.json 不存在，跳过");
            return 0;
        }

        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ArticleTag.class);
        List<ArticleTag> articleTags = objectMapper.readValue(filePath.toFile(), type);

        int count = 0;
        for (ArticleTag articleTag : articleTags) {
            ArticleTag existing = articleTagMapper.selectById(articleTag.getId());
            if (existing != null) {
                articleTagMapper.updateById(articleTag);
                log.debug("更新文章标签关联: id={}", articleTag.getId());
            } else {
                articleTagMapper.insert(articleTag);
                log.debug("插入文章标签关联: id={}", articleTag.getId());
            }
            count++;
        }

        log.info("导入文章标签关联数据: {} 条", count);
        return count;
    }

    /**
     * 恢复上传文件
     */
    private void restoreUploadFiles(Path filesDir) throws IOException {
        if (!Files.exists(filesDir)) {
            log.debug("files 目录不存在，跳过文件恢复");
            return;
        }

        Path uploadsSource = filesDir.resolve("uploads");
        if (!Files.exists(uploadsSource)) {
            log.debug("uploads 目录不存在，跳过文件恢复");
            return;
        }

        Path targetUploads = Paths.get("uploads");
        Files.createDirectories(targetUploads);

        Files.walk(uploadsSource)
            .filter(Files::isRegularFile)
            .forEach(sourcePath -> {
                try {
                    Path relativePath = uploadsSource.relativize(sourcePath);
                    Path targetPath = targetUploads.resolve(relativePath.toString());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.warn("恢复文件失败: {}", sourcePath, e);
                }
            });

        log.info("上传文件恢复完成");
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