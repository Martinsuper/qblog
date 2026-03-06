package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.Tag;
import com.qblog.model.vo.TagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签 Mapper 接口
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 获取标签列表及其文章数量（仅统计已发布文章）
     */
    @Select("SELECT t.id, t.name, t.create_time as createTime, COUNT(at.article_id) as articleCount " +
            "FROM tag t LEFT JOIN article_tag at ON t.id = at.tag_id " +
            "LEFT JOIN article a ON at.article_id = a.id AND a.status = 1 " +
            "GROUP BY t.id, t.name, t.create_time ORDER BY t.create_time DESC")
    List<TagVO> selectTagListWithArticleCount();
}
