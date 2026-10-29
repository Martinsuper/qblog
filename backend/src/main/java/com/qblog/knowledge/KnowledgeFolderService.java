package com.qblog.knowledge;

import com.qblog.common.exception.BusinessException;
import com.qblog.knowledge.KnowledgeModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Service @RequiredArgsConstructor
public class KnowledgeFolderService {
    private final JdbcTemplate jdbc;
    private final KnowledgeNoteService notes;
    /** At most 100 folders in this single workspace. A full tree is always returned. */
    public List<FolderView> list() {
        List<FolderView> all = jdbc.query("SELECT f.*, (SELECT COUNT(*) FROM knowledge_note n WHERE n.folder_id=f.id AND n.workspace_id=1 AND n.deleted=0) AS note_count FROM knowledge_folder f WHERE f.workspace_id=1 ORDER BY f.sort,f.id LIMIT 101", (rs, row) -> {
            FolderView v = new FolderView(); v.setId(rs.getLong("id")); v.setName(rs.getString("name"));
            v.setParentId(rs.getObject("parent_id", Long.class)); v.setSort(rs.getInt("sort")); v.setCount(rs.getLong("note_count"));
            v.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime()); v.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            return v;
        });
        if (all.size() > 100) throw new BusinessException(409, "目录数量超过100，请管理员整理后重试");
        Map<Long, FolderView> index = new LinkedHashMap<>(); all.forEach(f -> index.put(f.getId(), f));
        List<FolderView> roots = new ArrayList<>();
        for (FolderView f : all) {
            FolderView parent = index.get(f.getParentId());
            if (parent == null) roots.add(f); else parent.getChildren().add(f);
        }
        return roots;
    }
    private void validate(FolderWrite w) {
        if (w == null || w.getName() == null || w.getName().isBlank() || w.getName().length() > 100) throw KnowledgeNoteService.bad("目录名必填且最多100字符");
        if (w.getSort() == null) w.setSort(0);
        notes.folderExists(w.getParentId());
    }
    private FolderView get(Long id) {
        List<FolderView> queue = new ArrayList<>(list());
        for (int i=0; i<queue.size(); i++) { FolderView f = queue.get(i); if (f.getId().equals(id)) return f; queue.addAll(f.getChildren()); }
        throw new BusinessException(404, "目录不存在");
    }
    @Transactional public FolderView create(FolderWrite w) {
        notes.lockWorkspace(); validate(w);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_folder WHERE workspace_id=1", Long.class) >= 100) throw KnowledgeNoteService.bad("最多100个目录");
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement p = c.prepareStatement("INSERT INTO knowledge_folder(workspace_id,name,parent_id,sort) VALUES(1,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            p.setString(1, w.getName()); p.setObject(2, w.getParentId()); p.setInt(3, w.getSort()); return p;
        }, key);
        return get(Objects.requireNonNull(key.getKey()).longValue());
    }
    @Transactional public FolderView update(Long id, FolderWrite w) {
        notes.lockWorkspace(); KnowledgeNoteService.positive(id); get(id); validate(w);
        Long cursor = w.getParentId(); Set<Long> visited = new HashSet<>();
        while (cursor != null) {
            if (cursor.equals(id) || !visited.add(cursor)) throw KnowledgeNoteService.bad("目录不能形成循环");
            List<Long> parent = jdbc.query("SELECT parent_id FROM knowledge_folder WHERE workspace_id=1 AND id=?", (rs, n) -> rs.getObject(1, Long.class), cursor);
            if (parent.isEmpty()) throw KnowledgeNoteService.bad("父目录不存在");
            cursor = parent.get(0);
        }
        jdbc.update("UPDATE knowledge_folder SET name=?,parent_id=?,sort=?,update_time=CURRENT_TIMESTAMP(6) WHERE workspace_id=1 AND id=?", w.getName(), w.getParentId(), w.getSort(), id);
        return get(id);
    }
    @Transactional public void delete(Long id) {
        notes.lockWorkspace(); KnowledgeNoteService.positive(id); get(id);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_folder WHERE workspace_id=1 AND parent_id=?", Long.class, id) > 0 || jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_note WHERE workspace_id=1 AND deleted=0 AND folder_id=?", Long.class, id) > 0)
            throw new BusinessException(409, "目录含子目录或笔记，不能删除");
        // Deleted notes keep their integration tombstones, but no longer retain a dead folder FK.
        jdbc.update("UPDATE knowledge_note SET folder_id=NULL WHERE workspace_id=1 AND deleted=1 AND folder_id=?", id);
        jdbc.update("DELETE FROM knowledge_folder WHERE workspace_id=1 AND id=?", id);
    }
}
