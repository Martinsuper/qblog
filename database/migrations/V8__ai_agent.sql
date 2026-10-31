-- 站内智能体：大模型 API 配置（单行表）
CREATE TABLE `llm_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键（单行，恒为 1）',
    `provider_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '供应商名称（仅作展示备注）',
    `base_url` VARCHAR(255) NOT NULL DEFAULT 'https://api.openai.com' COMMENT 'OpenAI 兼容 API 地址（不带 /v1/chat/completions 路径）',
    `chat_completions_path` VARCHAR(128) NOT NULL DEFAULT '/v1/chat/completions' COMMENT 'Chat Completions 路径',
    `api_key` TEXT NOT NULL COMMENT 'API Key，接口读取时脱敏',
    `chat_model` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '对话模型名称',
    `temperature` DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '生成温度',
    `max_tokens` INT NOT NULL DEFAULT 4096 COMMENT '单轮回复最大 token',
    `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用智能体',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内智能体大模型配置';

INSERT INTO `llm_config` (`id`, `api_key`) VALUES (1, '') ON DUPLICATE KEY UPDATE `id` = `id`;
