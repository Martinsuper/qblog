package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章 Mapper 接口
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 增量更新浏览量
     */
    @Update("UPDATE article SET view_count = view_count + #{increment} WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id, @Param("increment") Long increment);
}
