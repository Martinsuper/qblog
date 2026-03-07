package com.qblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.ArticleTag;
import com.qblog.entity.Tag;
import com.qblog.mapper.TagMapper;
import com.qblog.model.vo.TagVO;
import com.qblog.service.ArticleTagService;
import com.qblog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签服务实现
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagService articleTagService;

    @Override
    public List<TagVO> listWithArticleCount() {
        return tagMapper.selectTagListWithArticleCount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTagWithRelations(Long id) {
        // 先删除 article_tag 表中的关联记录
        articleTagService.remove(new LambdaQueryWrapper<ArticleTag>()
            .eq(ArticleTag::getTagId, id));
        // 再删除标签本身
        return removeById(id);
    }
}
