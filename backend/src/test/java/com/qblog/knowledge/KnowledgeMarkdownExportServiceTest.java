package com.qblog.knowledge;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeMarkdownExportServiceTest {

    private static Map<String, Object> folder(long id, Long parent, String name) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("parent_id", parent);
        row.put("name", name);
        return row;
    }

    private static Map<String, Object> note(long id, String title, String content, String tags, Object favorite, Long folderId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("title", title);
        row.put("content", content);
        row.put("tags", tags);
        row.put("favorite", favorite);
        row.put("folder_id", folderId);
        row.put("create_time", "2026-10-30T10:00:00");
        row.put("update_time", "2026-10-30T11:00:00");
        return row;
    }

    /** 读出 zip 内全部条目：路径 → UTF-8 文本 */
    private static Map<String, String> unzip(byte[] archive) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    @Test void filenameSanitizesAndRejectsBlank() {
        assertEquals("a-b-c-d", KnowledgeMarkdownExportService.sanitizeFilename("a/b\\c:d*?\"<>|"));
        assertEquals("无标题", KnowledgeMarkdownExportService.sanitizeFilename(" / "));
        assertEquals("无标题", KnowledgeMarkdownExportService.sanitizeFilename(null));
        assertEquals("中文名", KnowledgeMarkdownExportService.sanitizeFilename("中文名"));
        String longName = "长".repeat(150);
        assertEquals(100, KnowledgeMarkdownExportService.sanitizeFilename(longName).length());
    }

    @Test void folderPathJoinsAncestorsAndCycleTerminates() {
        var byId = Map.of(
                3L, folder(3, 2L, "Spring"),
                2L, folder(2, null, "Java"),
                9L, folder(9, 9L, "自引用"));
        assertEquals("Java/Spring", KnowledgeMarkdownExportService.folderPath(3L, byId, 0));
        // 父链成环的脏数据：不应栈溢出，最多截断到深度上限
        assertDoesNotThrow(() -> KnowledgeMarkdownExportService.folderPath(9L, byId, 0));
        // 目录缺失返回 null，调用方会落回收件箱
        assertNull(KnowledgeMarkdownExportService.folderPath(99L, byId, 0));
    }

    @Test void markdownHasFrontmatterAndBody() {
        var note = note(1L, "标题: 带冒号", "正文一段", "[\"Java\",\"Spring\"]", 1, 3L);
        String md = new String(KnowledgeMarkdownExportService.noteMarkdown(note, "Java/Spring"), StandardCharsets.UTF_8);
        assertTrue(md.startsWith("---\n"));
        assertTrue(md.contains("title: \"标题: 带冒号\""));
        assertTrue(md.contains("tags: [\"Java\", \"Spring\"]"));
        assertTrue(md.contains("folder: \"Java/Spring\""));
        assertTrue(md.contains("favorite: true"));
        assertTrue(md.contains("created: 2026-10-30T10:00:00"));
        // 正文不被 frontmatter 重复标题污染
        assertTrue(md.endsWith("---\n正文一段\n"));
        // 引号与反斜杠要被转义，防止破坏 YAML
        var escaped = note(1L, "引号\"和\\斜杠", "", "[]", 0, null);
        String esc = new String(KnowledgeMarkdownExportService.noteMarkdown(escaped, null), StandardCharsets.UTF_8);
        assertTrue(esc.contains("title: \"引号\\\"和\\\\斜杠\""));
        assertTrue(esc.contains("favorite: false"));
        assertFalse(esc.contains("folder:"));
    }

    @Test void frontmatterEscapesControlCharactersWithoutCreatingExtraFields() throws Exception {
        String value = "title\n---\r\nadmin: true\t\u0001";
        String quoted = KnowledgeMarkdownExportService.quote(value);
        assertFalse(quoted.contains("\n"));
        assertFalse(quoted.contains("\r"));
        assertEquals(value, new com.fasterxml.jackson.databind.ObjectMapper().readValue(quoted, String.class));
    }

    @Test void archiveOrganizesByFolderAndDeduplicates() throws Exception {
        var folders = List.of(folder(3, 2L, "Spring"), folder(2, null, "Java"));
        var notes = List.of(
                note(1L, "同题", "第二篇", "[]", 0, 3L),
                note(2L, "同题", "第一篇", "[]", 0, 3L),
                note(3L, "笔记/名", "正文", "[\"a\"]", 1, 2L),
                note(4L, "收件箱笔记", "内", "[]", 0, null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        KnowledgeMarkdownExportService.writeArchive(folders, notes, out);

        Map<String, String> entries = unzip(out.toByteArray());
        assertEquals(4, entries.size());
        // zip 内按目录层级组织；重名自动 (2)
        assertTrue(entries.containsKey("Java/Spring/同题.md"));
        assertEquals("---\ntitle: \"同题\"\n", entries.get("Java/Spring/同题.md").substring(0, 16));
        assertTrue(entries.containsKey("Java/Spring/同题 (2).md"), "同目录重名应追加序号：" + entries.keySet());
        assertTrue(entries.containsKey("Java/笔记-名.md"), "非法文件名字符应替换：" + entries.keySet());
        assertTrue(entries.containsKey("收件箱/收件箱笔记.md"));
        // 文件名路径相同但目录不同不冲突（同目录去重已经用 (2)）
        for (String name : entries.keySet()) {
            assertFalse(name.startsWith("/"));
        }
    }

    @Test void sameTitleInTwoFoldersDoesNotCollide() throws Exception {
        var folders = List.of(folder(2, null, "Java"));
        var notes = List.of(
                note(1L, "Java", "在收件箱", "[]", 0, null),
                note(2L, "Java", "在目录里", "[]", 0, 2L));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        KnowledgeMarkdownExportService.writeArchive(folders, notes, out);
        Map<String, String> entries = unzip(out.toByteArray());
        assertTrue(entries.containsKey("收件箱/Java.md"));
        assertTrue(entries.containsKey("Java/Java.md"));
    }
}
