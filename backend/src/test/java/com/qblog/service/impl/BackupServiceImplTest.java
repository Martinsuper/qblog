package com.qblog.service.impl;

import com.qblog.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BackupServiceImplTest {
    @TempDir Path temp;

    private BackupServiceImpl service(Path directory) {
        BackupServiceImpl service = new BackupServiceImpl(mock(UserMapper.class), mock(CategoryMapper.class),
                mock(TagMapper.class), mock(ArticleMapper.class), mock(ArticleTagMapper.class),
                new TransactionTemplate(mock(org.springframework.transaction.PlatformTransactionManager.class)));
        ReflectionTestUtils.setField(service, "backupDirectory", directory.toString());
        service.init();
        return service;
    }

    @Test void initializesConfiguredDirectoryAndSettings() {
        Path directory = temp.resolve("backups");
        service(directory);
        assertTrue(Files.isRegularFile(directory.resolve("backup-settings.properties")));
    }

    @Test void rejectsInvalidBackupIds() {
        BackupServiceImpl service = service(temp.resolve("backups"));
        assertThrows(IllegalArgumentException.class, () -> service.downloadBackup("../outside"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteBackup("/tmp/other"));
    }

    @Test void acceptsExistingLegacyBackupIds() throws Exception {
        Path directory = temp.resolve("backups");
        BackupServiceImpl service = service(directory);
        Files.write(directory.resolve("qblog-backup-20260227-210546.zip"), new byte[]{1, 2});
        var resource = service.downloadBackup("qblog-backup-20260227-210546");
        try (var is = resource.getInputStream()) {
            assertArrayEquals(new byte[]{1, 2}, is.readAllBytes());
        }
    }

    @Test void refusesNonDirectoryBackupLocation() throws Exception {
        Path file = Files.writeString(temp.resolve("file"), "not a directory");
        assertThrows(IllegalStateException.class, () -> service(file));
    }
}
