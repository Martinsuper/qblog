package com.qblog.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class KnowledgeNoteServiceTest {
    private JdbcTemplate jdbc;
    private KnowledgeNoteService service;
    @BeforeEach void setup() { jdbc = mock(JdbcTemplate.class); service = spy(new KnowledgeNoteService(jdbc, new ObjectMapper())); }
    private NoteWrite write() { NoteWrite w = new NoteWrite(); w.setTitle("Title"); w.setContent("Content"); w.setSource("manual"); return w; }
    private NoteView existing() {
        NoteView v = new NoteView(); v.setId(1L); v.setTitle("Title"); v.setContent("Content"); v.setSummary("Content");
        v.setSource("manual"); v.setTags(List.of()); v.setFavorite(false); v.setVersion(4L); return v;
    }
    @Test void scopeHasExactFoldersAndIndependentInbox() {
        KnowledgeScope s = new KnowledgeScope(Set.of(2L), false, true);
        assertDoesNotThrow(() -> s.requireFolder(null)); assertDoesNotThrow(() -> s.requireFolder(2L));
        assertEquals(403, assertThrows(BusinessException.class, () -> s.requireFolder(3L)).getCode());
        assertThrows(BusinessException.class, () -> KnowledgeScope.folders(Set.of(2L)).requireFolder(null));
    }
    @Test void rejectsUnboundedPagesBeforeQuery() {
        NoteQuery q = new NoteQuery(); q.setSize(101);
        assertEquals(400, assertThrows(BusinessException.class, () -> service.list(q)).getCode()); verifyNoInteractions(jdbc);
    }
    @Test void rejectsConflictingInboxAndFolder() {
        NoteQuery q = new NoteQuery(); q.setInbox(true); q.setFolderId(2L);
        assertThrows(BusinessException.class, () -> service.list(q)); verifyNoInteractions(jdbc);
    }
    @Test void rejectsUntrustedSortBeforeQuery() {
        NoteQuery q = new NoteQuery(); q.setSort("title; DROP TABLE knowledge_note");
        assertEquals(400, assertThrows(BusinessException.class, () -> service.list(q)).getCode());
        verifyNoInteractions(jdbc);
    }
    @Test void titleSortUsesFixedSqlAndStableIdOrder() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        NoteQuery q = new NoteQuery(); q.setSort("title"); service.list(q);
        verify(jdbc).query(contains("ORDER BY title ASC,id ASC LIMIT ? OFFSET ?"), any(RowMapper.class), any(Object[].class));
    }
    @Test void bothSearchBoxesNarrowResultsAndOverlongLocalQueryIsRejected() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        NoteQuery q = new NoteQuery(); q.setSearch("knowledge"); q.setQuery("notes"); service.list(q);
        verify(jdbc).query(argThat(sql -> sql.contains("LOCATE(?,tags)") && sql.split("LOCATE\\(\\?,title\\)", -1).length == 3), any(RowMapper.class), any(Object[].class));
        clearInvocations(jdbc); q.setQuery("a".repeat(501));
        assertEquals(400, assertThrows(BusinessException.class, () -> service.list(q)).getCode());
        verifyNoInteractions(jdbc);
    }
    @Test void listNeverSelectsLargeContentAndSearchIsBound() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        NoteQuery q = new NoteQuery(); q.setSearch("%' OR 1=1 --");
        service.list(q, new KnowledgeScope(Set.of(2L), false, true));
        verify(jdbc).query(argThat(sql -> !sql.startsWith("SELECT content") && !sql.contains(" OR 1=1 --") && sql.contains("LOCATE(?,content)") && sql.contains("folder_id IN (?)")), any(RowMapper.class), any(Object[].class));
    }
    @Test void invalidInputRejectedAndOutOfScopeCannotCreate() {
        NoteWrite w = write(); w.setTitle(" ");
        assertEquals(400, assertThrows(BusinessException.class, () -> service.create(w)).getCode());
        w.setTitle("valid"); assertEquals(403, assertThrows(BusinessException.class, () -> service.create(w, KnowledgeScope.folders(Set.of(2L)))).getCode());
    }
    @Test void updateRequiresVersionAndImmutableIntegrationIdentity() {
        doReturn(existing()).when(service).get(1L, KnowledgeScope.all());
        NoteWrite w = write();
        assertEquals(409, assertThrows(BusinessException.class, () -> service.update(1L, w)).getCode());
        w.setVersion(3L); assertEquals(409, assertThrows(BusinessException.class, () -> service.update(1L, w)).getCode());
        w.setVersion(4L); w.setSourceTool("token:other"); w.setExternalId("forged");
        assertEquals(400, assertThrows(BusinessException.class, () -> service.update(1L, w)).getCode());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void conditionalUpdateZeroRowsBecomes409() {
        doReturn(existing()).when(service).get(1L, KnowledgeScope.all());
        NoteWrite w = write(); w.setVersion(4L);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.update(1L, w)).getCode());
        verify(jdbc).update(contains("AND version=?"), any(Object[].class));
    }
    @Test void deletionRequiresVersionAndIsSoft() {
        doReturn(existing()).when(service).get(1L, KnowledgeScope.all());
        assertEquals(400, assertThrows(BusinessException.class, () -> service.delete(1L, null)).getCode());
        assertEquals(409, assertThrows(BusinessException.class, () -> service.delete(1L, 3L)).getCode());
        verify(jdbc).update(contains("SET deleted=1,version=version+1"), eq(1L), eq(3L));
    }
    @Test void identicalMcpUpsertIsIdempotentButChangesNeedVersion() {
        NoteView old = existing(); old.setSource("mcp"); old.setSourceTool("token:9"); old.setExternalId("doc1");
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("token:9"), eq("doc1"))).thenReturn(List.of(1L));
        doReturn(old).when(service).get(1L, KnowledgeScope.all());
        NoteWrite w = write(); w.setSource("mcp"); w.setSourceTool("token:9"); w.setExternalId("doc1");
        assertSame(old, service.upsert(w, KnowledgeScope.all()));
        w.setContent("New manual-overwriting body");
        assertEquals(409, assertThrows(BusinessException.class, () -> service.upsert(w, KnowledgeScope.all())).getCode());
    }
}
