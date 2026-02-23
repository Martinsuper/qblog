package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.ArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章标签关联 Mapper 接口
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
}
