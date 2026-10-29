package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.Article;
import org.apache.ibatis.annotations.Insert;
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

    /**
     * 按显式主键插入（备份恢复专用）：实体 IdType.AUTO 的 insert() 不会写入预设 id，
     * 恢复后 article_tag 等对旧 id 的引用会全部错位
     */
    @Insert("INSERT INTO article (id, title, summary, content, cover_image, author_id, category_id, " +
            "view_count, version, top, status, publish_time, create_time, update_time) VALUES " +
            "(#{id}, #{title}, #{summary}, #{content}, #{coverImage}, #{authorId}, #{categoryId}, " +
            "COALESCE(#{viewCount}, 0), COALESCE(#{version}, 0), COALESCE(#{top}, 0), COALESCE(#{status}, 0), " +
            "#{publishTime}, #{createTime}, #{updateTime})")
    int insertWithId(Article article);
}
