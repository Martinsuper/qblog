package com.qblog.util;

import com.qblog.entity.Article;
import com.qblog.mapper.ArticleMapper;
import com.qblog.mapper.ArticleTagMapper;
import com.qblog.mapper.CategoryMapper;
import com.qblog.mapper.TagMapper;
import com.qblog.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private JsonBackupImporter importer() {
        return new JsonBackupImporter(users, categories, tags, articles, articleTags, "uploads");
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
        // 解析/恢复失败或结束后临时目录必须清理
        var tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (var entries = Files.list(tempRoot)) {
            assertTrue(entries.noneMatch(p -> p.getFileName().toString().startsWith("qblog-restore-")));
        }
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
