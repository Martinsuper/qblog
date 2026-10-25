package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.entity.Category;
import com.qblog.model.vo.CategoryVO;
import com.qblog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表（含文章数量）
     */
    @Operation(summary = "获取分类列表（含文章数量）")
    @GetMapping
    public Result<List<CategoryVO>> getCategoryList() {
        return Result.success(categoryService.listWithArticleCount());
    }

    /**
     * 获取分类详情
     */
    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<Category> getCategoryDetail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /**
     * 创建分类
     */
    @Operation(summary = "创建分类（需管理员）")
    @PostMapping
    public Result<Category> createCategory(@RequestBody Category category) {
        categoryService.save(category);
        return Result.success(category);
    }

    /**
     * 更新分类
     */
    @Operation(summary = "更新分类（需管理员）")
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
    @Operation(summary = "删除分类（需管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }

    /**
     * 批量更新分类排序
     */
    @Operation(summary = "批量更新分类排序（需管理员）")
    @PutMapping("/sort")
    public Result<Void> updateCategorySort(@RequestBody List<Long> ids) {
        categoryService.updateBatchSort(ids);
        return Result.success();
    }
}
