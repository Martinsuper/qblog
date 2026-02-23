package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章 Mapper 接口
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
