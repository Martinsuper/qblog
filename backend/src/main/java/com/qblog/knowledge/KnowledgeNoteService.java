package com.qblog.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.common.exception.BusinessException;
import com.qblog.knowledge.KnowledgeModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

/** All SQL values are bind parameters; workspace is deliberately one private admin workspace. */
@Service @RequiredArgsConstructor
public class KnowledgeNoteService implements KnowledgeService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private static final String COLUMNS = "id,title,summary,folder_id,tags,favorite,source,source_tool,external_id,version,create_time,update_time";

    void lockWorkspace() { jdbc.queryForObject("SELECT id FROM knowledge_workspace WHERE id=1 FOR UPDATE", Long.class); }
    static BusinessException bad(String message) { return new BusinessException(400, message); }
    static BusinessException missing() { return new BusinessException(404, "笔记不存在或不在授权范围内"); }
    static BusinessException conflict() { return new BusinessException(409, "版本冲突，请重新读取后合并修改"); }
    static void positive(Long id) { if (id == null || id <= 0) throw bad("无效ID"); }
    void folderExists(Long id) {
        if (id != null && jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_folder WHERE workspace_id=1 AND id=?", Long.class, id) == 0)
            throw bad("目录不存在");
    }
    private RowMapper<NoteView> mapper(boolean content) {
        return (rs, n) -> {
            NoteView v = new NoteView();
            v.setId(rs.getLong("id")); v.setTitle(rs.getString("title"));
            v.setSummary(rs.getString("summary")); v.setFolderId(rs.getObject("folder_id", Long.class));
            try { v.setTags(json.readValue(rs.getString("tags"), new TypeReference<List<String>>() {})); }
            catch (Exception e) { throw new IllegalStateException("Invalid stored note tags", e); }
            v.setFavorite(rs.getBoolean("favorite")); v.setSource(rs.getString("source"));
            v.setSourceTool(rs.getString("source_tool")); v.setExternalId(rs.getString("external_id"));
            v.setVersion(rs.getLong("version"));
            v.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            v.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            if (content) v.setContent(rs.getString("content"));
            return v;
        };
    }
    @Override public PageResult<NoteView> list(NoteQuery q, KnowledgeScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (q == null) q = new NoteQuery();
        if (q.getCurrent() < 1 || q.getSize() < 1 || q.getSize() > 100 || q.getCurrent() > 1000000)
            throw bad("current须为1到1000000，size须为1到100");
        if (q.getFolderId() != null && Boolean.TRUE.equals(q.getInbox())) throw bad("folderId与inbox不可同时指定");
        if (q.getSort() == null) q.setSort("updated");
        if (!Set.of("updated", "title").contains(q.getSort())) throw bad("无效sort");
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE workspace_id=1 AND deleted=0");
        if (!scope.unrestricted()) {
            where.append(" AND (1=0");
            if (scope.inbox()) where.append(" OR folder_id IS NULL");
            if (!scope.folderIds().isEmpty()) {
                where.append(" OR folder_id IN (").append(String.join(",", Collections.nCopies(scope.folderIds().size(), "?"))).append(")");
                args.addAll(scope.folderIds());
            }
            where.append(")");
        }
        if (q.getFolderId() != null) { scope.requireFolder(q.getFolderId()); where.append(" AND folder_id=?"); args.add(q.getFolderId()); }
        if (Boolean.TRUE.equals(q.getInbox())) { scope.requireFolder(null); where.append(" AND folder_id IS NULL"); }
        if (Boolean.FALSE.equals(q.getInbox())) where.append(" AND folder_id IS NOT NULL");
        for (String search : new String[]{q.getSearch(), q.getQuery()}) {
            if (search == null || search.isBlank()) continue;
            if (search.length() > 500) throw bad("搜索词过长");
            // Both search boxes narrow the result. LOCATE keeps SQL metacharacters literal.
            for (String term : search.trim().split("\\s+")) {
                where.append(" AND (LOCATE(?,title)>0 OR LOCATE(?,content)>0 OR LOCATE(?,tags)>0)");
                args.add(term); args.add(term); args.add(term);
            }
        }
        if (q.getTag() != null && !q.getTag().isBlank()) {
            if (q.getTag().length() > 50) throw bad("标签过长");
            where.append(" AND JSON_CONTAINS(tags,JSON_QUOTE(?))"); args.add(q.getTag());
        }
        if (q.getSource() != null) { if (!Set.of("manual", "mcp").contains(q.getSource())) throw bad("无效source"); where.append(" AND source=?"); args.add(q.getSource()); }
        if (q.getFavorite() != null) { where.append(" AND favorite=?"); args.add(q.getFavorite()); }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_note" + where, Long.class, args.toArray());
        args.add(q.getSize()); args.add((q.getCurrent()-1)*q.getSize());
        String order = "title".equals(q.getSort()) ? "title ASC,id ASC" : "update_time DESC,id DESC";
        List<NoteView> records = jdbc.query("SELECT " + COLUMNS + " FROM knowledge_note" + where + " ORDER BY " + order + " LIMIT ? OFFSET ?", mapper(false), args.toArray());
        return new PageResult<>(records, total, q.getCurrent(), q.getSize());
    }
    @Override public NoteView get(Long id, KnowledgeScope scope) {
        positive(id); Objects.requireNonNull(scope, "scope");
        List<NoteView> rows = jdbc.query("SELECT " + COLUMNS + ",content FROM knowledge_note WHERE workspace_id=1 AND deleted=0 AND id=?", mapper(true), id);
        if (rows.isEmpty()) throw missing();
        NoteView v = rows.get(0);
        // Return the same 404 for absent/out-of-scope IDs, never reveal another folder.
        if (!scope.unrestricted() && (v.getFolderId() == null ? !scope.inbox() : !scope.folderIds().contains(v.getFolderId()))) throw missing();
        return v;
    }
    private void validate(NoteWrite w, KnowledgeScope scope) {
        if (w == null || w.getTitle() == null || w.getTitle().isBlank() || w.getTitle().length() > 200) throw bad("标题必填且最多200字符");
        if (w.getContent() == null) w.setContent("");
        if (w.getContent().length() > 2000000) throw bad("正文最多2000000字符");
        if (w.getSummary() != null && w.getSummary().length() > 500) throw bad("摘要最多500字符");
        if (w.getSummary() == null) w.setSummary(w.getContent().substring(0, Math.min(200, w.getContent().length())));
        if (w.getTags() == null) w.setTags(List.of());
        if (w.getTags().size() > 50 || w.getTags().stream().anyMatch(t -> t == null || t.isBlank() || t.length() > 50)) throw bad("最多50个非空标签，每个最多50字符");
        w.setTags(new ArrayList<>(new LinkedHashSet<>(w.getTags())));
        if (w.getSource() == null) w.setSource("manual");
        if (!Set.of("manual", "mcp").contains(w.getSource())) throw bad("source须为manual或mcp");
        if ((w.getSourceTool() == null) != (w.getExternalId() == null)) throw bad("sourceTool和externalId须同时提供");
        if (w.getSourceTool() != null && (w.getSourceTool().isBlank() || w.getSourceTool().length() > 100 || w.getExternalId().isBlank() || w.getExternalId().length() > 200)) throw bad("无效外部标识");
        scope.requireFolder(w.getFolderId()); folderExists(w.getFolderId());
        if (w.getFavorite() == null) w.setFavorite(false);
    }
    private String tags(NoteWrite w) {
        try { return json.writeValueAsString(w.getTags()); } catch (Exception e) { throw bad("无效tags"); }
    }
    @Override @Transactional public NoteView create(NoteWrite w, KnowledgeScope scope) {
        lockWorkspace(); validate(w, scope);
        if (w.getSourceTool() != null && jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_note WHERE workspace_id=1 AND source_tool=? AND external_id=?", Long.class, w.getSourceTool(), w.getExternalId()) > 0) throw conflict();
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement p = c.prepareStatement("INSERT INTO knowledge_note(workspace_id,title,content,summary,folder_id,tags,favorite,source,source_tool,external_id,version,deleted) VALUES(1,?,?,?,?,?,?,?,?,?,0,0)", Statement.RETURN_GENERATED_KEYS);
            Object[] values = {w.getTitle(), w.getContent(), w.getSummary(), w.getFolderId(), tags(w), w.getFavorite(), w.getSource(), w.getSourceTool(), w.getExternalId()};
            for (int i=0; i<values.length; i++) p.setObject(i+1, values[i]);
            return p;
        }, key);
        return get(Objects.requireNonNull(key.getKey()).longValue(), scope);
    }
    @Override @Transactional public NoteView update(Long id, NoteWrite w, KnowledgeScope scope) {
        lockWorkspace(); NoteView old = get(id, scope); validate(w, scope);
        if (w.getVersion() == null || !w.getVersion().equals(old.getVersion())) throw conflict();
        // Integration identity is immutable, including when a human edits MCP-created notes.
        if (!Objects.equals(old.getSourceTool(), w.getSourceTool()) || !Objects.equals(old.getExternalId(), w.getExternalId()) || !Objects.equals(old.getSource(), w.getSource())) throw bad("来源标识不可修改");
        int changed = jdbc.update("UPDATE knowledge_note SET title=?,content=?,summary=?,folder_id=?,tags=?,favorite=?,version=version+1,update_time=CURRENT_TIMESTAMP(6) WHERE workspace_id=1 AND id=? AND deleted=0 AND version=?", w.getTitle(), w.getContent(), w.getSummary(), w.getFolderId(), tags(w), w.getFavorite(), id, w.getVersion());
        if (changed != 1) throw conflict();
        return get(id, scope);
    }
    @Override @Transactional public void delete(Long id, Long version, KnowledgeScope scope) {
        lockWorkspace(); get(id, scope);
        if (version == null) throw bad("删除需要version");
        if (jdbc.update("UPDATE knowledge_note SET deleted=1,version=version+1,update_time=CURRENT_TIMESTAMP(6) WHERE workspace_id=1 AND id=? AND deleted=0 AND version=?", id, version) != 1) throw conflict();
    }
    @Override @Transactional public NoteView upsert(NoteWrite w, KnowledgeScope scope) {
        lockWorkspace(); validate(w, scope);
        if (!"mcp".equals(w.getSource()) || w.getSourceTool() == null) throw bad("upsert需要mcp来源与外部标识");
        List<Long> ids = jdbc.queryForList("SELECT id FROM knowledge_note WHERE workspace_id=1 AND source_tool=? AND external_id=?", Long.class, w.getSourceTool(), w.getExternalId());
        if (ids.isEmpty()) return create(w, scope);
        NoteView old = get(ids.get(0), scope); // Includes deleted tombstones: do not resurrect silently.
        if (Objects.equals(old.getTitle(), w.getTitle()) && Objects.equals(old.getContent(), w.getContent()) && Objects.equals(old.getSummary(), w.getSummary()) && Objects.equals(old.getFolderId(), w.getFolderId()) && Objects.equals(old.getTags(), w.getTags()) && Objects.equals(old.getFavorite(), w.getFavorite())) return old;
        return update(old.getId(), w, scope);
    }
    public PageResult<NoteView> list(NoteQuery q) { return list(q, KnowledgeScope.all()); }
    public NoteView get(Long id) { return get(id, KnowledgeScope.all()); }
    @Transactional public NoteView create(NoteWrite w) { return create(w, KnowledgeScope.all()); }
    @Transactional public NoteView update(Long id, NoteWrite w) { return update(id, w, KnowledgeScope.all()); }
    @Transactional public void delete(Long id, Long version) { delete(id, version, KnowledgeScope.all()); }
}
