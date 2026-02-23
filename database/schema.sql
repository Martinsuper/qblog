-- 博客系统数据库设计
-- MySQL 8.0+

-- ==================== 用户相关 ====================

-- 用户表
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户 ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码 (加密)',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像 URL',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role` TINYINT DEFAULT 0 COMMENT '角色：0-普通用户 1-管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==================== 文章相关 ====================

-- 分类表
CREATE TABLE `category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(255) COMMENT '分类描述',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 标签表
CREATE TABLE `tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 文章表
CREATE TABLE `article` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文章 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `summary` VARCHAR(500) COMMENT '摘要',
    `content` LONGTEXT NOT NULL COMMENT '文章内容 (Markdown/HTML)',
    `cover_image` VARCHAR(255) COMMENT '封面图 URL',
    `author_id` BIGINT NOT NULL COMMENT '作者 ID',
    `category_id` BIGINT COMMENT '分类 ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿 1-已发布 2-已删除',
    `top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `publish_time` DATETIME COMMENT '发布时间',
    INDEX `idx_author_id` (`author_id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    FULLTEXT INDEX `ft_title_content` (`title`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 文章标签关联表
CREATE TABLE `article_tag` (
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`article_id`, `tag_id`),
    INDEX `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- ==================== 评论相关 ====================

-- 评论表
CREATE TABLE `comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论 ID(0 表示一级评论)',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-待审核 1-正常 2-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_article_id` (`article_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ==================== 互动相关 ====================

-- 点赞表
CREATE TABLE `like_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_article` (`user_id`, `article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

-- 收藏表
CREATE TABLE `favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ==================== 系统相关 ====================

-- 系统配置表
CREATE TABLE `system_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置 ID',
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `description` VARCHAR(255) COMMENT '配置描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE `operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志 ID',
    `user_id` BIGINT COMMENT '用户 ID',
    `operation` VARCHAR(100) COMMENT '操作类型',
    `module` VARCHAR(50) COMMENT '模块',
    `ip` VARCHAR(50) COMMENT 'IP 地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ==================== 初始数据 ====================

-- 插入默认管理员账户 (密码：admin123, BCrypt 加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDJ1fRjPZqYqN5zXqJvZ9vZJZqZq', '管理员', 1);

-- 插入默认分类
INSERT INTO `category` (`name`, `description`, `sort`) VALUES
('技术', '技术相关文章', 1),
('生活', '生活随笔', 2),
('随笔', '日常随笔', 3);

-- 插入默认标签
INSERT INTO `tag` (`name`) VALUES
('Java'), ('Spring Boot'), ('Vue.js'), ('前端'), ('后端'), ('数据库');

-- 插入系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site_name', 'Q 博客', '网站名称'),
('site_description', '一个基于 Spring Boot 和 Vue.js 的博客系统', '网站描述'),
('site_logo', '/logo.png', '网站 Logo'),
('article_page_size', '10', '文章分页大小');
