package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.Category;
import com.qblog.model.vo.CategoryVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分类 Mapper 接口
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 获取分类列表及其文章数量（仅统计已发布文章）
     */
    @Select("SELECT c.id, c.name, c.description, c.sort, c.create_time as createTime, COUNT(a.id) as articleCount " +
            "FROM category c LEFT JOIN article a ON c.id = a.category_id AND a.status = 1 " +
            "GROUP BY c.id, c.name, c.description, c.sort, c.create_time ORDER BY c.sort ASC")
    List<CategoryVO> selectCategoryListWithArticleCount();

    /**
     * 按显式主键插入（备份恢复专用），保留原 id 供 category_id 引用
     */
    @Insert("INSERT INTO category (id, name, description, sort, create_time, update_time) VALUES " +
            "(#{id}, #{name}, #{description}, COALESCE(#{sort}, 1), #{createTime}, #{updateTime})")
    int insertWithId(Category category);
}
