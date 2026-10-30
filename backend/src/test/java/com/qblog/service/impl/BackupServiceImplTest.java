package com.qblog.service.impl;

import com.qblog.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
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
                mock(JdbcTemplate.class),
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

    @Test void partialSettingsUpdatePreservesUneditedFields() throws Exception {
        Path directory = temp.resolve("backups");
        BackupServiceImpl service = service(directory);
        var settingsFile = directory.resolve("backup-settings.properties");
        Files.writeString(settingsFile, "enabled=true\nfrequency=daily\nhour=3\nminute=10\ndayOfWeek=2\nkeepCount=5\nbackupType=full\ncustom=preserved\n");
        var update = new com.qblog.model.dto.BackupSettingsDTO();
        update.setMinute(45);
        service.updateSettings(update);
        var settings = service.getSettings();
        assertEquals(45, settings.getMinute());
        assertEquals("full", settings.getBackupType());
        assertEquals(3, settings.getHour());
        assertTrue(Files.readString(settingsFile).contains("custom=preserved"));
    }

    @Test void settingsReadAndWriteFailuresAreReported() throws Exception {
        BackupServiceImpl service = service(temp.resolve("backups"));
        Path invalid = temp.resolve("missing").resolve("settings");
        ReflectionTestUtils.setField(service, "settingsFilePath", invalid);
        assertThrows(IllegalStateException.class, () -> service.updateSettings(new com.qblog.model.dto.BackupSettingsDTO()));
        ReflectionTestUtils.setField(service, "settingsFilePath", temp);
        assertThrows(IllegalStateException.class, service::getSettings);
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(service, "saveSettings", new java.util.Properties()));
    }

    @Test void importAcceptsUppercaseZipAtLimitAndRejectsOversizeBeforeTransfer() throws Exception {
        BackupServiceImpl service = service(temp.resolve("backups"));
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(file.getOriginalFilename()).thenReturn("backup.ZIP");
        org.mockito.Mockito.when(file.getSize()).thenReturn(100L * 1024 * 1024);
        org.mockito.Mockito.doThrow(new java.io.IOException("transfer reached"))
                .when(file).transferTo(org.mockito.ArgumentMatchers.any(Path.class));
        RuntimeException reached = assertThrows(RuntimeException.class, () -> service.importBackup(file));
        assertTrue(reached.getMessage().contains("transfer reached"));
        org.mockito.Mockito.reset(file);
        org.mockito.Mockito.when(file.getOriginalFilename()).thenReturn("backup.ZiP");
        org.mockito.Mockito.when(file.getSize()).thenReturn(100L * 1024 * 1024 + 1);
        assertThrows(IllegalArgumentException.class, () -> service.importBackup(file));
        org.mockito.Mockito.verify(file, org.mockito.Mockito.never()).transferTo(org.mockito.ArgumentMatchers.any(Path.class));
    }

    @Test void imageUploadStillRejectsFilesAboveFiveMib() {
        var controller = new com.qblog.controller.UploadController();
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(file.isEmpty()).thenReturn(false);
        org.mockito.Mockito.when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        var error = assertThrows(com.qblog.common.exception.BusinessException.class,
                () -> controller.uploadImage(file, new org.springframework.mock.web.MockHttpServletRequest()));
        assertTrue(error.getMessage().contains("5MB"));
        ReflectionTestUtils.setField(controller, "uploadDirectory", temp.resolve("uploads").toString());
        var atLimit = new org.springframework.mock.web.MockMultipartFile("file", "image.png", "image/png", new byte[5 * 1024 * 1024]);
        assertNotNull(controller.uploadImage(atLimit, new org.springframework.mock.web.MockHttpServletRequest()).getData());
    }

    @Test void refusesNonDirectoryBackupLocation() throws Exception {
        Path file = Files.writeString(temp.resolve("file"), "not a directory");
        assertThrows(IllegalStateException.class, () -> service(file));
    }
}
