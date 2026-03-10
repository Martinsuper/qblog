-- =====================================================
-- V1: 添加索引和唯一约束
-- 执行前请备份数据库
-- 注意：如果有重复数据，需要先清理
-- =====================================================

USE qblog;

-- 1. 文章表复合索引（优化列表查询）
-- 覆盖场景: WHERE status=1 ORDER BY top DESC, publish_time DESC
-- 仅在索引不存在时创建
SET @exist_idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'article' AND index_name = 'idx_status_top_publish');
SET @sql := IF(@exist_idx = 0,
    'ALTER TABLE article ADD INDEX idx_status_top_publish (status, top, publish_time)',
    'SELECT ''Index idx_status_top_publish already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 文章标签关联表唯一约束（防止重复关联）
-- 注意：如果已存在重复数据，需要先清理
-- 清理重复数据的SQL（可选）：
-- DELETE t1 FROM article_tag t1 INNER JOIN article_tag t2 WHERE t1.id > t2.id AND t1.article_id = t2.article_id AND t1.tag_id = t2.tag_id;
SET @exist_uk := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'article_tag' AND index_name = 'uk_article_tag');
SET @sql := IF(@exist_uk = 0,
    'ALTER TABLE article_tag ADD UNIQUE INDEX uk_article_tag (article_id, tag_id)',
    'SELECT ''Index uk_article_tag already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 分类名称唯一约束
SET @exist_uk := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'category' AND index_name = 'uk_name');
SET @sql := IF(@exist_uk = 0,
    'ALTER TABLE category ADD UNIQUE INDEX uk_name (name)',
    'SELECT ''Index uk_name already exists on category''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 标签名称唯一约束
SET @exist_uk := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tag' AND index_name = 'uk_name');
SET @sql := IF(@exist_uk = 0,
    'ALTER TABLE tag ADD UNIQUE INDEX uk_name (name)',
    'SELECT ''Index uk_name already exists on tag''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;