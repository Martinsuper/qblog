package com.qblog.util;

import com.qblog.entity.Article;
import com.qblog.mapper.ArticleMapper;
import com.qblog.mapper.ArticleTagMapper;
import com.qblog.mapper.CategoryMapper;
import com.qblog.mapper.TagMapper;
import com.qblog.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonBackupImporterTest {
    @TempDir Path temp;
    private final UserMapper users = mock(UserMapper.class);
    private final CategoryMapper categories = mock(CategoryMapper.class);
    private final TagMapper tags = mock(TagMapper.class);
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final ArticleTagMapper articleTags = mock(ArticleTagMapper.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private JsonBackupImporter importer() {
        return new JsonBackupImporter(users, categories, tags, articles, articleTags, "uploads", jdbc);
    }

    private Path zip(String name, byte[] data) throws IOException {
        Path zip = temp.resolve("input.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry(name));
            out.write(data);
            out.closeEntry();
        }
        return zip;
    }

    @Test void rejectsTraversalBeforeWritingOutsideRoot() throws Exception {
        Path root = Files.createDirectory(temp.resolve("extract"));
        assertThrows(IOException.class, () -> importer().extractZip(zip("../outside.txt", new byte[]{1}), root));
        assertFalse(Files.exists(temp.resolve("outside.txt")));
    }

    @Test void rejectsAbsoluteAndWindowsPaths() throws Exception {
        Path root = Files.createDirectory(temp.resolve("extract"));
        for (String name : new String[]{temp.resolve("outside.txt").toString(), "C:/outside.txt", "..\\outside.txt"}) {
            assertThrows(IOException.class, () -> importer().extractZip(zip(name, new byte[]{1}), root));
        }
    }

    @Test void extractsNormalFile() throws Exception {
        Path root = Files.createDirectory(temp.resolve("extract"));
        importer().extractZip(zip("data/article.json", "[]".getBytes()), root);
        assertEquals("[]", Files.readString(root.resolve("data/article.json")));
    }

    @Test void rejectsCompressionBombUsingActualBytes() throws Exception {
        Path input = temp.resolve("bomb.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(input))) {
            out.putNextEntry(new ZipEntry("data/article.json"));
            byte[] buffer = new byte[8192];
            for (int i = 0; i <= 8192; i++) out.write(buffer);
            out.closeEntry();
        }
        assertTrue(Files.size(input) < 100_000);
        Path root = Files.createDirectory(temp.resolve("extract"));
        assertThrows(IOException.class, () -> importer().extractZip(input, root));
    }

    @Test void restoreIsSnapshotReplayWithOriginalIds() throws Exception {
        Path backup = temp.resolve("backup.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(backup))) {
            out.putNextEntry(new ZipEntry("manifest.json"));
            out.write(("{" +
                    "\"application\":\"QBlog\",\"format\":\"json\",\"version\":\"2.0\"," +
                    "\"timestamp\":\"2026-10-30T00:00:00\"}").getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/user.json"));
            out.write("[{\"id\":1,\"username\":\"admin\",\"password\":\"$2a$10$x\",\"role\":1}]".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/category.json"));
            out.write("[{\"id\":3,\"name\":\"技术\",\"sort\":1}]".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/tag.json"));
            out.write("[{\"id\":5,\"name\":\"Java\"}]".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/article.json"));
            out.write(("[{\"id\":42,\"title\":\"旧文\",\"status\":1,\"viewCount\":7,\"version\":3}]")
                    .getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/article_tag.json"));
            out.write(("[{\"id\":9,\"articleId\":42,\"tagId\":5}]").getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        Map<String, Integer> counts;
        try (JsonBackupImporter.ParsedBackup parsed = importer().parse(backup)) {
            counts = importer().restore(parsed);
        }

        // 恢复是快照语义：先清空全部业务表，再按备份中的原 id 重放
        InOrder inOrder = inOrder(articleTags, articles, tags, categories, users);
        inOrder.verify(articleTags).delete(any());
        inOrder.verify(articles).delete(any());
        inOrder.verify(tags).delete(any());
        inOrder.verify(categories).delete(any());
        inOrder.verify(users).delete(any());

        var articleCaptor = org.mockito.ArgumentCaptor.forClass(Article.class);
        verify(articles).insertWithId(articleCaptor.capture());
        assertEquals(42L, articleCaptor.getValue().getId());
        assertEquals("旧文", articleCaptor.getValue().getTitle());
        // insertWithId 必须显式写入主键：AUTO 主键的普通 insert() 会重新分配 id，
        // 恢复后 article_tag.article_id=42 就会指向错误的文章
        verify(articles, never()).insert(any(Article.class));
        verify(articleTags).insertWithId(any());
        assertEquals(1, counts.get("article"));
        assertEquals(1, counts.get("article_tag"));
        // v2 备份不含知识库分区，恢复时绝不触碰知识库表（笔记是主数据）
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        // 解析/恢复失败或结束后临时目录必须清理
        var tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (var entries = Files.list(tempRoot)) {
            assertTrue(entries.noneMatch(p -> p.getFileName().toString().startsWith("qblog-restore-")));
        }
    }

    @Test void v3BackupRestoresKnowledgeTablesInForeignKeyOrder() throws Exception {
        Path backup = temp.resolve("v3.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(backup))) {
            out.putNextEntry(new ZipEntry("manifest.json"));
            out.write(("{" +
                    "\"application\":\"QBlog\",\"format\":\"json\",\"version\":\"3.0\"," +
                    "\"timestamp\":\"2026-10-30T00:00:00\"}").getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            for (String table : new String[]{"user", "category", "tag", "article", "article_tag"}) {
                out.putNextEntry(new ZipEntry("data/" + table + ".json"));
                out.write("[]".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry("data/knowledge_workspace.json"));
            out.write("[{\"id\":1,\"name\":\"私人工作区\"}]".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/knowledge_folder.json"));
            out.write(("[{\"id\":10,\"workspace_id\":1,\"parent_id\":null,\"name\":\"Java\",\"sort\":0,\"create_time\":\"2026-10-30T08:00:00\",\"update_time\":\"2026-10-30T08:00:00\"},"
                    + "{\"id\":11,\"workspace_id\":1,\"parent_id\":10,\"name\":\"Spring\",\"sort\":0,\"create_time\":\"2026-10-30T08:00:00\",\"update_time\":\"2026-10-30T08:00:00\"}]")
                    .getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("data/knowledge_note.json"));
            out.write(("[{\"id\":20,\"workspace_id\":1,\"title\":\"笔记A\",\"content\":\"正文\",\"summary\":\"\",\"folder_id\":11,"
                    + "\"tags\":[\"Java\"],\"favorite\":1,\"source\":\"manual\",\"source_tool\":null,\"external_id\":null,"
                    + "\"version\":2,\"deleted\":0,\"create_time\":\"2026-10-30T09:00:00\",\"update_time\":\"2026-10-30T09:30:00\"}]")
                    .getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        Map<String, Integer> counts;
        try (JsonBackupImporter.ParsedBackup parsed = importer().parse(backup)) {
            assertTrue(parsed.includesKnowledge());
            counts = importer().restore(parsed);
        }

        assertEquals(0, counts.get("article"));
        assertEquals(1, counts.get("knowledge_workspace"));
        assertEquals(2, counts.get("knowledge_folder"));
        assertEquals(1, counts.get("knowledge_note"));

        // 删除顺序 note → folder → workspace（不带参数的调用）；插入顺序 workspace → folder → note（外键依赖）
        InOrder order = inOrder(jdbc);
        order.verify(jdbc).update(argThat((String sql) -> sql.startsWith("DELETE FROM knowledge_note")));
        order.verify(jdbc).update("UPDATE knowledge_folder SET parent_id = NULL WHERE parent_id IS NOT NULL");
        order.verify(jdbc).update(argThat((String sql) -> sql.startsWith("DELETE FROM knowledge_folder")));
        order.verify(jdbc).update(argThat((String sql) -> sql.startsWith("DELETE FROM knowledge_workspace")));
        var workspaceArgs = ArgumentCaptor.forClass(Object[].class);
        order.verify(jdbc).update(argThat((String sql) -> sql.startsWith("INSERT INTO knowledge_workspace")), workspaceArgs.capture());
        assertArrayEquals(new Object[]{1L, "私人工作区"}, workspaceArgs.getValue());
        var folderArgs = ArgumentCaptor.forClass(Object[].class);
        order.verify(jdbc, times(2)).update(argThat((String sql) -> sql.startsWith("INSERT INTO knowledge_folder")), folderArgs.capture());
        Object[] folderFirst = folderArgs.getAllValues().get(0);
        Object[] folderSecond = folderArgs.getAllValues().get(1);
        // 按 id 升序插入 = 父目录先于子目录（自引用外键）
        assertEquals(10L, folderFirst[0]);
        assertNull(folderFirst[2]);
        assertEquals(11L, folderSecond[0]);
        assertEquals(10L, folderSecond[2]);
        var noteArgs = ArgumentCaptor.forClass(Object[].class);
        order.verify(jdbc).update(argThat((String sql) -> sql.startsWith("INSERT INTO knowledge_note")), noteArgs.capture());

        // tags 数组要转回 JSON 字符串写库（CHECK(JSON_VALID)），时间要走 Timestamp，标题/正文原样保留
        Object[] note = noteArgs.getValue();
        assertEquals(15, note.length);
        assertEquals(20L, note[0]);
        assertEquals("正文", note[3]);
        assertEquals("[\"Java\"]", note[6]);
        assertEquals(LocalDateTime.parse("2026-10-30T09:00:00"), ((java.sql.Timestamp) note[13]).toLocalDateTime());
    }

    private Path v3Zip(String folders, String notes) throws IOException {
        Path backup = temp.resolve("v3-regression.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(backup))) {
            Map<String, String> entries = new java.util.LinkedHashMap<>();
            entries.put("manifest.json", "{\"application\":\"QBlog\",\"format\":\"json\",\"version\":\"3.0\"}");
            for (String table : new String[]{"user", "category", "tag", "article", "article_tag"}) {
                entries.put("data/" + table + ".json", "[]");
            }
            entries.put("data/knowledge_workspace.json", "[{\"id\":1,\"name\":\"workspace\"}]");
            entries.put("data/knowledge_folder.json", folders);
            entries.put("data/knowledge_note.json", notes);
            for (var entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return backup;
    }

    @Test void sortsFoldersByParentsNotIdsAndRestoresBooleanAndNumericFlags() throws Exception {
        String folders = "[{\"id\":1,\"workspace_id\":1,\"parent_id\":5,\"name\":\"child\",\"create_time\":\"2026-01-01T00:00:00\",\"update_time\":\"2026-01-01T00:00:00\"},"
                + "{\"id\":5,\"workspace_id\":1,\"parent_id\":9,\"name\":\"parent\",\"create_time\":\"2026-01-01T00:00:00\",\"update_time\":\"2026-01-01T00:00:00\"},"
                + "{\"id\":9,\"workspace_id\":1,\"name\":\"root\",\"create_time\":\"2026-01-01T00:00:00\",\"update_time\":\"2026-01-01T00:00:00\"}]";
        String notes = "[{\"id\":1,\"workspace_id\":1,\"title\":\"a\",\"content\":\"\",\"favorite\":true,\"deleted\":false,\"create_time\":\"2026-01-01T00:00:00\",\"update_time\":\"2026-01-01T00:00:00\"},"
                + "{\"id\":2,\"workspace_id\":1,\"title\":\"b\",\"content\":\"\",\"favorite\":0,\"deleted\":1,\"create_time\":\"2026-01-01T00:00:00\",\"update_time\":\"2026-01-01T00:00:00\"}]";
        try (var parsed = importer().parse(v3Zip(folders, notes))) { importer().restore(parsed); }
        var folderArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(3)).update(startsWith("INSERT INTO knowledge_folder"), folderArgs.capture());
        assertEquals(java.util.List.of(9L, 5L, 1L), folderArgs.getAllValues().stream().map(a -> a[0]).toList());
        var noteArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(2)).update(startsWith("INSERT INTO knowledge_note"), noteArgs.capture());
        assertEquals(1, noteArgs.getAllValues().get(0)[7]);
        assertEquals(0, noteArgs.getAllValues().get(0)[12]);
        assertEquals(0, noteArgs.getAllValues().get(1)[7]);
        assertEquals(1, noteArgs.getAllValues().get(1)[12]);
    }

    private void addUpload(Path backup, String name, String content) throws IOException {
        try (var zipFs = java.nio.file.FileSystems.newFileSystem(backup, Map.of())) {
            Path file = zipFs.getPath("/files/uploads/" + name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }
    }

    @Test void restoresNestedUploadFiles() throws Exception {
        Path backup = v3Zip("[]", "[]");
        addUpload(backup, "nested/file.txt", "backup content");
        Path uploads = temp.resolve("uploads-success");
        JsonBackupImporter importer = new JsonBackupImporter(users, categories, tags, articles, articleTags, uploads.toString(), jdbc);
        importer.importBackup(backup);
        assertEquals("backup content", Files.readString(uploads.resolve("nested/file.txt")));
    }

    @Test void uploadCopyFailurePropagatesInsteadOfReportingSuccess() throws Exception {
        Path backup = v3Zip("[]", "[]");
        addUpload(backup, "conflict", "backup content");
        Path uploads = temp.resolve("uploads-failure");
        Files.createDirectories(uploads.resolve("conflict"));
        Files.writeString(uploads.resolve("conflict/existing.txt"), "existing");
        JsonBackupImporter importer = new JsonBackupImporter(users, categories, tags, articles, articleTags, uploads.toString(), jdbc);
        assertThrows(IOException.class, () -> importer.importBackup(backup));
        assertEquals("existing", Files.readString(uploads.resolve("conflict/existing.txt")));
    }

    @Test void restoresBigintNoteVersionsWithoutTruncation() throws Exception {
        String template = "{\"id\":%d,\"workspace_id\":1,\"title\":\"n\",\"content\":\"\",\"version\":%s,"
                + "\"create_time\":\"2026-01-01T00:00:00\",\"update_time\":\"2026-01-01T00:00:00\"}";
        String notes = "[" + String.format(template, 1, "2147483648") + ","
                + String.format(template, 2, "9223372036854775807") + "]";
        try (var parsed = importer().parse(v3Zip("[]", notes))) { importer().restore(parsed); }
        var args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(2)).update(startsWith("INSERT INTO knowledge_note"), args.capture());
        assertEquals(2147483648L, args.getAllValues().get(0)[11]);
        assertEquals(Long.MAX_VALUE, args.getAllValues().get(1)[11]);
    }

    @Test void rejectsInvalidFolderGraphsAndFlagsBeforeAnyDatabaseWrites() throws Exception {
        String[] invalidFolders = {
                "[{\"workspace_id\":1}]",
                "[{\"id\":1,\"workspace_id\":1},{\"id\":1,\"workspace_id\":1}]",
                "[{\"id\":1,\"workspace_id\":1,\"parent_id\":2}]",
                "[{\"id\":1,\"workspace_id\":1,\"parent_id\":1}]",
                "[{\"id\":1,\"workspace_id\":1,\"parent_id\":2},{\"id\":2,\"workspace_id\":1,\"parent_id\":1}]",
                "[{\"id\":1,\"workspace_id\":1},{\"id\":2,\"workspace_id\":2,\"parent_id\":1}]",
                "[{\"id\":1.5,\"workspace_id\":1}]"
        };
        for (String folders : invalidFolders) {
            assertThrows(IOException.class, () -> importer().importBackup(v3Zip(folders, "[]")), folders);
        }
        for (String flag : new String[]{"2", "\"true\"", "1.5"}) {
            assertThrows(IOException.class, () -> importer().importBackup(v3Zip("[]", "[{\"favorite\":" + flag + "}]")));
        }
        for (String version : new String[]{"-1", "9223372036854775808", "1.5", "\"2\""}) {
            assertThrows(IOException.class, () -> importer().importBackup(v3Zip("[]", "[{\"version\":" + version + "}]")));
        }
        verifyNoInteractions(jdbc, users, categories, tags, articles, articleTags);
    }

    @Test void parseFailureCleansTempDirectoryAndRejectsBadManifest() throws Exception {
        Path backup = zip("data/article.json", "[{\"id\":1}]".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> importer().parse(backup));
        var tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (var entries = Files.list(tempRoot)) {
            assertTrue(entries.noneMatch(p -> p.getFileName().toString().startsWith("qblog-restore-")));
        }
    }
}
