package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.Category;
import com.qblog.mapper.CategoryMapper;
import com.qblog.service.CategoryService;
import org.springframework.stereotype.Service;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
}
