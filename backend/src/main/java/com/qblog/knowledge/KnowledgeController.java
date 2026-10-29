package com.qblog.knowledge;

import com.qblog.common.Result;
import com.qblog.common.SecurityUtils;
import com.qblog.common.exception.BusinessException;
import com.qblog.knowledge.KnowledgeModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/knowledge") @RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeNoteService notes;
    private final KnowledgeFolderService folders;
    private void admin() { if (!SecurityUtils.isAdmin()) throw new BusinessException(403, "仅管理员可访问私人知识库"); }
    @GetMapping("/notes") public Result<PageResult<NoteView>> list(@ModelAttribute NoteQuery q) { admin(); return Result.success(notes.list(q)); }
    @GetMapping("/notes/{id}") public Result<NoteView> get(@PathVariable Long id) { admin(); return Result.success(notes.get(id)); }
    @PostMapping("/notes") public Result<NoteView> create(@RequestBody NoteWrite w) {
        admin(); if (w == null) throw KnowledgeNoteService.bad("请求正文不可为空");
        if (w.getSourceTool() != null || w.getExternalId() != null || (w.getSource() != null && !"manual".equals(w.getSource()))) throw KnowledgeNoteService.bad("浏览器创建仅支持manual来源，不可提供外部标识");
        return Result.success(notes.create(w));
    }
    @PutMapping("/notes/{id}") public Result<NoteView> update(@PathVariable Long id, @RequestBody NoteWrite w) {
        admin(); if (w == null) throw KnowledgeNoteService.bad("请求正文不可为空"); NoteView old = notes.get(id);
        if ((w.getSourceTool() != null && !w.getSourceTool().equals(old.getSourceTool())) || (w.getExternalId() != null && !w.getExternalId().equals(old.getExternalId())) || (w.getSource() != null && !w.getSource().equals(old.getSource()))) throw KnowledgeNoteService.bad("来源标识不可修改");
        w.setSource(old.getSource()); w.setSourceTool(old.getSourceTool()); w.setExternalId(old.getExternalId());
        return Result.success(notes.update(id, w));
    }
    @DeleteMapping("/notes/{id}") public Result<Void> delete(@PathVariable Long id, @RequestParam Long version) { admin(); notes.delete(id, version); return Result.success(); }
    @GetMapping("/folders") public Result<List<FolderView>> folders() { admin(); return Result.success(folders.list()); }
    @PostMapping("/folders") public Result<FolderView> createFolder(@RequestBody FolderWrite w) { admin(); return Result.success(folders.create(w)); }
    @PutMapping("/folders/{id}") public Result<FolderView> updateFolder(@PathVariable Long id, @RequestBody FolderWrite w) { admin(); return Result.success(folders.update(id, w)); }
    @DeleteMapping("/folders/{id}") public Result<Void> deleteFolder(@PathVariable Long id) { admin(); folders.delete(id); return Result.success(); }
}
