package com.qblog.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * JSON 备份导出工具类
 * 将全库数据导出为 JSON 格式的 ZIP 文件（v3：含知识库表与上传文件）
 */
@Slf4j
public class JsonBackupExporter {

    /** 知识库表没有 MyBatis 实体，走 JdbcTemplate 读原始行 */
    private static final String NOTE_COLUMNS =
            "id,workspace_id,title,content,summary,folder_id,tags,favorite,source,source_tool,external_id,version,deleted,create_time,update_time";
    private static final String FOLDER_COLUMNS =
            "id,workspace_id,parent_id,name,sort,create_time,update_time";
    private static final String WORKSPACE_COLUMNS = "id,name";

    // 与导入器对实际解压字节和条目数的限制保持一致。
    private static final long MAX_ENTRY_BYTES = 64L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;
    private static final int MAX_ENTRIES = 10_000;

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final JdbcTemplate jdbc;
    private final String uploadDirectory;

    public JsonBackupExporter(UserMapper userMapper,
                              CategoryMapper categoryMapper,
                              TagMapper tagMapper,
                              ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper,
                              JdbcTemplate jdbc,
                              String uploadDirectory) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.jdbc = jdbc;
        this.uploadDirectory = uploadDirectory;

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

            // 在目标目录暂存完整 ZIP，避免失败时截断已有备份。
            Path absoluteOutput = outputPath.toAbsolutePath();
            Path stagedZip = Files.createTempFile(absoluteOutput.getParent(), ".qblog-backup-", ".zip.tmp");
            try {
                createZipArchive(tempDir, stagedZip);
                try {
                    Files.move(stagedZip, absoluteOutput, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(stagedZip, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(stagedZip);
            }

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

        // 导出知识库表（v3 新增：没有实体类，读原始行）
        exportKnowledge(dataDir);
    }

    /**
     * 导出知识库三张表。日期列转为 ISO 字符串，tags 列解析为 JSON 数组，
     * 与知识库 JSON 流式导出（KnowledgeBackupService）保持同一种行格式。
     */
    private void exportKnowledge(Path dataDir) throws IOException {
        List<Map<String, Object>> workspace = normalizedRows("SELECT " + WORKSPACE_COLUMNS + " FROM knowledge_workspace");
        writeJsonFile(dataDir.resolve("knowledge_workspace.json"), workspace);
        log.debug("导出 knowledge_workspace 表: {} 条记录", workspace.size());

        List<Map<String, Object>> folders = normalizedRows("SELECT " + FOLDER_COLUMNS + " FROM knowledge_folder");
        writeJsonFile(dataDir.resolve("knowledge_folder.json"), folders);
        log.debug("导出 knowledge_folder 表: {} 条记录", folders.size());

        List<Map<String, Object>> notes = normalizedRows("SELECT " + NOTE_COLUMNS + " FROM knowledge_note");
        writeJsonFile(dataDir.resolve("knowledge_note.json"), notes);
        log.debug("导出 knowledge_note 表: {} 条记录", notes.size());
    }

    /**
     * 列名与值一一对应；DATETIME 统一转 ISO 字符串；tags 列还原成 JSON 数组
     */
    private List<Map<String, Object>> normalizedRows(String sql) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> raw : jdbc.queryForList(sql)) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Timestamp time) {
                    row.put(entry.getKey(), time.toLocalDateTime().toString());
                } else if (entry.getKey().equals("tags") && value != null) {
                    String text = value.toString();
                    JsonNode node;
                    try {
                        node = objectMapper.readTree(text);
                    } catch (IOException e) {
                        throw new IllegalStateException("知识笔记 tags 列不是合法 JSON：" + e.getMessage(), e);
                    }
                    row.put(entry.getKey(), node);
                } else {
                    row.put(entry.getKey(), value);
                }
            }
            rows.add(row);
        }
        return rows;
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
        manifest.put("version", "3.0");
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
        counts.put("knowledge_workspace", (long) jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_workspace", Long.class));
        counts.put("knowledge_folder", (long) jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_folder", Long.class));
        counts.put("knowledge_note", (long) jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_note", Long.class));
        manifest.put("counts", counts);

        // 导入顺序（处理依赖关系）
        manifest.put("importOrder", Arrays.asList(
                "user", "category", "tag", "article", "article_tag",
                "knowledge_workspace", "knowledge_folder", "knowledge_note"));

        writeJsonFile(tempDir.resolve("manifest.json"), manifest);
        log.debug("创建 manifest.json");
    }

    /**
     * 复制上传文件到备份目录
     */
    private void copyUploadFiles(Path filesDir) throws IOException {
        Path uploadsPath = Paths.get(uploadDirectory);
        if (Files.notExists(uploadsPath)) {
            log.debug("uploads 目录不存在，跳过文件备份");
            return;
        }

        Path targetUploads = filesDir.resolve("uploads");
        Files.createDirectories(targetUploads);

        if (!Files.isDirectory(uploadsPath)) {
            throw new IOException("上传路径不是可访问的目录: " + uploadsPath);
        }
        Files.walkFileTree(uploadsPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path sourcePath, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                // 显式读取符号链接目标，避免 isRegularFile 将访问错误悄悄视为不存在。
                var targetAttrs = attrs.isSymbolicLink()
                        ? Files.readAttributes(sourcePath, java.nio.file.attribute.BasicFileAttributes.class) : attrs;
                if (!targetAttrs.isRegularFile()) {
                    throw new IOException("不支持备份的上传文件类型: " + sourcePath);
                }
                Path targetPath = targetUploads.resolve(uploadsPath.relativize(sourcePath));
                Files.createDirectories(targetPath.getParent());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        log.debug("复制上传文件完成");
    }

    /**
     * 创建 ZIP 压缩包
     */
    private void createZipArchive(Path sourceDir, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                private long total;
                private int entries;
                private final byte[] buffer = new byte[8192];

                @Override
                public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                    String relativePath = sourceDir.relativize(file).toString().replace('\\', '/');
                    if (++entries > MAX_ENTRIES) throw new IOException("备份文件条目过多");
                    if (attrs.size() > MAX_ENTRY_BYTES) throw new IOException("备份条目超过 64 MiB 限制: " + relativePath);
                    zos.putNextEntry(new ZipEntry(relativePath));
                    long size = 0;
                    try (InputStream input = Files.newInputStream(file)) {
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            size += read;
                            total += read;
                            if (size > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) {
                                throw new IOException("备份解压大小超过限制: " + relativePath);
                            }
                            zos.write(buffer, 0, read);
                        }
                    }
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
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