package com.qblog.ai;

import com.qblog.entity.LlmConfig;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 根据数据库里的 llm_config 构建/缓存 OpenAI 兼容 ChatModel。
 * 配置指纹（地址+Key+模型+参数）变化时重建，避免每次请求新建连接池。
 */
@Component
public class LlmModelHolder {

    private static final long CONNECT_TIMEOUT_MS = 10_000;

    private final LlmConfigService configService;

    private volatile String fingerprint = "";
    private volatile LlmConfig cachedConfig;
    private volatile ChatModel model;

    public LlmModelHolder(LlmConfigService configService) {
        this.configService = configService;
    }

    /** @return 当前生效配置 */
    public LlmConfig config() {
        return cachedConfig != null ? cachedConfig : configService.load();
    }

    public boolean enabled() {
        return configService.load().getEnabled() != null && configService.load().getEnabled() == 1;
    }

    /** 每次会话前调用，必要时按最新配置重建模型 */
    public ChatModel chatModel() {
        LlmConfig config = configService.load();
        String fp = fingerprint(config);
        if (model == null || !fp.equals(fingerprint)) {
            synchronized (this) {
                if (model == null || !fp.equals(fingerprint)) {
                    this.cachedConfig = config;
                    this.model = build(config);
                    this.fingerprint = fp;
                }
            }
        }
        return model;
    }

    private String fingerprint(LlmConfig c) {
        return String.join("|", nz(c.getBaseUrl()), nz(c.getChatCompletionsPath()), nz(c.getApiKey()),
                nz(c.getChatModel()), String.valueOf(c.getTemperature()), String.valueOf(c.getMaxTokens()));
    }

    private String nz(String s) { return s == null ? "" : s; }

    private ChatModel build(LlmConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank())
            throw new IllegalStateException("请在「API 与 MCP → 智能体」中配置大模型 API Key");
        if (config.getChatModel() == null || config.getChatModel().isBlank())
            throw new IllegalStateException("请在「API 与 MCP → 智能体」中配置对话模型");

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .completionsPath(config.getChatCompletionsPath() == null ? "/v1/chat/completions"
                        : config.getChatCompletionsPath())
                // Netty 自带的独立 DNS 解析器在 fake-IP / 局域网环境（如本机代理接管系统 DNS）下
                // 会解析超时且连接不干净关闭，导致 SSE 挂死；改走 JDK 系统解析器（与 curl/浏览器一致）。
                .webClientBuilder(WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                .resolver(DefaultAddressResolverGroup.INSTANCE)
                                .responseTimeout(Duration.ofSeconds(300)))))
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(nz(config.getChatModel()))
                .temperature(config.getTemperature() == null ? 0.7 : config.getTemperature().doubleValue())
                .maxTokens(config.getMaxTokens() == null ? 4096 : config.getMaxTokens())
                .build();

        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .build();
    }
}
