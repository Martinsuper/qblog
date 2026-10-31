package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.common.Transactions;
import com.qblog.entity.Category;
import com.qblog.mapper.CategoryMapper;
import com.qblog.model.vo.CategoryVO;
import com.qblog.service.CategoryService;
import com.qblog.service.ContentCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final ContentCacheService contentCacheService;

    public CategoryServiceImpl(ContentCacheService contentCacheService) {
        this.contentCacheService = contentCacheService;
    }

    @Override
    public List<CategoryVO> listWithArticleCount() {
        return contentCacheService.getCategories(() -> baseMapper.selectCategoryListWithArticleCount());
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
        // 排序变化影响列表顺序；事务提交后再失效
        Transactions.afterCommit(contentCacheService::evictCategories);
    }

    // 以下写入口的缓存失效集中在 service 层（CategoryController 直接调用这些 IService 方法）

    @Override
    public boolean save(Category category) {
        boolean saved = super.save(category);
        if (saved) {
            Transactions.afterCommit(contentCacheService::evictCategories);
        }
        return saved;
    }

    @Override
    public boolean updateById(Category category) {
        boolean updated = super.updateById(category);
        if (updated) {
            // 改名会影响文章详情 VO 里内嵌的分类副本
            Transactions.afterCommit(() -> {
                contentCacheService.evictCategories();
                contentCacheService.evictAllArticles();
            });
        }
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        if (removed) {
            Transactions.afterCommit(() -> {
                contentCacheService.evictCategories();
                contentCacheService.evictAllArticles();
            });
        }
        return removed;
    }
}
