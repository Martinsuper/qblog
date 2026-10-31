package com.qblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.ai.AgentChatService;
import com.qblog.ai.AgentTools;
import com.qblog.ai.LlmConfigService;
import com.qblog.common.Result;
import com.qblog.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 站内智能体：配置管理（脱敏读写）+ SSE 流式对话。
 * 权限：SecurityConfig 的 anyRequest().hasRole("ADMIN") 已覆盖 /ai/**。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiAgentController {

    private final LlmConfigService configService;
    private final AgentChatService agent;
    private final ObjectMapper json;

    public AiAgentController(LlmConfigService configService, AgentChatService agent, ObjectMapper json) {
        this.configService = configService;
        this.agent = agent;
        this.json = json;
    }

    // ---------------------------------------------------------------- 配置

    @GetMapping("/config")
    public Result<LlmConfigService.LlmConfigView> getConfig() {
        return Result.success(configService.view());
    }

    @PutMapping("/config")
    public Result<LlmConfigService.LlmConfigView> updateConfig(@RequestBody LlmConfigService.LlmConfigUpsert req) {
        return Result.success(configService.update(req));
    }

    /** 保存配置后点一下：真实调用一次模型验证连通性 */
    @PostMapping("/config/test")
    public Result<String> testConnection() {
        try {
            return Result.success(agent.ping());
        } catch (Exception e) {
            log.warn("LLM 测试连接失败: {}", e.getMessage());
            return Result.error(502, e.getMessage() == null ? "连接失败" : e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 对话（SSE）

    /**
     * SSE 事件：
     *   token  {t: 增量文本}
     *   tool   {tool, summary, phase: begin|end, ok}
     *   error  {message}
     *   done   {}
     */
    @PostMapping(value = "/agent", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agent(@RequestBody AgentChatService.ChatIn body,
                            @org.springframework.web.bind.annotation.RequestParam(value = "q", required = false) String q) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时，由客户端主动断开
        String question = q == null || q.isBlank() ? "" : q;
        if (question.length() > 8000) throw new BusinessException(400, "消息过长");

        AgentTools.Listener listener = new AgentTools.Listener() {
            @Override public void onBegin(String tool, String summary) { emit(emitter, "tool", java.util.Map.of("tool", tool, "phase", "begin", "summary", summary, "ok", true)); }
            @Override public void onEnd(String tool, String summary, boolean ok) { emit(emitter, "tool", java.util.Map.of("tool", tool, "phase", "end", "summary", summary, "ok", ok)); }
        };

        try {
            reactor.core.Disposable[] ref = new reactor.core.Disposable[1];
            ref[0] = agent.chat(body, question, listener)
                    .subscribe(
                            response -> {
                                if (response == null || response.getResult() == null) return;
                                String content = response.getResult().getOutput() == null ? null
                                        : response.getResult().getOutput().getText();
                                if (content == null || content.isEmpty()) return;
                                emit(emitter, "token", java.util.Map.of("t", content));
                            },
                            error -> {
                                String msg = rootMessage(error);
                                log.warn("智能体会话异常: {}", msg);
                                emit(emitter, "error", java.util.Map.of("message", msg));
                                emitter.complete();
                            },
                            () -> emit(emitter, "done", java.util.Map.of()));
            // 客户端断开/超时 → 取消上游流，停止消耗模型 token
            emitter.onCompletion(() -> { if (ref[0] != null && !ref[0].isDisposed()) ref[0].dispose(); });
            emitter.onTimeout(() -> { if (ref[0] != null && !ref[0].isDisposed()) ref[0].dispose(); });
        } catch (Exception e) {
            emit(emitter, "error", java.util.Map.of("message", rootMessage(e)));
            emitter.complete();
        }
        return emitter;
    }

    private String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        String m = c.getMessage();
        if (m == null || m.isBlank()) m = "请求失败";
        return m.length() > 500 ? m.substring(0, 500) : m;
    }

    private void emit(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(json.writeValueAsString(data), MediaType.APPLICATION_JSON));
        } catch (IOException ignore) {
            // 客户端断开，交给 Flux cancel
        }
    }
}
