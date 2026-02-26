package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.Category;
import com.qblog.mapper.CategoryMapper;
import com.qblog.model.vo.CategoryVO;
import com.qblog.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<CategoryVO> listWithArticleCount() {
        return baseMapper.selectCategoryListWithArticleCount();
    }

    @Override
    @Transactional
    public void updateBatchSort(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 批量构建更新对象列表
        List<Category> categoriesToUpdate = IntStream.range(0, ids.size())
            .mapToObj(i -> {
                Category category = new Category();
                category.setId(ids.get(i));
                category.setSort(i + 1); // 从1开始排序
                return category;
            })
            .toList();

        // 批量更新所有排序，提高性能
        boolean result = updateBatchById(categoriesToUpdate);
        if (!result) {
            throw new RuntimeException("批量更新分类排序失败");
        }
    }
}
