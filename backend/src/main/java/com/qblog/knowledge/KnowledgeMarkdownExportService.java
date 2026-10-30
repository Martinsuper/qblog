package com.qblog.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 按目录层级把笔记导出为 Markdown 压缩包：每篇一个 .md，附 YAML frontmatter。
 * 已删除笔记不导出；图片引用原样保留（完整备份请用备份 zip）。收件箱笔记归入「收件箱/」。
 * Synchronous servlet output keeps the transaction on the request thread, like KnowledgeBackupService.
 */
@Service @RequiredArgsConstructor
public class KnowledgeMarkdownExportService {
    private final JdbcTemplate jdbc;
    private static final ObjectMapper JSON = new ObjectMapper();

    static final String INBOX_DIR = "收件箱";
    private static final int MAX_NAME_CHARS = 100;
    private static final int MAX_FOLDER_DEPTH = 64;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, rollbackFor = IOException.class)
    public void export(OutputStream output) throws IOException {
        List<Map<String, Object>> folders = jdbc.queryForList(
                "SELECT id,parent_id,name FROM knowledge_folder WHERE workspace_id=1");
        List<Map<String, Object>> notes = jdbc.queryForList(
                "SELECT id,title,content,tags,favorite,folder_id,create_time,update_time FROM knowledge_note WHERE workspace_id=1 AND deleted=0 ORDER BY id");
        writeArchive(folders, notes, output);
    }

    /** 核心：把目录行与笔记行写成 zip。包可见以便单测（纯函数，无 IO 依赖）。 */
    static void writeArchive(List<Map<String, Object>> folders, List<Map<String, Object>> notes,
                             OutputStream output) throws IOException {
        Map<Long, Map<String, Object>> foldersById = new HashMap<>();
        for (Map<String, Object> folder : folders) foldersById.put(asLong(folder.get("id")), folder);

        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            Set<String> usedEntries = new HashSet<>();
            Map<String, Set<String>> usedNamesByDir = new HashMap<>();
            for (Map<String, Object> note : notes) {
                Long folderId = nullableLong(note.get("folder_id"));
                String dir = folderId == null ? INBOX_DIR : folderPath(folderId, foldersById, 0);
                dir = dir == null ? INBOX_DIR : dir;
                String base = sanitizeFilename(text(note.get("title")));
                String entryName = uniqueEntry(dir, base, usedNamesByDir, usedEntries);
                zip.putNextEntry(new ZipEntry(entryName));
                byte[] bytes = noteMarkdown(note, dir);
                zip.write(bytes, 0, bytes.length);
                zip.closeEntry();
            }
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    /** 生成一篇笔记的 Markdown：frontmatter + 正文（标题已由文件名与 frontmatter 承载，不再重复 H1） */
    static byte[] noteMarkdown(Map<String, Object> note, String folderPath) {
        StringBuilder md = new StringBuilder("---\n");
        md.append("title: ").append(quote(text(note.get("title")))).append('\n');
        List<String> tags = tagsOf(note.get("tags"));
        md.append("tags: [");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) md.append(", ");
            md.append(quote(tags.get(i)));
        }
        md.append("]\n");
        if (folderPath != null) md.append("folder: ").append(quote(folderPath)).append('\n');
        md.append("favorite: ").append(truthy(note.get("favorite"))).append('\n');
        md.append("created: ").append(text(note.get("create_time"))).append('\n');
        md.append("updated: ").append(text(note.get("update_time"))).append('\n');
        md.append("---\n");
        String content = text(note.get("content"));
        if (!content.isEmpty() && !content.endsWith("\n")) content += "\n";
        md.append(content);
        return md.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 目录的完整路径（以 / 连接，顶级目录为路径根）。目录缺失返回 null（调用方落回收件箱）；
     * parent 链成环则按顶级截断，导出不因脏数据挂死。 */
    static String folderPath(Long folderId, Map<Long, Map<String, Object>> foldersById, int depth) {
        Map<String, Object> folder = foldersById.get(folderId);
        if (folder == null) return null;
        Long parent = nullableLong(folder.get("parent_id"));
        String self = sanitizeFilename(text(folder.get("name")));
        if (parent == null || depth > MAX_FOLDER_DEPTH) return self;
        String parentPath = folderPath(parent, foldersById, depth + 1);
        return parentPath == null ? self : parentPath + "/" + self;
    }

    /** Windows/zip 非法字符与控制符替换掉（连续非法字符合并为一个 -，两端空白与 - 修剪）；空标题回退「无标题」 */
    static String sanitizeFilename(String name) {
        String safe = name == null ? ""
                : name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]+", "-").replaceAll("^[\\s-]+|[\\s-]+$", "");
        if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) safe = "无标题";
        if (safe.length() > MAX_NAME_CHARS) safe = safe.substring(0, MAX_NAME_CHARS);
        return safe;
    }

    /** 同一目录内重名追加 (2)、(3)…；全网路径兜底防「目录名与收件箱同名」一类冲突 */
    static String uniqueEntry(String dir, String base, Map<String, Set<String>> usedNamesByDir, Set<String> usedEntries) {
        Set<String> usedNames = usedNamesByDir.computeIfAbsent(dir, key -> new HashSet<>());
        String unique = base;
        int n = 2;
        while (!usedNames.add(unique) || !usedEntries.add(dir + "/" + unique + ".md")) {
            unique = base + " (" + n++ + ")";
        }
        return dir + "/" + unique + ".md";
    }

    /** tags 列是 JSON 数组字符串；解析失败按无标签处理（CHECK 约束正常应保证合法） */
    static List<String> tagsOf(Object value) {
        List<String> tags = new ArrayList<>();
        try {
            if (value instanceof String text) {
                for (var node : new ObjectMapper().readTree(text)) tags.add(node.isNull() ? "" : node.asText());
            } else if (value instanceof List<?> list) {
                for (Object item : list) tags.add(item == null ? "" : item.toString());
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return tags;
    }

    /** 值恒以双引号包裹，免去 YAML 特殊字符的分支处理 */
    static String quote(String value) {
        try {
            // JSON quoted strings are valid YAML double-quoted scalars, including control escapes.
            return JSON.writeValueAsString(value == null ? "" : value);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        return false;
    }

    private static String text(Object value) {
        if (value instanceof String s) return s;
        return value == null ? "" : isoTime(value);
    }

    private static String isoTime(Object value) {
        if (value instanceof Timestamp time) return time.toLocalDateTime().toString();
        if (value instanceof LocalDateTime time) return time.toString();
        return value == null ? "" : value.toString();
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        throw new IllegalArgumentException("目录行缺少整数 id: " + value);
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : asLong(value);
    }
}
