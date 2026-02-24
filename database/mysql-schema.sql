-- =====================================================
-- Q 博客数据库初始化脚本
-- 数据库：qblog
-- 字符集：utf8mb4 (兼容中文、emoji 等所有 Unicode 字符)
-- 排序规则：utf8mb4_general_ci (不区分大小写)
-- =====================================================

-- 如果数据库已存在则删除，避免字符集冲突
DROP DATABASE IF EXISTS qblog;

-- 创建数据库，指定字符集和排序规则
CREATE DATABASE qblog 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_general_ci;

USE qblog;

-- 设置客户端字符集（确保连接时使用正确的字符集）
SET NAMES utf8mb4;

-- =====================================================
-- 用户表
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `role` TINYINT DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- =====================================================
-- 分类表
-- =====================================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    `sort` INT DEFAULT 1 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类表';

-- =====================================================
-- 标签表
-- =====================================================
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标签表';

-- =====================================================
-- 文章表
-- =====================================================
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    `content` TEXT DEFAULT NULL COMMENT '文章内容',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图 URL',
    `author_id` BIGINT DEFAULT NULL COMMENT '作者 ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类 ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
    `top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿，1-已发布',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章表';

-- =====================================================
-- 文章标签关联表
-- =====================================================
DROP TABLE IF EXISTS `article_tag`;
CREATE TABLE `article_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章标签关联表';

-- =====================================================
-- 评论表
-- =====================================================
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论 ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评论表';

-- =====================================================
-- 收藏表
-- =====================================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
    KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收藏表';

-- =====================================================
-- 测试数据
-- =====================================================

-- 管理员账户（密码：admin123，BCrypt 加密）
-- 生成方式：new BCryptPasswordEncoder().encode("admin123")
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$n7y.FzdHQWM69UK4ohewZOAUZu4ZBidvtmrKzhohsOiKlE/DL1fwO', '管理员', 1, 1);

-- 测试分类
-- 注意：使用 CONVERT(... USING utf8mb4) 确保中文正确存储
INSERT INTO `category` (`name`, `description`, `sort`) VALUES
(CONVERT('技术' USING utf8mb4), CONVERT('技术相关文章' USING utf8mb4), 1),
(CONVERT('生活' USING utf8mb4), CONVERT('生活随笔' USING utf8mb4), 2),
(CONVERT('随笔' USING utf8mb4), CONVERT('日常随笔' USING utf8mb4), 3);

-- 测试标签
INSERT INTO `tag` (`name`) VALUES
('Java'),
('Vue'),
('Spring Boot'),
('前端'),
('后端'),
('MySQL');
