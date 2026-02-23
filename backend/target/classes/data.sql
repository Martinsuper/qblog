-- H2 数据库初始化脚本

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户 ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码 (加密)',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像 URL',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role` TINYINT DEFAULT 0 COMMENT '角色：0-普通用户 1-管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(255) COMMENT '分类描述',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文章 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `summary` VARCHAR(500) COMMENT '摘要',
    `content` LONGTEXT NOT NULL COMMENT '文章内容',
    `cover_image` VARCHAR(255) COMMENT '封面图 URL',
    `author_id` BIGINT NOT NULL COMMENT '作者 ID',
    `category_id` BIGINT COMMENT '分类 ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿 1-已发布 2-已删除',
    `top` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `publish_time` DATETIME COMMENT '发布时间'
);

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS `article_tag` (
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`article_id`, `tag_id`)
);

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论 ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 点赞表
CREATE TABLE IF NOT EXISTS `like_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置 ID',
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `description` VARCHAR(255) COMMENT '配置描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志 ID',
    `user_id` BIGINT COMMENT '用户 ID',
    `operation` VARCHAR(100) COMMENT '操作类型',
    `module` VARCHAR(50) COMMENT '模块',
    `ip` VARCHAR(50) COMMENT 'IP 地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 初始数据
-- 管理员账号：admin / admin123
-- 测试用户：test / test123
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$tSXwHO/I.ogRRCN08.mX8.6Snr9xf3YsagmEvGalzNsp9rPQ0SmYe', '管理员', 1),
('test', '$2a$10$4N21kV14hN1X4N21kV14hO.XqJvZ9vZJZqZqN5zXqJvZ9vZJZqZq', '测试用户', 0);

-- 分类
INSERT INTO `category` (`name`, `description`, `sort`) VALUES
('技术', '技术相关文章', 1),
('生活', '生活随笔', 2),
('随笔', '日常随笔', 3);

-- 标签
INSERT INTO `tag` (`name`) VALUES
('Java'), ('Spring Boot'), ('Vue.js'), ('前端'), ('后端'), ('数据库');

-- 系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site_name', 'Q 博客', '网站名称'),
('site_description', '一个基于 Spring Boot 和 Vue.js 的博客系统', '网站描述'),
('article_page_size', '10', '文章分页大小');

-- 示例文章
INSERT INTO `article` (`title`, `summary`, `content`, `author_id`, `category_id`, `view_count`, `like_count`, `status`, `publish_time`) VALUES
('Spring Boot 入门教程', '本文介绍 Spring Boot 的基础用法，包括项目搭建、配置、常用注解等', '# Spring Boot 入门教程\n\n## 简介\n\nSpring Boot 是一个用于创建 Spring 应用的开源框架，它简化了 Spring 应用的搭建和开发过程。\n\n## 主要特性\n\n- 创建独立的 Spring 应用程序\n- 嵌入的 Tomcat，无需部署 WAR 文件\n- 简化 Maven 配置\n- 自动配置 Spring\n\n## 快速开始\n\n```java\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n```\n\n更多内容请关注后续文章...', 1, 1, 100, 10, 1, CURRENT_TIMESTAMP),
('Vue.js 3.0 新特性', 'Vue.js 3.0 带来了 Composition API、性能提升等众多新特性', '# Vue.js 3.0 新特性\n\n## Composition API\n\nComposition API 是 Vue.js 3.0 最重要的特性，它提供了一种新的代码组织方式。\n\n```javascript\nimport { ref, reactive, computed } from ''vue''\n\nexport default {\n  setup() {\n    const count = ref(0)\n    const state = reactive({ name: ''Vue'' })\n    \n    const double = computed(() => count.value * 2)\n    \n    return { count, state, double }\n  }\n}\n```\n\n## 性能提升\n\n- 虚拟 DOM 重写\n- 更小的包体积\n- 更快的渲染速度', 1, 1, 80, 8, 1, CURRENT_TIMESTAMP),
('MySQL 性能优化实战', '分享 MySQL 数据库性能优化的实战经验', '# MySQL 性能优化实战\n\n## 索引优化\n\n索引是数据库性能优化的重要手段。\n\n### 索引类型\n\n- B-Tree 索引\n- Hash 索引\n- 全文索引\n\n### 创建索引的原则\n\n1. 频繁查询的字段\n2. WHERE 子句中的字段\n3. JOIN 连接字段\n\n## SQL 优化\n\n```sql\n-- 避免 SELECT *\nSELECT id, name FROM user WHERE status = 1;\n\n-- 使用 EXPLAIN 分析\nEXPLAIN SELECT * FROM article WHERE author_id = 1;\n```\n\n## 配置优化\n\n调整 innodb_buffer_pool_size 等参数...', 1, 1, 50, 5, 1, CURRENT_TIMESTAMP);
