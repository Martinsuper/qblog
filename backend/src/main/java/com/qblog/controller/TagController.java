package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.entity.Tag;
import com.qblog.model.vo.TagVO;
import com.qblog.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理")
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取标签列表（含文章数量）
     */
    @Operation(summary = "获取标签列表（含文章数量）")
    @GetMapping
    public Result<List<TagVO>> getTagList() {
        return Result.success(tagService.listWithArticleCount());
    }

    /**
     * 获取标签详情
     */
    @Operation(summary = "获取标签详情")
    @GetMapping("/{id}")
    public Result<Tag> getTagDetail(@PathVariable Long id) {
        return Result.success(tagService.getById(id));
    }

    /**
     * 创建标签
     */
    @Operation(summary = "创建标签（需管理员）")
    @PostMapping
    public Result<Tag> createTag(@RequestBody Tag tag) {
        tagService.save(tag);
        return Result.success(tag);
    }

    /**
     * 更新标签
     */
    @Operation(summary = "更新标签（需管理员）")
    @PutMapping("/{id}")
    public Result<Tag> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        tag.setId(id);
        tagService.updateById(tag);
        return Result.success(tag);
    }

    /**
     * 删除标签
     */
    @Operation(summary = "删除标签（需管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTagWithRelations(id);
        return Result.success();
    }
}
