package com.qblog.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qblog.common.exception.BusinessException;
import com.qblog.entity.LlmConfig;
import com.qblog.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 大模型 API 配置读写（llm_config 单行表）。
 * API Key 只在更新时可见，查询一律脱敏返回。
 */
@Service
@RequiredArgsConstructor
public class LlmConfigService {

    private static final Pattern MASKABLE = Pattern.compile("^(.{6}).*");

    private final LlmConfigMapper mapper;

    /** 给前端的安全视图，api_key 只显示末四位 */
    public record LlmConfigView(String providerName, String baseUrl, String chatCompletionsPath,
                                String apiKeyMasked, String chatModel, double temperature,
                                int maxTokens, boolean enabled) {
        public static LlmConfigView of(LlmConfig c) {
            String key = c.getApiKey() == null ? "" : c.getApiKey();
            String masked = key.isBlank() ? "" : ("******" + key.substring(Math.max(0, key.length() - 4)));
            return new LlmConfigView(c.getProviderName(), c.getBaseUrl(), c.getChatCompletionsPath(),
                    masked, c.getChatModel(),
                    c.getTemperature() == null ? 0.7 : c.getTemperature().doubleValue(),
                    c.getMaxTokens() == null ? 4096 : c.getMaxTokens(),
                    c.getEnabled() != null && c.getEnabled() == 1);
        }
    }

    /** 更新请求；apiKey 为空/null 表示保持原值 */
    public record LlmConfigUpsert(String providerName, String baseUrl, String chatCompletionsPath,
                                  String apiKey, String chatModel, Double temperature,
                                  Integer maxTokens, Boolean enabled) {}

    public LlmConfig load() {
        LlmConfig config = mapper.selectOne(new QueryWrapper<LlmConfig>().eq("id", 1L));
        if (config == null) {
            // 兜底：迁移未执行时自动补一行，保证页面可用
            config = new LlmConfig();
            config.setId(1L);
            config.setBaseUrl("https://api.openai.com");
            config.setChatCompletionsPath("/v1/chat/completions");
            config.setApiKey("");
            config.setEnabled(0);
            config.setTemperature(new java.math.BigDecimal("0.70"));
            config.setMaxTokens(4096);
            mapper.insert(config);
        }
        return config;
    }

    public LlmConfigView view() {
        return LlmConfigView.of(load());
    }

    @Transactional(rollbackFor = Exception.class)
    public LlmConfigView update(LlmConfigUpsert req) {
        LlmConfig config = load();
        if (req.providerName() != null) config.setProviderName(t(req.providerName(), 64));
        if (req.baseUrl() != null) config.setBaseUrl(t(req.baseUrl(), 255));
        if (req.chatCompletionsPath() != null) config.setChatCompletionsPath(t(req.chatCompletionsPath(), 128));
        if (req.apiKey() != null && !req.apiKey().isBlank()) config.setApiKey(req.apiKey());
        if (req.chatModel() != null) config.setChatModel(t(req.chatModel(), 128));
        if (req.temperature() != null) {
            if (req.temperature() < 0 || req.temperature() > 2) throw new BusinessException(400, "temperature 取值范围 0-2");
            config.setTemperature(java.math.BigDecimal.valueOf(req.temperature()));
        }
        if (req.maxTokens() != null) {
            if (req.maxTokens() < 64 || req.maxTokens() > 200000) throw new BusinessException(400, "maxTokens 取值范围 64-200000");
            config.setMaxTokens(req.maxTokens());
        }
        if (req.enabled() != null) config.setEnabled(req.enabled() ? 1 : 0);
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) throw new BusinessException(400, "API 地址不能为空");
        config.setBaseUrl(config.getBaseUrl().replaceAll("/+$", ""));
        if (config.getChatCompletionsPath() == null || config.getChatCompletionsPath().isBlank())
            config.setChatCompletionsPath("/v1/chat/completions");
        if (!config.getChatCompletionsPath().startsWith("/")) throw new BusinessException(400, "路径必须以 / 开头");
        // updateTime 由数据库 ON UPDATE 维护
        mapper.updateById(config);
        return view();
    }

    private String t(String v, int max) {
        String s = v == null ? "" : v.trim();
        if (s.length() > max) throw new BusinessException(400, "长度超出限制");
        return s;
    }
}
