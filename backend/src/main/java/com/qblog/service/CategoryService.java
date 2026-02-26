package com.qblog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qblog.entity.Category;
import com.qblog.model.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取分类列表及其文章数量
     */
    List<CategoryVO> listWithArticleCount();
}
