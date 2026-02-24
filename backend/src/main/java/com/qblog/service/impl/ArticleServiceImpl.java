package com.qblog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.Article;
import com.qblog.entity.ArticleTag;
import com.qblog.entity.Category;
import com.qblog.entity.Tag;
import com.qblog.entity.User;
import com.qblog.mapper.ArticleMapper;
import com.qblog.model.dto.ArticleDTO;
import com.qblog.model.vo.ArticleListItemVO;
import com.qblog.model.vo.ArticleVO;
import com.qblog.model.vo.CategoryVO;
import com.qblog.model.vo.TagVO;
import com.qblog.model.vo.UserVO;
import com.qblog.service.ArticleService;
import com.qblog.service.ArticleTagService;
import com.qblog.service.CategoryService;
import com.qblog.service.TagService;
import com.qblog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章服务实现
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final ArticleTagService articleTagService;

    @Override
    public Page<ArticleListItemVO> getArticleList(Integer page, Integer size, Long categoryId,
                                                   Long tagId, String keyword, String sortBy, String sortOrder) {
        Page<Article> articlePage = new Page<>(page, size);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        // 默认只查询已发布的文章
        wrapper.eq(Article::getStatus, 1);

        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getSummary, keyword));
        }
        
        // 排序
        if (StrUtil.isNotBlank(sortBy)) {
            String column = getSortColumn(sortBy);
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.last("ORDER BY " + column + " ASC");
            } else {
                wrapper.last("ORDER BY " + column + " DESC");
            }
        } else {
            wrapper.orderByDesc(true, Article::getTop);
            wrapper.orderByDesc(true, Article::getPublishTime);
        }
        
        Page<Article> resultPage = page(articlePage, wrapper);
        
        // 转换为 VO
        Page<ArticleListItemVO> voPage = new Page<>();
        voPage.setTotal(resultPage.getTotal());
        voPage.setRecords(convertToListItemVO(resultPage.getRecords()));
        
        return voPage;
    }

    @Override
    public Page<ArticleListItemVO> getAdminArticleList(Integer page, Integer size, Long categoryId,
                                                        String keyword, Integer status, String sortBy, String sortOrder) {
        Page<Article> articlePage = new Page<>(page, size);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        // 管理员可以选择查看特定状态的文章，不传则查看除已删除外的全部文章
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        } else {
            // 默认排除已删除的文章（status=2）
            wrapper.ne(Article::getStatus, 2);
        }

        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getSummary, keyword));
        }

        // 排序
        if (StrUtil.isNotBlank(sortBy)) {
            String column = getSortColumn(sortBy);
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.last("ORDER BY " + column + " ASC");
            } else {
                wrapper.last("ORDER BY " + column + " DESC");
            }
        } else {
            wrapper.orderByDesc(true, Article::getCreateTime);
        }

        Page<Article> resultPage = page(articlePage, wrapper);

        // 转换为 VO - 手动设置总数，解决 MyBatis-Plus 分页计数问题
        Page<ArticleListItemVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize());
        // 使用 count 查询获取总数
        Long total = count(wrapper);
        voPage.setTotal(total);
        voPage.setRecords(convertToListItemVO(resultPage.getRecords()));

        return voPage;
    }

    @Override
    public ArticleVO getArticleDetail(Long id) {
        Article article = getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        
        // 只检查已删除的文章（status=2），允许访问草稿（status=0）和已发布（status=1）
        if (article.getStatus() == 2) {
            throw new RuntimeException("文章不存在");
        }

        // 增加浏览量（仅已发布文章）
        if (article.getStatus() == 1) {
            article.setViewCount(article.getViewCount() + 1);
            updateById(article);
        }

        ArticleVO vo = BeanUtil.copyProperties(article, ArticleVO.class);

        // 填充作者信息
        if (article.getAuthorId() != null) {
            User author = userService.getById(article.getAuthorId());
            if (author != null) {
                UserVO authorVO = new UserVO();
                BeanUtil.copyProperties(author, authorVO);
                vo.setAuthor(authorVO);
            }
        }

        // 填充分类信息
        if (article.getCategoryId() != null) {
            Category category = categoryService.getById(article.getCategoryId());
            if (category != null) {
                CategoryVO categoryVO = new CategoryVO();
                BeanUtil.copyProperties(category, categoryVO);
                vo.setCategory(categoryVO);
            }
        }

        // 填充标签信息
        if (article.getId() != null) {
            List<ArticleTag> articleTags = articleTagService.list(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId())
            );
            if (!articleTags.isEmpty()) {
                List<Long> tagIds = articleTags.stream()
                    .map(ArticleTag::getTagId)
                    .collect(Collectors.toList());
                List<Tag> tags = tagService.listByIds(tagIds);
                List<TagVO> tagVOs = tags.stream().map(tag -> {
                    TagVO tagVO = new TagVO();
                    BeanUtil.copyProperties(tag, tagVO);
                    return tagVO;
                }).collect(Collectors.toList());
                vo.setTags(tagVOs);
            }
        }

        // TODO: 填充是否点赞、收藏状态

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article createArticle(ArticleDTO articleDTO) {
        Article article = BeanUtil.copyProperties(articleDTO, Article.class);
        
        // 设置作者 ID（从当前登录用户获取）
        // article.setAuthorId(getCurrentUserId());
        article.setAuthorId(1L);
        
        if (articleDTO.getStatus() == 1) {
            article.setPublishTime(LocalDateTime.now());
        }
        
        save(article);
        
        // TODO: 处理文章标签关联
        
        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article updateArticle(Long id, ArticleDTO articleDTO) {
        Article article = getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        
        BeanUtil.copyProperties(articleDTO, article);
        
        if (articleDTO.getStatus() == 1 && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        
        updateById(article);
        
        // TODO: 处理文章标签关联
        
        return article;
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = getById(id);
        if (article != null) {
            article.setStatus(2); // 标记为已删除
            updateById(article);
        }
    }

    @Override
    public List<ArticleListItemVO> getHotArticles(Integer limit) {
        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT " + limit));
        return convertToListItemVO(articles);
    }

    @Override
    public List<ArticleListItemVO> getLatestArticles(Integer limit) {
        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + limit));
        return convertToListItemVO(articles);
    }

    @Override
    public void likeArticle(Long id) {
        // TODO: 实现点赞逻辑
    }

    @Override
    public void unlikeArticle(Long id) {
        // TODO: 实现取消点赞逻辑
    }

    private List<ArticleListItemVO> convertToListItemVO(List<Article> articles) {
        List<ArticleListItemVO> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Article article : articles) {
            ArticleListItemVO vo = BeanUtil.copyProperties(article, ArticleListItemVO.class);

            // 设置时间格式化
            if (article.getCreateTime() != null) {
                vo.setCreateTime(article.getCreateTime().format(formatter));
            }
            if (article.getPublishTime() != null) {
                vo.setPublishTime(article.getPublishTime().format(formatter));
            }

            // TODO: 填充作者、分类、标签信息
            vo.setAuthor(new UserVO());
            vo.setCategory(new CategoryVO());
            vo.setTags(new ArrayList<>());

            list.add(vo);
        }
        return list;
    }

    private String getSortColumn(String sortBy) {
        // 简单的字段映射，防止 SQL 注入
        return switch (sortBy) {
            case "createTime" -> "create_time";
            case "viewCount" -> "view_count";
            case "likeCount" -> "like_count";
            default -> "publish_time";
        };
    }
}
