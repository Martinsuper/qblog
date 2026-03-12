package com.qblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qblog.common.Result;
import com.qblog.common.annotation.RateLimit;
import com.qblog.entity.Article;
import com.qblog.model.dto.ArticleDTO;
import com.qblog.model.vo.ArticleListItemVO;
import com.qblog.model.vo.ArticleVO;
import com.qblog.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章控制器
 */
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 获取文章列表（公开接口，只返回已发布文章）
     */
    @GetMapping
    public Result<Page<ArticleListItemVO>> getArticleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "publishTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        // 参数验证
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // 限制最大页面大小
        return Result.success(articleService.getArticleList(page, size, categoryId, tagId, keyword, sortBy, sortOrder));
    }

    /**
     * 管理后台 - 获取所有文章（包括草稿）
     */
    @GetMapping("/admin/list")
    public Result<Page<ArticleListItemVO>> getAdminArticleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        // 参数验证
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // 限制最大页面大小
        return Result.success(articleService.getAdminArticleList(page, size, categoryId, keyword, status, sortBy, sortOrder));
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{id}")
    public Result<ArticleVO> getArticleDetail(@PathVariable Long id) {
        return Result.success(articleService.getArticleDetail(id));
    }

    /**
     * 创建文章
     */
    @PostMapping
    @RateLimit(key = "create_article", period = 60, count = 10, limitType = RateLimit.LimitType.USER)
    public Result<ArticleVO> createArticle(@Valid @RequestBody ArticleDTO articleDTO) {
        Article article = articleService.createArticle(articleDTO);
        ArticleVO vo = articleService.getArticleDetail(article.getId());
        return Result.success(vo);
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    @RateLimit(key = "update_article", period = 60, count = 20, limitType = RateLimit.LimitType.USER)
    public Result<ArticleVO> updateArticle(@PathVariable Long id,
                                           @Valid @RequestBody ArticleDTO articleDTO) {
        Article article = articleService.updateArticle(id, articleDTO);
        ArticleVO vo = articleService.getArticleDetail(id);
        return Result.success(vo);
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    /**
     * 获取热门文章
     */
    @GetMapping("/hot")
    public Result<List<ArticleListItemVO>> getHotArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        // 参数验证
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50; // 限制最大数量
        return Result.success(articleService.getHotArticles(limit));
    }

    /**
     * 获取最新文章
     */
    @GetMapping("/latest")
    public Result<List<ArticleListItemVO>> getLatestArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        // 参数验证
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50; // 限制最大数量
        return Result.success(articleService.getLatestArticles(limit));
    }

    /**
     * 获取相关文章（基于共同标签）
     */
    @GetMapping("/{id}/related")
    public Result<List<ArticleListItemVO>> getRelatedArticles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") Integer limit) {
        // 参数验证
        if (limit < 1) limit = 5;
        if (limit > 20) limit = 20; // 限制最大数量
        return Result.success(articleService.getRelatedArticles(id, limit));
    }
}
