package com.qblog.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class JsonBackupImporterTest {
    @TempDir Path temp;
    private final JsonBackupImporter importer = new JsonBackupImporter(null, null, null, null, null);

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
        assertThrows(IOException.class, () -> importer.extractZip(zip("../outside.txt", new byte[]{1}), root));
        assertFalse(Files.exists(temp.resolve("outside.txt")));
    }

    @Test void rejectsAbsoluteAndWindowsPaths() throws Exception {
        Path root = Files.createDirectory(temp.resolve("extract"));
        for (String name : new String[]{temp.resolve("outside.txt").toString(), "C:/outside.txt", "..\\outside.txt"}) {
            assertThrows(IOException.class, () -> importer.extractZip(zip(name, new byte[]{1}), root));
        }
    }

    @Test void extractsNormalFile() throws Exception {
        Path root = Files.createDirectory(temp.resolve("extract"));
        importer.extractZip(zip("data/article.json", "[]".getBytes()), root);
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
        assertThrows(IOException.class, () -> importer.extractZip(input, root));
    }
}
