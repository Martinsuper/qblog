package com.qblog.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qblog.common.exception.BusinessException;
import com.qblog.knowledge.KnowledgeModels;
import com.qblog.knowledge.KnowledgeScope;
import com.qblog.knowledge.KnowledgeService;
import com.qblog.knowledge.KnowledgeFolderService;
import com.qblog.knowledge.KnowledgeTokenService;
import com.qblog.knowledge.McpArticleTools;
import com.qblog.knowledge.NoteMarkdownPolicy;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 站内智能体的执行工具集（每次会话 new 一个实例，携带该次会话的事件监听器）。
 * 复用 MCP 工具层：文章走 McpArticleTools（含版本校验），笔记走 KnowledgeService，
 * 以管理员全量 scope 运行——见 SecurityConfig，站内智能体本身已是 ADMIN-only。
 */
public class AgentTools {

    /** 工具执行过程回调：begin / end，控制器负责转成 SSE 事件 */
    public interface Listener {
        void onBegin(String tool, String summary);

        void onEnd(String tool, String summary, boolean success);
    }

    private static final KnowledgeTokenService.Principal ADMIN =
            new KnowledgeTokenService.Principal(0L, 1L,
                    Set.of("READ", "WRITE", "ARTICLE_READ", "ARTICLE_WRITE", "ARTICLE_PUBLISH", "ARTICLE_DELETE"),
                    new KnowledgeTokenService.DirectoryScope(true, false, Set.of()));

    private final KnowledgeService notes;
    private final KnowledgeFolderService folders;
    private final McpArticleTools articles;
    private final ObjectMapper json;
    private final Listener listener;

    /** 单回合工具调用上限：防止模型忘我循环（真实事故：notes_upsert 反复重试上百次） */
    public static final int MAX_TOOL_CALLS = 24;
    private int calls;

    public AgentTools(KnowledgeService notes, KnowledgeFolderService folders, McpArticleTools articles,
                      ObjectMapper json, Listener listener) {
        this.notes = notes;
        this.folders = folders;
        this.articles = articles;
        this.json = json;
        this.listener = listener;
    }

    private static final String NOTE_FORMAT =
            "内容为编号 Markdown：顶级章节用「## 一、标题」，小节用「### 1.1 标题」。不要目录、不要一级标题。" + NoteMarkdownPolicy.INSTRUCTIONS;

    // ---------------------------------------------------------------- 文章

    @Tool(name = "articles_search", description = "按关键词搜索站点全部文章（含草稿）。返回标题、状态（0草稿/1已发布）、摘要")
    public Object articlesSearch(
            @ToolParam(description = "关键词，可为空表示全部", required = false) String keyword,
            @ToolParam(description = "页码，从1开始", required = false) Integer page,
            @ToolParam(description = "每页条数，默认20", required = false) Integer size) {
        ObjectNode args = json.createObjectNode();
        if (keyword != null && !keyword.isBlank()) args.put("keyword", keyword.trim());
        if (page != null) args.put("page", page);
        if (size != null) args.put("size", size);
        return run("articles_search", (keyword == null ? "" : "“" + keyword + "”"), () ->
                articles.call("articles_search", args, ADMIN));
    }

    @Tool(name = "articles_get", description = "按 ID 读取文章全文，返回内容与当前版本号 version")
    public Object articlesGet(@ToolParam(description = "文章 ID") long id) {
        ObjectNode args = json.createObjectNode().put("id", id);
        return run("articles_get", "文章 #" + id, () -> articles.call("articles_get", args, ADMIN));
    }

    @Tool(name = "articles_create", description = "创建文章草稿（不发布）。返回新文章 ID")
    public Object articlesCreate(
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "Markdown 全文") String content,
            @ToolParam(description = "摘要，可为空", required = false) String summary) {
        ObjectNode args = json.createObjectNode().put("title", title).put("content", content);
        if (summary != null && !summary.isBlank()) args.put("summary", summary);
        return run("articles_create", "新建「" + shortName(title) + "」", () -> articles.call("articles_create", args, ADMIN));
    }

    @Tool(name = "articles_update", description = "更新文章（整篇替换），必须传 articles_get 里拿到的当前 version")
    public Object articlesUpdate(
            @ToolParam(description = "文章 ID") long id,
            @ToolParam(description = "当前版本号") long version,
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "Markdown 全文") String content,
            @ToolParam(description = "摘要，可为空", required = false) String summary) {
        ObjectNode args = json.createObjectNode().put("id", id).put("version", version)
                .put("title", title).put("content", content);
        if (summary != null) args.put("summary", summary);
        return run("articles_update", "更新文章 #" + id, () -> articles.call("articles_update", args, ADMIN));
    }

    @Tool(name = "articles_publish", description = "使用当前版本发布文章")
    public Object articlesPublish(
            @ToolParam(description = "文章 ID") long id,
            @ToolParam(description = "当前版本号") long version) {
        ObjectNode args = json.createObjectNode().put("id", id).put("version", version);
        return run("articles_publish", "发布文章 #" + id, () -> articles.call("articles_publish", args, ADMIN));
    }

    @Tool(name = "articles_delete", description = "软删除文章（移入回收站）")
    public Object articlesDelete(
            @ToolParam(description = "文章 ID") long id,
            @ToolParam(description = "当前版本号") long version) {
        ObjectNode args = json.createObjectNode().put("id", id).put("version", version);
        return run("articles_delete", "删除文章 #" + id, () -> articles.call("articles_delete", args, ADMIN));
    }

    // ---------------------------------------------------------------- 知识库

    @Tool(name = "notes_search", description = "在知识库中做文本搜索，可过滤目录/标签/收藏")
    public Object notesSearch(
            @ToolParam(description = "搜索词，可为空表示全部", required = false) String search,
            @ToolParam(description = "目录 ID，可为空", required = false) Long folderId,
            @ToolParam(description = "标签过滤", required = false) String tag,
            @ToolParam(description = "页码", required = false) Integer current,
            @ToolParam(description = "每页条数", required = false) Integer size) {
        KnowledgeModels.NoteQuery q = new KnowledgeModels.NoteQuery();
        q.setSearch(blankToNull(search));
        q.setFolderId(folderId);
        q.setTag(blankToNull(tag));
        if (current != null && current > 0) q.setCurrent(current);
        if (size != null && size > 0 && size <= 100) q.setSize(size);
        return run("notes_search", searchSummary(q), () ->
                notes.list(q, KnowledgeScope.all()));
    }

    @Tool(name = "notes_get", description = "按 ID 读取知识库笔记全文（含当前版本号 version）")
    public Object notesGet(@ToolParam(description = "笔记 ID") long id) {
        return run("notes_get", "笔记 #" + id, () -> notes.get(id, KnowledgeScope.all()));
    }

    @Tool(name = "notes_upsert", description = "创建或更新知识库笔记：给 externalId 即按该键幂等落库；更新已有笔记需传其 version。标题+编号Markdown正文必填")
    public Object notesUpsert(
            @ToolParam(description = "标题（≤200字）") String title,
            @ToolParam(description = NOTE_FORMAT) String content,
            @ToolParam(description = "摘要，可为空", required = false) String summary,
            @ToolParam(description = "目录 ID，null 表示未归类", required = false) Long folderId,
            @ToolParam(description = "标签列表", required = false) List<String> tags,
            @ToolParam(description = "幂等键：由你如 'topic-<主题slug>' 生成的唯一外部 ID，可为空",
                    required = false) String externalId,
            @ToolParam(description = "覆盖已有笔记时传其当前版本号", required = false) Long version) {
        KnowledgeModels.NoteWrite w = new KnowledgeModels.NoteWrite();
        w.setTitle(title);
        w.setContent(content);
        w.setSummary(blankToNull(summary));
        w.setFolderId(folderId);
        if (tags != null && !tags.isEmpty()) w.setTags(tags);
        w.setSourceTool("site-agent");
        w.setSource("agent");
        w.setExternalId(blankToNull(externalId));
        if (version != null) w.setVersion(version);
        NoteMarkdownPolicy.validate(w.getContent());
        return run("notes_upsert", shortName(title), () -> notes.upsert(w, KnowledgeScope.all()));
    }

    @Tool(name = "folders_list", description = "列出知识库全部目录树及各目录笔记数")
    public List<KnowledgeModels.FolderView> foldersList() {
        if (++calls > MAX_TOOL_CALLS) throwToolLimit();
        listener.onBegin("folders_list", "目录树");
        listener.onEnd("folders_list", "目录树", true);
        return folders.list();
    }

    private void throwToolLimit() {
        throw new BusinessException(429, "本回合已执行 " + MAX_TOOL_CALLS + " 次工具调用，为避免失控已强制终止。" +
                "提示：多次失败的操作不要循环重试，请基于已有结果直接作答，或建议用户缩小问题范围。");
    }

    // ---------------------------------------------------------------- 公共

    private Object run(String tool, String summary, java.util.function.Supplier<Object> call) {
        if (++calls > MAX_TOOL_CALLS) throwToolLimit();
        listener.onBegin(tool, summary);
        try {
            Object result = call.get();
            listener.onEnd(tool, summary, true);
            return result;
        } catch (BusinessException | IllegalArgumentException e) {
            listener.onEnd(tool, summary, false);
            throw e instanceof BusinessException be ? be : new BusinessException(400, String.valueOf(e.getMessage()));
        } catch (RuntimeException e) {
            listener.onEnd(tool, summary, false);
            throw e;
        }
    }

    private String searchSummary(KnowledgeModels.NoteQuery q) {
        return q.getSearch() == null ? "全部笔记" : ("“" + q.getSearch() + "”");
    }

    private String shortName(String s) {
        return s == null ? "" : s.substring(0, Math.min(24, s.length()));
    }

    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
