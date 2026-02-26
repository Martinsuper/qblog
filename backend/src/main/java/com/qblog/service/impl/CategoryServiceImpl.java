package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.Category;
import com.qblog.mapper.CategoryMapper;
import com.qblog.model.vo.CategoryVO;
import com.qblog.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<CategoryVO> listWithArticleCount() {
        return baseMapper.selectCategoryListWithArticleCount();
    }
}
