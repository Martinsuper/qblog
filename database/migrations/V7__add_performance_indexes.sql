-- =====================================================
-- V7: 性能优化补充索引
-- 覆盖场景见每条索引上方注释；仅在索引不存在时创建
-- =====================================================

USE qblog;

-- 1. 热门文章排序: WHERE status=1 ORDER BY view_count DESC
SET @exist_idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'article' AND index_name = 'idx_status_view_count');
SET @sql := IF(@exist_idx = 0,
    'ALTER TABLE article ADD INDEX idx_status_view_count (status, view_count)',
    'SELECT ''Index idx_status_view_count already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 管理列表默认排序: WHERE status<>2 ORDER BY create_time DESC（也覆盖公开列表按创建时间排序）
SET @exist_idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'article' AND index_name = 'idx_article_create_time');
SET @sql := IF(@exist_idx = 0,
    'ALTER TABLE article ADD INDEX idx_article_create_time (create_time)',
    'SELECT ''Index idx_article_create_time already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
