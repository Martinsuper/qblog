package com.qblog.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.entity.Article;
import com.qblog.model.dto.ArticleDTO;
import com.qblog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class McpArticleTools {
    private final ArticleService articles;
    private final ObjectMapper json;

    @Transactional(rollbackFor = Exception.class)
    public Object call(String name, JsonNode args, KnowledgeTokenService.Principal token) {
        if (!args.isObject()) throw new IllegalArgumentException();
        switch (name) {
            case "articles_search": {
                token.require("ARTICLE_READ"); fields(args, Set.of("keyword", "page", "size"));
                String keyword = args.has("keyword") ? text(args, "keyword", 200) : null;
                int page = integer(args, "page", 1, 1000000), size = integer(args, "size", 20, 100);
                return articles.getAdminArticleList(page, size, null, keyword, null, "createTime", "desc");
            }
            case "articles_get": {
                token.require("ARTICLE_READ"); fields(args, Set.of("id"));
                return articles.getArticleForMcp(id(args));
            }
            case "articles_create": {
                token.require("ARTICLE_WRITE"); fields(args, Set.of("title", "content", "summary"));
                ArticleDTO dto = write(args); dto.setStatus(0);
                return articles.createArticle(dto);
            }
            case "articles_update": {
                token.require("ARTICLE_WRITE"); fields(args, Set.of("id", "version", "title", "content", "summary"));
                long id = id(args); Article current = articles.getById(id);
                if (current == null || current.getStatus() == 2) throw new IllegalArgumentException();
                if (current.getStatus() == 1) token.require("ARTICLE_PUBLISH");
                ArticleDTO dto = write(args); dto.setVersion(version(args));
                return articles.updateArticle(id, dto);
            }
            case "articles_publish": {
                token.require("ARTICLE_PUBLISH"); fields(args, Set.of("id", "version"));
                ArticleDTO dto = new ArticleDTO(); dto.setVersion(version(args)); dto.setStatus(1);
                return articles.updateArticle(id(args), dto);
            }
            case "articles_delete": {
                token.require("ARTICLE_DELETE"); fields(args, Set.of("id", "version"));
                ArticleDTO dto = new ArticleDTO(); dto.setVersion(version(args)); dto.setStatus(2);
                return articles.updateArticle(id(args), dto);
            }
            default: throw new IllegalArgumentException();
        }
    }
    private ArticleDTO write(JsonNode args) {
        ArticleDTO dto = new ArticleDTO(); dto.setTitle(text(args, "title", 200)); dto.setContent(text(args, "content", 60000));
        NoteMarkdownPolicy.validateLinks(dto.getContent());
        if (args.has("summary")) dto.setSummary(text(args, "summary", 500));
        return dto;
    }
    private String text(JsonNode n, String key, int max) {
        JsonNode v = n.get(key); if (v == null || !v.isTextual() || v.asText().isBlank() || v.asText().length() > max) throw new IllegalArgumentException(); return v.asText();
    }
    private int integer(JsonNode n, String key, int fallback, int max) {
        if (!n.has(key)) return fallback; JsonNode v=n.get(key); if (!v.isIntegralNumber() || !v.canConvertToInt() || v.asInt()<1 || v.asInt()>max) throw new IllegalArgumentException(); return v.asInt();
    }
    private long id(JsonNode n) { JsonNode v=n.get("id"); if (v==null || !v.isIntegralNumber() || !v.canConvertToLong() || v.asLong()<1) throw new IllegalArgumentException(); return v.asLong(); }
    private long version(JsonNode n) { JsonNode v=n.get("version"); if (v==null || !v.isIntegralNumber() || !v.canConvertToLong() || v.asLong()<0 || v.asLong()==Long.MAX_VALUE) throw new IllegalArgumentException(); return v.asLong(); }
    private void fields(JsonNode n, Set<String> allowed) { n.fieldNames().forEachRemaining(k -> { if (!allowed.contains(k)) throw new IllegalArgumentException(); }); }
}
