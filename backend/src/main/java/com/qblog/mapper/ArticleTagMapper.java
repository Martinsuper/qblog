package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.ArticleTag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章标签关联 Mapper 接口
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    /**
     * 按显式主键插入（备份恢复专用），保持与备份中 article_id/tag_id 的对应关系
     */
    @Insert("INSERT INTO article_tag (id, article_id, tag_id, create_time) VALUES " +
            "(#{id}, #{articleId}, #{tagId}, #{createTime})")
    int insertWithId(ArticleTag articleTag);
}
