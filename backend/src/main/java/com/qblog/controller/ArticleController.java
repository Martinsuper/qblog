package com.qblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qblog.common.Result;
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
     * 获取文章列表
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
        return Result.success(articleService.getArticleList(page, size, categoryId, tagId, keyword, sortBy, sortOrder));
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
    public Result<ArticleVO> createArticle(@Valid @RequestBody ArticleDTO articleDTO) {
        Article article = articleService.createArticle(articleDTO);
        ArticleVO vo = articleService.getArticleDetail(article.getId());
        return Result.success(vo);
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
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
        return Result.success(articleService.getHotArticles(limit));
    }

    /**
     * 获取最新文章
     */
    @GetMapping("/latest")
    public Result<List<ArticleListItemVO>> getLatestArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(articleService.getLatestArticles(limit));
    }

    /**
     * 点赞文章
     */
    @PostMapping("/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.success();
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlikeArticle(@PathVariable Long id) {
        articleService.unlikeArticle(id);
        return Result.success();
    }
}
