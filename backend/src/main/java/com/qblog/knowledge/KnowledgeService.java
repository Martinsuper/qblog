package com.qblog.knowledge;

import com.qblog.knowledge.KnowledgeModels.*;

/** Internal shared-workspace boundary. MCP callers MUST use scope-aware methods. */
public interface KnowledgeService {
    PageResult<NoteView> list(NoteQuery query, KnowledgeScope scope);
    NoteView get(Long id, KnowledgeScope scope);
    NoteView create(NoteWrite write, KnowledgeScope scope);
    NoteView update(Long id, NoteWrite write, KnowledgeScope scope);
    void delete(Long id, Long version, KnowledgeScope scope);
    NoteView upsert(NoteWrite write, KnowledgeScope scope);
}
