package com.qblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 站内智能体大模型 API 配置（单行表，恒为 id=1）
 */
@Data
@TableName("llm_config")
public class LlmConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 供应商名称（展示备注） */
    private String providerName;

    /** OpenAI 兼容 API 地址 */
    private String baseUrl;

    /** Chat Completions 路径 */
    private String chatCompletionsPath;

    /** API Key（读取时脱敏） */
    private String apiKey;

    /** 对话模型名称 */
    private String chatModel;

    private BigDecimal temperature;

    private Integer maxTokens;

    /** 是否启用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
