package com.qblog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.common.exception.ResourceNotFoundException;
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
import com.qblog.service.CacheService;
import com.qblog.service.CategoryService;
import com.qblog.service.TagService;
import com.qblog.service.UserService;
import com.qblog.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final CacheService cacheService;
    private final ViewCountService viewCountService;

    // 缓存 Key 前缀
    private static final String CACHE_ARTICLE_DETAIL = "article:detail:";
    private static final String CACHE_ARTICLE_HOT = "article:hot";
    private static final String CACHE_ARTICLE_LATEST = "article:latest";
    private static final Duration TTL_DETAIL = Duration.ofMinutes(10);
    private static final Duration TTL_HOT = Duration.ofMinutes(30);
    private static final Duration TTL_LATEST = Duration.ofMinutes(5);

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
        if (tagId != null) {
            // 安全：先查询关联的文章ID，再使用 in 查询，避免 SQL 注入
            List<Long> articleIds = articleTagService.list(
                new LambdaQueryWrapper<ArticleTag>()
                    .select(ArticleTag::getArticleId)
                    .eq(ArticleTag::getTagId, tagId)
            ).stream()
             .map(ArticleTag::getArticleId)
             .collect(Collectors.toList());

            if (!articleIds.isEmpty()) {
                wrapper.in(Article::getId, articleIds);
            } else {
                // 没有匹配的文章，返回空结果
                wrapper.apply("1 = 0");
            }
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getSummary, keyword));
        }
        
        // 排序 - 使用安全的方式，避免 SQL 注入
        if (StrUtil.isNotBlank(sortBy)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortBy) {
                case "createTime" -> wrapper.orderBy(true, isAsc, Article::getCreateTime);
                case "viewCount" -> wrapper.orderBy(true, isAsc, Article::getViewCount);
                case "likeCount" -> wrapper.orderBy(true, isAsc, Article::getLikeCount);
                default -> wrapper.orderBy(true, isAsc, Article::getPublishTime);
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

        // 排序 - 使用安全的方式，避免 SQL 注入
        if (StrUtil.isNotBlank(sortBy)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortBy) {
                case "createTime" -> wrapper.orderBy(true, isAsc, Article::getCreateTime);
                case "viewCount" -> wrapper.orderBy(true, isAsc, Article::getViewCount);
                case "likeCount" -> wrapper.orderBy(true, isAsc, Article::getLikeCount);
                default -> wrapper.orderBy(true, isAsc, Article::getPublishTime);
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
        String cacheKey = CACHE_ARTICLE_DETAIL + id;

        // 使用缓存击穿保护的方法获取文章详情
        ArticleVO vo = cacheService.getOrLoad(cacheKey, ArticleVO.class, TTL_DETAIL, () -> {
            Article article = getById(id);
            if (article == null) {
                throw new ResourceNotFoundException("文章", id);
            }

            // 只检查已删除的文章（status=2），允许访问草稿（status=0）和已发布（status=1）
            if (article.getStatus() == 2) {
                throw new ResourceNotFoundException("文章", id);
            }

            ArticleVO result = BeanUtil.copyProperties(article, ArticleVO.class);

            // 填充作者信息
            if (article.getAuthorId() != null) {
                User author = userService.getById(article.getAuthorId());
                if (author != null) {
                    UserVO authorVO = new UserVO();
                    BeanUtil.copyProperties(author, authorVO);
                    result.setAuthor(authorVO);
                }
            }

            // 填充分类信息
            if (article.getCategoryId() != null) {
                Category category = categoryService.getById(article.getCategoryId());
                if (category != null) {
                    CategoryVO categoryVO = new CategoryVO();
                    BeanUtil.copyProperties(category, categoryVO);
                    result.setCategory(categoryVO);
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
                    result.setTags(tagVOs);
                }
            }

            return result;
        });

        // 使用 ViewCountService 增加浏览量（仅已发布文章）
        Article article = getById(id);
        if (article != null && article.getStatus() == 1) {
            viewCountService.incrementViewCount(id);
        }

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

        // 处理文章标签关联
        if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
            List<ArticleTag> articleTags = articleDTO.getTagIds().stream().map(tagId -> {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tagId);
                return articleTag;
            }).collect(Collectors.toList());
            articleTagService.saveBatch(articleTags);
        }

        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article updateArticle(Long id, ArticleDTO articleDTO) {
        Article article = getById(id);
        if (article == null) {
            throw new ResourceNotFoundException("文章", id);
        }

        BeanUtil.copyProperties(articleDTO, article);

        if (articleDTO.getStatus() == 1 && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }

        updateById(article);

        // 处理文章标签关联：先删除旧的，再添加新的
        articleTagService.remove(new LambdaQueryWrapper<ArticleTag>()
            .eq(ArticleTag::getArticleId, id));

        if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
            List<ArticleTag> articleTags = articleDTO.getTagIds().stream().map(tagId -> {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(id);
                articleTag.setTagId(tagId);
                return articleTag;
            }).collect(Collectors.toList());
            articleTagService.saveBatch(articleTags);
        }

        // 清除文章缓存
        clearArticleCache(id);

        return article;
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = getById(id);
        if (article != null) {
            article.setStatus(2); // 标记为已删除
            updateById(article);

            // 清除文章缓存
            clearArticleCache(id);
        }
    }

    @Override
    public List<ArticleListItemVO> getHotArticles(Integer limit) {
        String cacheKey = CACHE_ARTICLE_HOT + ":" + limit;

        // 使用缓存击穿保护的方法获取热门文章
        return cacheService.getOrLoadList(cacheKey, ArticleListItemVO.class, TTL_HOT, () -> {
            // 使用 Page 对象实现 LIMIT，避免 SQL 注入
            Page<Article> page = new Page<>(1, limit);
            List<Article> articles = page(page, new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .orderByDesc(Article::getViewCount))
                    .getRecords();
            return convertToListItemVO(articles);
        });
    }

    @Override
    public List<ArticleListItemVO> getLatestArticles(Integer limit) {
        String cacheKey = CACHE_ARTICLE_LATEST + ":" + limit;

        // 使用缓存击穿保护的方法获取最新文章
        return cacheService.getOrLoadList(cacheKey, ArticleListItemVO.class, TTL_LATEST, () -> {
            // 使用 Page 对象实现 LIMIT，避免 SQL 注入
            Page<Article> page = new Page<>(1, limit);
            List<Article> articles = page(page, new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .orderByDesc(Article::getPublishTime))
                    .getRecords();
            return convertToListItemVO(articles);
        });
    }

    @Override
    public List<ArticleListItemVO> getRelatedArticles(Long articleId, Integer limit) {
        // 获取当前文章的标签
        List<ArticleTag> currentArticleTags = articleTagService.list(
            new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId)
        );

        if (currentArticleTags.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = currentArticleTags.stream()
            .map(ArticleTag::getTagId)
            .collect(Collectors.toList());

        // 安全：使用参数化查询替代 inSql
        List<ArticleTag> relatedArticleTags = articleTagService.list(
            new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getTagId, tagIds)
                .ne(ArticleTag::getArticleId, articleId)
        );

        if (relatedArticleTags.isEmpty()) {
            return new ArrayList<>();
        }

        // 按文章ID分组，计算共同标签数
        Map<Long, Long> articleTagCount = relatedArticleTags.stream()
            .collect(Collectors.groupingBy(ArticleTag::getArticleId, Collectors.counting()));

        // 取 top N 文章ID
        List<Long> relatedArticleIds = articleTagCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (relatedArticleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询文章详情
        List<Article> relatedArticles = list(
            new LambdaQueryWrapper<Article>()
                .in(Article::getId, relatedArticleIds)
                .eq(Article::getStatus, 1)
        );

        // 按共同标签数排序返回
        Map<Long, Article> articleMap = relatedArticles.stream()
            .collect(Collectors.toMap(Article::getId, Function.identity()));

        List<Article> sortedArticles = relatedArticleIds.stream()
            .map(articleMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return convertToListItemVO(sortedArticles);
    }

    private List<ArticleListItemVO> convertToListItemVO(List<Article> articles) {
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<ArticleListItemVO> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 批量收集所有需要查询的 ID
        List<Long> authorIds = articles.stream()
            .map(Article::getAuthorId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        List<Long> categoryIds = articles.stream()
            .map(Article::getCategoryId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        List<Long> articleIds = articles.stream()
            .map(Article::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());

        // 批量查询作者、分类、标签
        Map<Long, User> authorMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            authorMap = userService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        }

        Map<Long, Category> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMap = categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        }

        // 批量查询文章标签关联和标签
        Map<Long, List<TagVO>> articleTagMap = new HashMap<>();
        if (!articleIds.isEmpty()) {
            // 查询所有文章的标签关联
            List<ArticleTag> allArticleTags = articleTagService.list(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds)
            );

            if (!allArticleTags.isEmpty()) {
                // 收集所有标签 ID
                List<Long> tagIds = allArticleTags.stream()
                    .map(ArticleTag::getTagId)
                    .distinct()
                    .collect(Collectors.toList());

                // 批量查询标签
                Map<Long, Tag> tagMap = tagService.listByIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, Function.identity()));

                // 按文章 ID 分组标签
                allArticleTags.stream()
                    .collect(Collectors.groupingBy(ArticleTag::getArticleId))
                    .forEach((articleId, articleTags) -> {
                        List<TagVO> tagVOs = articleTags.stream()
                            .map(at -> tagMap.get(at.getTagId()))
                            .filter(tag -> tag != null)
                            .map(tag -> {
                                TagVO tagVO = new TagVO();
                                BeanUtil.copyProperties(tag, tagVO);
                                return tagVO;
                            })
                            .collect(Collectors.toList());
                        articleTagMap.put(articleId, tagVOs);
                    });
            }
        }

        // 组装 VO
        for (Article article : articles) {
            ArticleListItemVO vo = BeanUtil.copyProperties(article, ArticleListItemVO.class);

            // 设置时间格式化
            if (article.getCreateTime() != null) {
                vo.setCreateTime(article.getCreateTime().format(formatter));
            }
            if (article.getPublishTime() != null) {
                vo.setPublishTime(article.getPublishTime().format(formatter));
            }

            // 填充作者信息
            if (article.getAuthorId() != null) {
                User author = authorMap.get(article.getAuthorId());
                if (author != null) {
                    UserVO authorVO = new UserVO();
                    BeanUtil.copyProperties(author, authorVO);
                    vo.setAuthor(authorVO);
                }
            }
            if (vo.getAuthor() == null) {
                vo.setAuthor(new UserVO());
            }

            // 填充分类信息
            if (article.getCategoryId() != null) {
                Category category = categoryMap.get(article.getCategoryId());
                if (category != null) {
                    CategoryVO categoryVO = new CategoryVO();
                    BeanUtil.copyProperties(category, categoryVO);
                    vo.setCategory(categoryVO);
                }
            }
            if (vo.getCategory() == null) {
                vo.setCategory(new CategoryVO());
            }

            // 填充标签信息
            vo.setTags(articleTagMap.getOrDefault(article.getId(), new ArrayList<>()));

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

    /**
     * 清除文章相关缓存
     */
    private void clearArticleCache(Long articleId) {
        // 清除文章详情缓存
        cacheService.delete(CACHE_ARTICLE_DETAIL + articleId);

        // 清除热门文章缓存（使用通配符删除所有热门文章缓存）
        cacheService.deleteByPattern(CACHE_ARTICLE_HOT + "*");

        // 清除最新文章缓存（使用通配符删除所有最新文章缓存）
        cacheService.deleteByPattern(CACHE_ARTICLE_LATEST + "*");
    }
}
