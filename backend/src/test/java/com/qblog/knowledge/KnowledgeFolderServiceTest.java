package com.qblog.knowledge;

import com.qblog.common.exception.BusinessException;
import com.qblog.knowledge.KnowledgeModels.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class KnowledgeFolderServiceTest {
    JdbcTemplate jdbc; KnowledgeNoteService notes; KnowledgeFolderService folders;
    @BeforeEach void setup() {
        jdbc = mock(JdbcTemplate.class); notes = mock(KnowledgeNoteService.class); folders = new KnowledgeFolderService(jdbc, notes);
    }
    FolderView folder(long id, Long parent) { FolderView f = new FolderView(); f.setId(id); f.setParentId(parent); f.setName("Folder"); return f; }
    void tree() { when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(i -> List.of(folder(1L, null), folder(2L, 1L))); }
    @Test void treeHasCountsAndChildren() { tree(); List<FolderView> roots = folders.list(); assertEquals(1, roots.size()); assertEquals(2L, roots.get(0).getChildren().get(0).getId()); }
    @Test void selfParentIsRejected() {
        tree(); FolderWrite w = new FolderWrite(); w.setName("Root"); w.setParentId(1L);
        assertEquals(400, assertThrows(BusinessException.class, () -> folders.update(1L, w)).getCode());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void movingIntoDescendantIsRejected() {
        tree(); FolderWrite w = new FolderWrite(); w.setName("Root"); w.setParentId(2L);
        when(jdbc.query(contains("SELECT parent_id"), any(RowMapper.class), eq(2L))).thenReturn(List.of(1L));
        assertEquals(400, assertThrows(BusinessException.class, () -> folders.update(1L, w)).getCode());
    }
    @Test void deletingNonEmptyFolderIsConflict() {
        tree(); when(jdbc.queryForObject(contains("parent_id=?"), eq(Long.class), eq(1L))).thenReturn(1L);
        assertEquals(409, assertThrows(BusinessException.class, () -> folders.delete(1L)).getCode());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void hundredFolderLimitCheckedUnderWorkspaceLock() {
        FolderWrite w = new FolderWrite(); w.setName("New");
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(100L);
        assertEquals(400, assertThrows(BusinessException.class, () -> folders.create(w)).getCode()); verify(notes).lockWorkspace();
    }
}
