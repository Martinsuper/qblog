package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.ArticleTag;
import com.qblog.mapper.ArticleTagMapper;
import com.qblog.service.ArticleTagService;
import org.springframework.stereotype.Service;

/**
 * 文章标签关联服务实现
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements ArticleTagService {
}
