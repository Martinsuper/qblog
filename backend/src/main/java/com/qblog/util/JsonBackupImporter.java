package com.qblog.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
 * 恢复语义是"真实快照"：先清空业务表，再按备份中的主键原样重放，
 * 保证 article_tag / author_id / category_id 等引用与备份时的数据完全一致。
 */
@Slf4j
public class JsonBackupImporter {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final String uploadDirectory;

    public JsonBackupImporter(UserMapper userMapper,
                              CategoryMapper categoryMapper,
                              TagMapper tagMapper,
                              ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper,
                              String uploadDirectory) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.uploadDirectory = uploadDirectory;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 解析后的备份数据：已解压并校验，等待恢复。用完必须 {@link #close()} 清理临时目录。
     * 数据库写入应当放在调用方的事务里（restore 不自行管理事务）。
     */
    public static final class ParsedBackup implements AutoCloseable {
        private final Path tempDir;
        private final List<User> users;
        private final List<Category> categories;
        private final List<Tag> tags;
        private final List<Article> articles;
        private final List<ArticleTag> articleTags;

        ParsedBackup(Path tempDir, List<User> users, List<Category> categories, List<Tag> tags,
                     List<Article> articles, List<ArticleTag> articleTags) {
            this.tempDir = tempDir;
            this.users = users;
            this.categories = categories;
            this.tags = tags;
            this.articles = articles;
            this.articleTags = articleTags;
        }

        Path filesDir() {
            return tempDir.resolve("files");
        }

        @Override
        public void close() {
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * 从 ZIP 文件解压并校验备份数据（不写数据库，可在事务外执行）
     *
     * @param zipPath ZIP 文件路径
     * @return 解析后的备份数据
     */
    public ParsedBackup parse(Path zipPath) throws IOException {
        log.info("开始解析 JSON 备份: {}", zipPath);

        Path tempDir = Files.createTempDirectory("qblog-restore-");
        boolean failed = true;
        try {
            extractZip(zipPath, tempDir);

            // 验证 manifest.json
            Path manifestPath = tempDir.resolve("manifest.json");
            if (!Files.exists(manifestPath)) {
                throw new IOException("无效的备份文件：缺少 manifest.json");
            }

            // 读取并验证 manifest
            Map<String, Object> manifest = readManifest(manifestPath);
            if (!"QBlog".equals(manifest.get("application")) || !"json".equals(manifest.get("format"))
                    || !"2.0".equals(manifest.get("version"))) {
                throw new IOException("不支持的备份格式或版本");
            }
            // 在数据库写入前验证并解析所有数据文件，避免损坏的后续文件导致半恢复。
            Path dataDir = tempDir.resolve("data");
            List<User> users = parseList(dataDir, "user", User.class);
            List<Category> categories = parseList(dataDir, "category", Category.class);
            List<Tag> tags = parseList(dataDir, "tag", Tag.class);
            List<Article> articles = parseList(dataDir, "article", Article.class);
            List<ArticleTag> articleTags = parseList(dataDir, "article_tag", ArticleTag.class);

            log.info("备份版本: {}, 时间: {}", manifest.get("version"), manifest.get("timestamp"));
            failed = false;
            return new ParsedBackup(tempDir, users, categories, tags, articles, articleTags);
        } finally {
            if (failed) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    /**
     * 执行恢复：清空业务表后按备份中的显式主键重放（应在事务内调用）
     *
     * @return 各表恢复的记录数
     */
    public Map<String, Integer> restore(ParsedBackup backup) throws IOException {
        log.info("开始清空并重放备份数据");

        // 按外键依赖顺序清空：这是快照恢复语义，恢复后数据库与备份时点完全一致
        articleTagMapper.delete(new QueryWrapper<>());
        articleMapper.delete(new QueryWrapper<>());
        tagMapper.delete(new QueryWrapper<>());
        categoryMapper.delete(new QueryWrapper<>());
        userMapper.delete(new QueryWrapper<>());

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("user", insertAll("user", backup.users, userMapper::insertWithId));
        counts.put("category", insertAll("category", backup.categories, categoryMapper::insertWithId));
        counts.put("tag", insertAll("tag", backup.tags, tagMapper::insertWithId));
        counts.put("article", insertAll("article", backup.articles, articleMapper::insertWithId));
        counts.put("article_tag", insertAll("article_tag", backup.articleTags, articleTagMapper::insertWithId));

        // 恢复上传文件
        restoreUploadFiles(backup.filesDir());

        log.info("JSON 备份恢复完成: {}", counts);
        return counts;
    }

    /**
     * 一步式导入（解析 + 恢复）。生产路径应使用 parse/restore 以便把解压挡在事务外。
     */
    public Map<String, Integer> importBackup(Path zipPath) throws IOException {
        try (ParsedBackup backup = parse(zipPath)) {
            return restore(backup);
        }
    }

    private <T> List<T> parseList(Path dataDir, String table, Class<T> type) throws IOException {
        Path file = dataDir.resolve(table + ".json");
        if (!Files.isRegularFile(file) || !objectMapper.readTree(file.toFile()).isArray()) {
            throw new IOException("备份缺少有效的数据文件：" + table);
        }
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
        return objectMapper.readValue(file.toFile(), listType);
    }

    private <T> int insertAll(String table, List<T> rows, java.util.function.ToIntFunction<T> insert) {
        int count = 0;
        for (T row : rows) {
            insert.applyAsInt(row);
            count++;
        }
        log.info("恢复{}数据: {} 条", table, count);
        return count;
    }

    /**
     * 解压 ZIP 文件
     */
    // 限制实际解压字节，不能信任 ZIP 声明的 size（也不能只限制上传压缩大小）。
    private static final long MAX_ENTRY_BYTES = 64L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;
    private static final int MAX_ENTRIES = 10_000;

    void extractZip(Path zipPath, Path targetDir) throws IOException {
        Path root = targetDir.toAbsolutePath().normalize();
        long total = 0;
        int entries = 0;
        Set<Path> seen = new HashSet<>();
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IOException("备份文件条目过多");
                String name = entry.getName();
                if (name.contains("\\") || name.matches("^[A-Za-z]:.*")) {
                    throw new IOException("备份包含非法路径");
                }
                Path entryPath = root.resolve(name).normalize();
                if (!entryPath.startsWith(root) || entryPath.equals(root) || !seen.add(entryPath)) {
                    throw new IOException("备份包含越界或重复路径");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    long size = 0;
                    try (OutputStream out = Files.newOutputStream(entryPath, StandardOpenOption.CREATE_NEW)) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            size += read;
                            total += read;
                            if (size > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) {
                                throw new IOException("备份解压大小超过限制");
                            }
                            out.write(buffer, 0, read);
                        }
                    }
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

        Path targetUploads = Paths.get(uploadDirectory);
        Files.createDirectories(targetUploads);

        try (var paths = Files.walk(uploadsSource)) {
            paths.filter(Files::isRegularFile)
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
        }

        log.info("上传文件恢复完成");
    }

    /**
     * 递归删除目录
     */
    private static void deleteDirectory(File directory) {
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
