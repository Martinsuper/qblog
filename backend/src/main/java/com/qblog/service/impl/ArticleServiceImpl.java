package com.qblog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.common.SecurityUtils;
import com.qblog.common.Transactions;
import com.qblog.common.WebUtils;
import com.qblog.common.exception.ResourceNotFoundException;
import com.qblog.common.exception.UnauthorizedException;
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
import com.qblog.service.ContentCacheService;
import com.qblog.service.TagService;
import com.qblog.service.UserService;
import com.qblog.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ViewCountService viewCountService;
    private final ContentCacheService contentCacheService;

    // List queries must not load the potentially large Markdown body.
    private LambdaQueryWrapper<Article> listQuery() {
        return new LambdaQueryWrapper<Article>().select(Article::getId, Article::getTitle,
                Article::getSummary, Article::getCoverImage, Article::getAuthorId,
                Article::getCategoryId, Article::getViewCount, Article::getStatus,
                Article::getTop, Article::getCreateTime, Article::getUpdateTime, Article::getPublishTime);
    }

    @Override
    public Page<ArticleListItemVO> getArticleList(Integer page, Integer size, Long categoryId,
                                                   Long tagId, String keyword, String sortBy, String sortOrder) {
        Page<Article> articlePage = new Page<>(page, size);

        LambdaQueryWrapper<Article> wrapper = listQuery();
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
                default -> wrapper.orderBy(true, isAsc, Article::getPublishTime);
            }
        } else {
            wrapper.orderByDesc(true, Article::getTop);
            wrapper.orderByDesc(true, Article::getPublishTime);
        }

        Page<Article> resultPage = page(articlePage, wrapper);

        // 转换为 VO
        Page<ArticleListItemVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());
        voPage.setRecords(convertToListItemVO(resultPage.getRecords()));

        return voPage;
    }

    @Override
    public Page<ArticleListItemVO> getAdminArticleList(Integer page, Integer size, Long categoryId,
                                                        String keyword, Integer status, String sortBy, String sortOrder) {
        Page<Article> articlePage = new Page<>(page, size);

        LambdaQueryWrapper<Article> wrapper = listQuery();

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
                default -> wrapper.orderBy(true, isAsc, Article::getPublishTime);
            }
        } else {
            wrapper.orderByDesc(true, Article::getCreateTime);
        }

        Page<Article> resultPage = page(articlePage, wrapper);

        // 转换为 VO（分页插件已自动执行 count，直接使用 total）
        Page<ArticleListItemVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());
        voPage.setRecords(convertToListItemVO(resultPage.getRecords()));

        return voPage;
    }

    @Override
    public ArticleVO getArticleDetail(Long id) {
        Article article = getById(id);
        if (article == null || article.getStatus() == 2) {
            throw new ResourceNotFoundException("文章", id);
        }

        // 草稿仅管理员可见（也不进缓存，避免缓存把草稿泄漏给匿名访客）
        if (article.getStatus() == 0) {
            if (!SecurityUtils.isAdmin()) {
                throw new ResourceNotFoundException("文章", id);
            }
            return assembleArticleVO(article);
        }

        // 已发布文章走进程内短 TTL 缓存（陈旧窗口与设计权衡见 ContentCacheService 类注释）
        ArticleVO vo = contentCacheService.getArticleDetail(id, () -> assembleArticleVO(getById(id)));

        // 仅已发布文章计数浏览量（同一访客 24 小时内去重）
        if (vo != null && Integer.valueOf(1).equals(vo.getStatus())) {
            viewCountService.incrementViewCount(id, currentVisitorKey());
        }

        return vo;
    }

    /**
     * 组装文章详情 VO（填充作者/分类/标签）
     */
    @Override
    public ArticleVO getArticleForMcp(Long id) {
        Article article = getById(id);
        if (article == null || Integer.valueOf(2).equals(article.getStatus())) throw new ResourceNotFoundException("文章", id);
        return assembleArticleVO(article);
    }

    private ArticleVO assembleArticleVO(Article article) {
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO createArticle(ArticleDTO articleDTO) {
        Article article = BeanUtil.copyProperties(articleDTO, Article.class);
        // 创建时不带 status 默认存为草稿，而不是意外发布
        article.setStatus(articleDTO.getStatus() != null ? articleDTO.getStatus() : 0);
        article.setVersion(0L);

        // 作者为当前登录用户
        Long authorId = SecurityUtils.getCurrentUserId();
        if (authorId == null) {
            throw new UnauthorizedException("用户未登录");
        }
        article.setAuthorId(authorId);

        if (Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishTime(LocalDateTime.now());
        }

        save(article);
        saveArticleTags(article.getId(), articleDTO.getTagIds());

        // 新文章影响分类/标签的 article_count 统计；事务提交后再失效缓存（见 Transactions#afterCommit）
        Transactions.afterCommit(() -> {
            contentCacheService.evictCategories();
            contentCacheService.evictTags();
        });

        return assembleArticleVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO updateArticle(Long id, ArticleDTO articleDTO) {
        Article article = getById(id);
        if (article == null || article.getStatus() == 2) {
            throw new ResourceNotFoundException("文章", id);
        }

        // 忽略 null 字段：部分更新不得清空未传字段（摘要/封面/分类），status 为 null 表示保持原状态
        if (articleDTO.getVersion() == null || !articleDTO.getVersion().equals(article.getVersion())) {
            throw new com.qblog.common.exception.BusinessException(409, "文章已被修改，请重新载入后合并更改");
        }
        BeanUtil.copyProperties(articleDTO, article, CopyOptions.create().ignoreNullValue());
        article.setId(id);
        article.setVersion(articleDTO.getVersion() + 1);

        if (Integer.valueOf(1).equals(articleDTO.getStatus()) && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }

        boolean updated = update(article, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Article>()
            .eq(Article::getId, id).eq(Article::getVersion, articleDTO.getVersion()).ne(Article::getStatus, 2));
        if (!updated) throw new com.qblog.common.exception.BusinessException(409, "文章已被修改，请重新载入后合并更改");

        // 仅当请求显式携带 tagIds 时才替换标签关联，避免部分更新把标签清空
        if (articleDTO.getTagIds() != null) {
            articleTagService.remove(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, id));
            saveArticleTags(id, articleDTO.getTagIds());
        }

        // 内容/状态/分类/标签都可能变化；事务提交后再失效详情与统计缓存
        Transactions.afterCommit(() -> {
            contentCacheService.evictArticle(id);
            contentCacheService.evictCategories();
            contentCacheService.evictTags();
        });

        return assembleArticleVO(article);
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<ArticleTag> articleTags = tagIds.stream().map(tagId -> {
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(articleId);
            articleTag.setTagId(tagId);
            return articleTag;
        }).collect(Collectors.toList());
        articleTagService.saveBatch(articleTags);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        Article article = getById(id);
        if (article != null) {
            article.setStatus(2); // 标记为已删除
            updateById(article);
            Transactions.afterCommit(() -> {
                contentCacheService.evictArticle(id);
                contentCacheService.evictCategories();
                contentCacheService.evictTags();
            });
        }
    }

    @Override
    public List<ArticleListItemVO> getHotArticles(Integer limit) {
        // Prefer fresh publication state over distributed snapshot-cache races.
        // searchCount=false：top-N 不需要总数，省掉分页插件默认附加的 COUNT 查询
        return convertToListItemVO(page(new Page<Article>(1, limit, false), listQuery()
                .eq(Article::getStatus, 1).orderByDesc(Article::getViewCount)).getRecords());
    }

    @Override
    public List<ArticleListItemVO> getLatestArticles(Integer limit) {
        return convertToListItemVO(page(new Page<Article>(1, limit, false), listQuery()
                .eq(Article::getStatus, 1).orderByDesc(Article::getPublishTime)).getRecords());
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
            listQuery()
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

    /**
     * 获取当前访客标识（用于浏览量去重）
     */
    private String currentVisitorKey() {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes) {
            return WebUtils.getClientIp(servletAttributes.getRequest());
        }
        return "anonymous";
    }

}
