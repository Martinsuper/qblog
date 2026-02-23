package com.qblog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qblog.entity.Article;
import com.qblog.model.dto.ArticleDTO;
import com.qblog.model.vo.ArticleListItemVO;
import com.qblog.model.vo.ArticleVO;

import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页获取文章列表
     */
    Page<ArticleListItemVO> getArticleList(Integer page, Integer size, Long categoryId, 
                                           Long tagId, String keyword, String sortBy, String sortOrder);

    /**
     * 获取文章详情
     */
    ArticleVO getArticleDetail(Long id);

    /**
     * 创建文章
     */
    Article createArticle(ArticleDTO articleDTO);

    /**
     * 更新文章
     */
    Article updateArticle(Long id, ArticleDTO articleDTO);

    /**
     * 删除文章
     */
    void deleteArticle(Long id);

    /**
     * 获取热门文章
     */
    List<ArticleListItemVO> getHotArticles(Integer limit);

    /**
     * 获取最新文章
     */
    List<ArticleListItemVO> getLatestArticles(Integer limit);

    /**
     * 点赞文章
     */
    void likeArticle(Long id);

    /**
     * 取消点赞
     */
    void unlikeArticle(Long id);
}
