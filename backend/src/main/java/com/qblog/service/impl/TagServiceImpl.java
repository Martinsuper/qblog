package com.qblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.common.Transactions;
import com.qblog.entity.ArticleTag;
import com.qblog.entity.Tag;
import com.qblog.mapper.TagMapper;
import com.qblog.model.vo.TagVO;
import com.qblog.service.ArticleTagService;
import com.qblog.service.ContentCacheService;
import com.qblog.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * 标签服务实现
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagService articleTagService;
    private final ContentCacheService contentCacheService;

    public TagServiceImpl(TagMapper tagMapper, ArticleTagService articleTagService,
                          ContentCacheService contentCacheService) {
        this.tagMapper = tagMapper;
        this.articleTagService = articleTagService;
        this.contentCacheService = contentCacheService;
    }

    @Override
    public List<TagVO> listWithArticleCount() {
        return contentCacheService.getTags(() -> tagMapper.selectTagListWithArticleCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTagWithRelations(Long id) {
        // 先删除 article_tag 表中的关联记录
        articleTagService.remove(new LambdaQueryWrapper<ArticleTag>()
            .eq(ArticleTag::getTagId, id));
        // 再删除标签本身
        boolean removed = removeById(id);
        if (removed) {
            // 文章详情 VO 内嵌标签副本，关联删除也会改变统计；事务提交后再失效
            Transactions.afterCommit(() -> {
                contentCacheService.evictTags();
                contentCacheService.evictAllArticles();
            });
        }
        return removed;
    }

    // 以下写入口的缓存失效集中在 service 层（TagController 直接调用这些 IService 方法）

    @Override
    public boolean save(Tag tag) {
        boolean saved = super.save(tag);
        if (saved) {
            Transactions.afterCommit(contentCacheService::evictTags);
        }
        return saved;
    }

    @Override
    public boolean updateById(Tag tag) {
        boolean updated = super.updateById(tag);
        if (updated) {
            // 改名会影响文章详情 VO 里内嵌的标签副本
            Transactions.afterCommit(() -> {
                contentCacheService.evictTags();
                contentCacheService.evictAllArticles();
            });
        }
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        if (removed) {
            Transactions.afterCommit(() -> {
                contentCacheService.evictTags();
                contentCacheService.evictAllArticles();
            });
        }
        return removed;
    }
}
