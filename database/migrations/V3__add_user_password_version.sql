-- V3: 用户表新增密码版本字段
-- 用途：修改密码时版本 +1，使所有已签发的旧 JWT 立即失效（Token 吊销机制）
ALTER TABLE `user`
    ADD COLUMN `password_version` INT NOT NULL DEFAULT 0 COMMENT '密码版本：改密时 +1，用于失效旧 Token' AFTER `status`;
