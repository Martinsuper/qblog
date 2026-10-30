package com.qblog.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import com.qblog.mapper.ArticleMapper;
import com.qblog.mapper.ArticleTagMapper;
import com.qblog.mapper.CategoryMapper;
import com.qblog.mapper.TagMapper;
import com.qblog.mapper.UserMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonBackupExporterTest {
    @TempDir Path temp;

    @Test void v3ZipContainsKnowledgeParitionsAndManifest() throws Exception {
        UserMapper users = mock(UserMapper.class);
        CategoryMapper categories = mock(CategoryMapper.class);
        TagMapper tags = mock(TagMapper.class);
        ArticleMapper articles = mock(ArticleMapper.class);
        ArticleTagMapper articleTags = mock(ArticleTagMapper.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // 知识表走 JdbcTemplate；缺省空列表即"无数据"快照
        when(jdbc.queryForList(contains("FROM knowledge_workspace"))).thenReturn(List.of(Map.of("id", 1L, "name", "私人工作区")));
        when(jdbc.queryForList(contains("FROM knowledge_folder"))).thenReturn(List.of());
        when(jdbc.queryForList(contains("FROM knowledge_note"))).thenReturn(List.of());
        when(jdbc.queryForObject(contains("knowledge_workspace"), eq(Long.class))).thenReturn(1L);
        when(jdbc.queryForObject(contains("knowledge_folder"), eq(Long.class))).thenReturn(0L);
        when(jdbc.queryForObject(contains("knowledge_note"), eq(Long.class))).thenReturn(0L);

        JsonBackupExporter exporter = new JsonBackupExporter(
                users, categories, tags, articles, articleTags, jdbc, temp.resolve("no-uploads").toString());
        Path zipPath = temp.resolve("v3.zip");
        exporter.exportBackup(zipPath, "测试备份");

        Map<String, String> entries = readZip(zipPath);
        assertTrue(entries.containsKey("manifest.json"), entries.keySet().toString());
        assertTrue(entries.containsKey("data/user.json"), entries.keySet().toString());
        assertTrue(entries.containsKey("data/knowledge_workspace.json"), entries.keySet().toString());
        assertTrue(entries.containsKey("data/knowledge_folder.json"), entries.keySet().toString());
        assertTrue(entries.containsKey("data/knowledge_note.json"), entries.keySet().toString());

        String manifest = entries.get("manifest.json");
        assertTrue(manifest.contains("\"version\" : \"3.0\""), manifest);
        assertTrue(manifest.contains("knowledge_note"), manifest);
        // INDENT_OUTPUT 下空数组序列化为 "[ ]"
        assertEquals("[]", entries.get("data/knowledge_folder.json").replaceAll("\\s", ""));
        assertTrue(entries.get("data/knowledge_workspace.json").contains("私人工作区"));
    }

    @Test void oversizedUploadFailsWithoutReplacingExistingBackup() throws Exception {
        Path uploads = Files.createDirectory(temp.resolve("uploads"));
        Path large = uploads.resolve("large.bin");
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(large.toFile(), "rw")) {
            file.setLength(64L * 1024 * 1024 + 1);
        }
        Path output = temp.resolve("existing.zip");
        Files.writeString(output, "existing backup");
        IOException error = assertThrows(IOException.class, () -> exporter(uploads).exportBackup(output, ""));
        assertTrue(error.getMessage().contains("64 MiB"));
        assertEquals("existing backup", Files.readString(output));
        try (var files = Files.list(temp)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".zip.tmp")));
        }
    }

    @Test void uploadCopyFailureIsReported() throws Exception {
        Path uploads = Files.createDirectory(temp.resolve("uploads"));
        Path source = Files.writeString(uploads.resolve("file.txt"), "contents");
        Path output = temp.resolve("backup.zip");
        try (var files = org.mockito.Mockito.mockStatic(Files.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.copy(eq(source), org.mockito.ArgumentMatchers.any(Path.class),
                    org.mockito.ArgumentMatchers.<java.nio.file.CopyOption>any()))
                    .thenThrow(new IOException("simulated copy failure"));
            IOException error = assertThrows(IOException.class, () -> exporter(uploads).exportBackup(output, ""));
            assertEquals("simulated copy failure", error.getMessage());
            assertFalse(Files.exists(output));
        }
    }

    @Test void uploadFilesAreIncluded() throws Exception {
        Path uploads = Files.createDirectory(temp.resolve("uploads"));
        Files.createDirectory(uploads.resolve("nested"));
        Files.writeString(uploads.resolve("nested/file.txt"), "hello");
        Path output = temp.resolve("backup.zip");
        exporter(uploads).exportBackup(output, "");
        assertEquals("hello", readZip(output).get("files/uploads/nested/file.txt"));
    }

    private JsonBackupExporter exporter(Path uploads) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.anyString(), eq(Long.class))).thenReturn(0L);
        return new JsonBackupExporter(mock(UserMapper.class), mock(CategoryMapper.class), mock(TagMapper.class),
                mock(ArticleMapper.class), mock(ArticleTagMapper.class), jdbc, uploads.toString());
    }

    private Map<String, String> readZip(Path zipPath) throws Exception {
        Map<String, String> entries = new java.util.LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
