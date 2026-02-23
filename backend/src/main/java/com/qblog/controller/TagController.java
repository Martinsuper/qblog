package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.entity.Tag;
import com.qblog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取标签列表
     */
    @GetMapping
    public Result<List<Tag>> getTagList() {
        return Result.success(tagService.list());
    }

    /**
     * 创建标签
     */
    @PostMapping
    public Result<Tag> createTag(@RequestBody Tag tag) {
        tagService.save(tag);
        return Result.success(tag);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.removeById(id);
        return Result.success();
    }
}
