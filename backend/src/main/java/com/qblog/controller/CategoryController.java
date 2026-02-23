package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.entity.Category;
import com.qblog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     */
    @GetMapping
    public Result<List<Category>> getCategoryList() {
        return Result.success(categoryService.list());
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    public Result<Category> getCategoryDetail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /**
     * 创建分类
     */
    @PostMapping
    public Result<Category> createCategory(@RequestBody Category category) {
        categoryService.save(category);
        return Result.success(category);
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, 
                                           @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.success(category);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }
}
