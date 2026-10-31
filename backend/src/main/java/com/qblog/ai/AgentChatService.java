package com.qblog.ai;

import com.qblog.knowledge.KnowledgeFolderService;
import com.qblog.knowledge.KnowledgeService;
import com.qblog.knowledge.McpArticleTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 站内智能体会话：系统提示词 + 历史消息 + 工具执行，全部流式。
 * 工具由 Spring AI 内置执行（默认 ToolCallingManager），执行过程通过 AgentTools.Listener 回传。
 */
@Service
public class AgentChatService {

    private static final int MAX_HISTORY = 24;
    private static final int MAX_HISTORY_CHARS = 120_000;

    private final LlmModelHolder holder;
    private final KnowledgeService notes;
    private final KnowledgeFolderService folders;
    private final McpArticleTools articles;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public AgentChatService(LlmModelHolder holder, KnowledgeService notes, KnowledgeFolderService folders,
                            McpArticleTools articles, com.fasterxml.jackson.databind.ObjectMapper json) {
        this.holder = holder;
        this.notes = notes;
        this.folders = folders;
        this.articles = articles;
        this.json = json;
    }

    public record ChatIn(List<HistoryMessage> history) {}

    /** role 仅支持 user / assistant */
    public record HistoryMessage(String role, String content) {}

    public Flux<ChatResponse> chat(ChatIn in, String question, AgentTools.Listener listener) {
        org.springframework.ai.chat.model.ChatModel model = chatModel();
        List<Message> history = toMessages(in == null ? null : in.history());
        ChatClient client = ChatClient.builder(model).build();

        return client.prompt()
                .system(systemPrompt())
                .messages(history)
                .user(question)
                .tools(new AgentTools(notes, folders, articles, json, listener))
                .stream()
                .chatResponse();
    }

    /** 供"测试连接"使用的一次性轻量调用 */
    public String ping() {
        org.springframework.ai.chat.model.ChatModel m = chatModel();
        return ChatClient.builder(m).build().prompt()
                .user("回复两个字：正常")
                .call()
                .content();
    }

    private org.springframework.ai.chat.model.ChatModel chatModel() {
        // enabled 必须读最新配置（不能依赖缓存），避免关了还能用
        if (!holder.enabled()) throw new IllegalStateException("智能体未启用，请在「API 与 MCP → 智能体」中启用");
        return holder.chatModel();
    }

    private List<Message> toMessages(List<HistoryMessage> history) {
        List<Message> messages = new ArrayList<>();
        if (history == null) return messages;
        int chars = 0;
        for (HistoryMessage hm : history) {
            if (hm == null || hm.content() == null || hm.content().isBlank()) continue;
            boolean user = "user".equalsIgnoreCase(hm.role());
            Message m = user ? new UserMessage(hm.content()) : new AssistantMessage(hm.content());
            messages.add(m);
            chars += hm.content().length();
            if (chars > MAX_HISTORY_CHARS) break;
        }
        // 截断到最近 N 条，且必须以 user 结尾（带 user 消息的请求）
        while (messages.size() > MAX_HISTORY) messages.remove(0);
        while (!messages.isEmpty() && !(messages.get(messages.size() - 1) instanceof UserMessage)) messages.remove(messages.size() - 1);
        return messages;
    }

    private String systemPrompt() {
        return """
                你是「Q 博客」网站的站内智能体，以管理员身份工作。用户让你检索、整理、撰写站内文章与知识库笔记。

                能力与规矩：
                1. 回答关于站点内容的问题前，先用 articles_search / notes_search 检索，再引用真实数据，避免编造。
                2. 修改/创建文章时先 articles_get 读取最新内容和 version，再 articles_update；写完默认保持草稿，除非用户明确要求发布。
                3. 创建或更新知识库笔记用 notes_upsert：为每个主题生成稳定的 externalId（如 "topic-spring-basics"），同一主题重复写时传相同 externalId 实现更新；覆盖他人已有笔记时需先 notes_get 拿 version。
                4. 知识库笔记正文必须是编号 Markdown（## 一、 / ### 1.1），不要目录、不要一级标题。
                5. 用户的话题类请求（"把这个话题整理成笔记"）→ 先检索现有内容，没有则写一篇结构化笔记存入知识库。
                6. 一般操作回复保持简短；写内容时直接给出成稿，不要问过多问题。
                7. 你的操作在页面上对用户可见（工具调用提示），重要改动在回复末尾列出"做了什么"清单。
                8. 禁止重试循环：同一操作失败后先分析原因（版本号过期就 notes_get 取新 version；目录不存在就 folders_list 确认），同一操作最多重试 1 次；两次失败就换方法或直接向用户说明，不要再次调用。
                9. 单回合工具调用有硬性上限（超限会被强制终止），节约调用次数：能用一条搜索回答就别翻页穷举，批量读取前先确认必要性。
                """;
    }
}
