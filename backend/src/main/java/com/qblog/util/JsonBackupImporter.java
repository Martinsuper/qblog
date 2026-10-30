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
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JSON 备份导入工具类
 * 恢复语义是"真实快照"：先清空业务表，再按备份中的主键原样重放，
 * 保证 article_tag / author_id / category_id 等引用与备份时的数据完全一致。
 * v3 备份包含知识库表；v2 旧备份（仅博客表）恢复时不会触碰知识库数据。
 */
@Slf4j
public class JsonBackupImporter {

    /** v3 备份的 manifest 版本；v2 是只含博客表的旧格式 */
    private static final Set<String> SUPPORTED_VERSIONS = Set.of("2.0", "3.0");

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final String uploadDirectory;
    private final JdbcTemplate jdbc;

    public JsonBackupImporter(UserMapper userMapper,
                              CategoryMapper categoryMapper,
                              TagMapper tagMapper,
                              ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper,
                              String uploadDirectory,
                              JdbcTemplate jdbc) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.uploadDirectory = uploadDirectory;
        this.jdbc = jdbc;

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
        /** v2 旧备份不含知识库数据，恢复时不应触碰知识库表 */
        private final List<Map<String, Object>> knowledgeWorkspaces;
        private final List<Map<String, Object>> knowledgeFolders;
        private final List<Map<String, Object>> knowledgeNotes;

        ParsedBackup(Path tempDir, List<User> users, List<Category> categories, List<Tag> tags,
                     List<Article> articles, List<ArticleTag> articleTags,
                     List<Map<String, Object>> knowledgeWorkspaces,
                     List<Map<String, Object>> knowledgeFolders,
                     List<Map<String, Object>> knowledgeNotes) {
            this.tempDir = tempDir;
            this.users = users;
            this.categories = categories;
            this.tags = tags;
            this.articles = articles;
            this.articleTags = articleTags;
            this.knowledgeWorkspaces = knowledgeWorkspaces;
            this.knowledgeFolders = knowledgeFolders;
            this.knowledgeNotes = knowledgeNotes;
        }

        boolean includesKnowledge() {
            return knowledgeWorkspaces != null;
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
            String version = String.valueOf(manifest.get("version"));
            if (!"QBlog".equals(manifest.get("application")) || !"json".equals(manifest.get("format"))
                    || !SUPPORTED_VERSIONS.contains(version)) {
                throw new IOException("不支持的备份格式或版本：" + version);
            }
            boolean v3 = "3.0".equals(version);

            // 在数据库写入前验证并解析所有数据文件，避免损坏的后续文件导致半恢复。
            Path dataDir = tempDir.resolve("data");
            List<User> users = parseList(dataDir, "user", User.class);
            List<Category> categories = parseList(dataDir, "category", Category.class);
            List<Tag> tags = parseList(dataDir, "tag", Tag.class);
            List<Article> articles = parseList(dataDir, "article", Article.class);
            List<ArticleTag> articleTags = parseList(dataDir, "article_tag", ArticleTag.class);

            // v3 全库备份：知识库三个分区缺一不可
            List<Map<String, Object>> workspaces = null, folders = null, notes = null;
            if (v3) {
                workspaces = parseRows(dataDir, "knowledge_workspace");
                folders = parseRows(dataDir, "knowledge_folder");
                notes = parseRows(dataDir, "knowledge_note");
                folders = sortKnowledgeFolders(folders);
                for (Map<String, Object> note : notes) {
                    if (note == null) throw new IOException("知识笔记行不能为空");
                    flag(note.get("favorite"), "favorite");
                    flag(note.get("deleted"), "deleted");
                    noteVersion(note.get("version"));
                }
            }

            log.info("备份版本: {}, 时间: {}", version, manifest.get("timestamp"));
            failed = false;
            return new ParsedBackup(tempDir, users, categories, tags, articles, articleTags, workspaces, folders, notes);
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

        // v2 旧备份不含知识库分区，此时绝不能清空知识库表（笔记是主数据）
        if (backup.includesKnowledge()) {
            counts.putAll(restoreKnowledge(backup));
        } else {
            log.info("v2 旧备份不包含知识库数据，知识库表未改动");
        }

        // 恢复上传文件
        restoreUploadFiles(backup.filesDir());

        log.info("JSON 备份恢复完成: {}", counts);
        return counts;
    }

    /**
     * 知识库表恢复（JdbcTemplate，无 MyBatis 实体）。与博客表同一个数据源，
     * 因此和调用方事务绑定在一起，失败整体回滚。
     */
    private Map<String, Integer> restoreKnowledge(ParsedBackup backup) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();

        // InnoDB 逐行检查自引用外键，批量删除目录前先断开父子关系。
        // 这些更新和删除都在调用方事务中，恢复失败时一起回滚。
        jdbc.update("DELETE FROM knowledge_note");
        jdbc.update("UPDATE knowledge_folder SET parent_id = NULL WHERE parent_id IS NOT NULL");
        jdbc.update("DELETE FROM knowledge_folder");
        jdbc.update("DELETE FROM knowledge_workspace");

        int workspaces = 0;
        for (Map<String, Object> row : backup.knowledgeWorkspaces) {
            jdbc.update("INSERT INTO knowledge_workspace(id,name) VALUES(?,?)",
                    asLong(row.get("id")), requireString(row, "name"));
            workspaces++;
        }

        // parse 已验证并按父目录先于子目录的顺序排列。
        int folders = 0;
        for (Map<String, Object> row : backup.knowledgeFolders) {
            jdbc.update("INSERT INTO knowledge_folder(id,workspace_id,parent_id,name,sort,create_time,update_time) "
                    + "VALUES(?,?,?,?,?,?,?)",
                    asLong(row.get("id")), asLong(row.get("workspace_id")), asNullableLong(row.get("parent_id")),
                    requireString(row, "name"), asIntWithDefault(row.get("sort"), 0),
                    timestamp(row.get("create_time")), timestamp(row.get("update_time")));
            folders++;
        }

        int notes = 0;
        for (Map<String, Object> row : backup.knowledgeNotes) {
            jdbc.update("INSERT INTO knowledge_note(id,workspace_id,title,content,summary,folder_id,tags,"
                            + "favorite,source,source_tool,external_id,version,deleted,create_time,update_time) "
                            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    asLong(row.get("id")), asLong(row.get("workspace_id")), requireString(row, "title"),
                    requireString(row, "content"), Objects.requireNonNullElse(asString(row.get("summary")), ""),
                    asNullableLong(row.get("folder_id")), tagsJson(row),
                    flag(row.get("favorite"), "favorite"), Objects.requireNonNullElse(asString(row.get("source")), "manual"),
                    asString(row.get("source_tool")), asString(row.get("external_id")),
                    noteVersion(row.get("version")), flag(row.get("deleted"), "deleted"),
                    timestamp(row.get("create_time")), timestamp(row.get("update_time")));
            notes++;
        }

        counts.put("knowledge_workspace", workspaces);
        counts.put("knowledge_folder", folders);
        counts.put("knowledge_note", notes);
        log.info("恢复知识库数据：workspace {} 条、目录 {} 条、笔记 {} 条", workspaces, folders, notes);
        return counts;
    }

    /** 校验目录自引用并使用非递归拓扑排序，避免深层目录导致栈溢出。 */
    private static List<Map<String, Object>> sortKnowledgeFolders(List<Map<String, Object>> rows) throws IOException {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        Map<Long, List<Long>> children = new HashMap<>();
        Deque<Long> ready = new ArrayDeque<>();
        for (Map<String, Object> row : rows) {
            if (row == null) throw new IOException("知识目录行不能为空");
            long id = folderId(row.get("id"), "id");
            folderId(row.get("workspace_id"), "workspace_id");
            if (byId.putIfAbsent(id, row) != null) throw new IOException("知识目录 id 重复: " + id);
            if (row.get("parent_id") != null) parents.put(id, folderId(row.get("parent_id"), "parent_id"));
        }
        for (Map.Entry<Long, Map<String, Object>> entry : byId.entrySet()) {
            long id = entry.getKey();
            Long parent = parents.get(id);
            if (parent == null) {
                ready.add(id);
            } else {
                Map<String, Object> parentRow = byId.get(parent);
                if (parentRow == null) throw new IOException("知识目录父目录不存在: " + parent);
                if (folderId(entry.getValue().get("workspace_id"), "workspace_id")
                        != folderId(parentRow.get("workspace_id"), "workspace_id")) {
                    throw new IOException("知识目录与父目录不属于同一工作区: " + id);
                }
                children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(id);
            }
        }
        List<Map<String, Object>> sorted = new ArrayList<>(rows.size());
        while (!ready.isEmpty()) {
            long id = ready.remove();
            sorted.add(byId.get(id));
            ready.addAll(children.getOrDefault(id, Collections.emptyList()));
        }
        if (sorted.size() != rows.size()) throw new IOException("知识目录存在循环引用");
        return sorted;
    }

    private static long folderId(Object value, String column) throws IOException {
        if (value instanceof Integer || value instanceof Long) return ((Number) value).longValue();
        if (value instanceof java.math.BigInteger number) {
            try { return number.longValueExact(); } catch (ArithmeticException ignored) { }
        }
        throw new IOException("知识目录缺少有效整数列: " + column);
    }

    private static long noteVersion(Object value) throws IOException {
        if (value == null) return 0L;
        long version;
        if (value instanceof Integer || value instanceof Long) {
            version = ((Number) value).longValue();
        } else if (value instanceof java.math.BigInteger number) {
            try {
                version = number.longValueExact();
            } catch (ArithmeticException e) {
                throw new IOException("知识笔记 version 超出 BIGINT 范围", e);
            }
        } else {
            throw new IOException("知识笔记 version 必须为整数");
        }
        if (version < 0) throw new IOException("知识笔记 version 不能为负数");
        return version;
    }

    private static int flag(Object value, String column) throws IOException {
        if (value == null) return 0;
        if (value instanceof Boolean bool) return bool ? 1 : 0;
        if ((value instanceof Integer || value instanceof Long || value instanceof java.math.BigInteger)
                && (value.toString().equals("0") || value.toString().equals("1"))) {
            return ((Number) value).intValue();
        }
        throw new IOException("知识笔记旗标无效: " + column);
    }

    /** JSON 数字可能是 Integer/Long/BigInteger，统一取 long */
    private static long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        throw new IllegalArgumentException("备份行缺少整数列: " + value);
    }

    private static Long asNullableLong(Object value) {
        return value == null ? null : asLong(value);
    }

    private static int asIntWithDefault(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private static String requireString(Map<String, Object> row, String column) {
        Object value = row.get(column);
        if (!(value instanceof String text)) throw new IllegalArgumentException("备份行缺少字符串列: " + column);
        return text;
    }

    /** tags 列是 JSON 数组；导出为 JsonNode 数组，恢复时转回字符串满足 CHECK(JSON_VALID) */
    private String tagsJson(Map<String, Object> row) {
        Object value = row.get("tags");
        if (value == null) return "[]";
        if (value instanceof String text) return text;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalArgumentException("tags 列无法序列化为 JSON", e);
        }
    }

    /** 导出的时间为 ISO 字符串，例如 2026-10-30T02:00:00.123456 */
    private static Timestamp timestamp(Object value) {
        if (value instanceof String text) return Timestamp.valueOf(LocalDateTime.parse(text));
        if (value instanceof Number number) return new Timestamp(number.longValue());
        throw new IllegalArgumentException("备份行缺少时间列: " + value);
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

    /** 知识库表没有实体类，按原始 Map 行解析 */
    private List<Map<String, Object>> parseRows(Path dataDir, String table) throws IOException {
        Path file = dataDir.resolve(table + ".json");
        if (!Files.isRegularFile(file) || !objectMapper.readTree(file.toFile()).isArray()) {
            throw new IOException("备份缺少有效的数据文件：" + table);
        }
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class);
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
        Path uploadsSource = filesDir.resolve("uploads");
        try {
            if (!Files.readAttributes(uploadsSource, java.nio.file.attribute.BasicFileAttributes.class).isDirectory()) {
                throw new IOException("备份 uploads 不是目录: " + uploadsSource);
            }
        } catch (NoSuchFileException e) {
            log.debug("uploads 目录不存在，跳过文件恢复");
            return;
        }

        Path targetUploads = Paths.get(uploadDirectory);
        Files.createDirectories(targetUploads);

        // 文件遍历及复制失败都交给调用方处理，不能将部分恢复报告为成功。
        Files.walkFileTree(uploadsSource, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path sourcePath, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                if (attrs.isRegularFile()) {
                    Path targetPath = targetUploads.resolve(uploadsSource.relativize(sourcePath).toString());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });

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
