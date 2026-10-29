package com.qblog.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.entity.Article;
import com.qblog.model.dto.ArticleDTO;
import com.qblog.service.ArticleService;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class McpArticleToolsTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ArticleService service = mock(ArticleService.class);
    private final McpArticleTools tools = new McpArticleTools(service, json);
    private KnowledgeTokenService.Principal token(String... scopes) {
        return new KnowledgeTokenService.Principal(1L, 2L, Set.of(scopes), new KnowledgeTokenService.DirectoryScope(false, false, Set.of()));
    }
    @Test void oldKnowledgeTokenCannotCreateArticle() throws Exception {
        var args = json.readTree("{\"title\":\"a\",\"content\":\"b\"}");
        assertThrows(RuntimeException.class, () -> tools.call("articles_create", args, token("WRITE")));
        verifyNoInteractions(service);
    }
    @Test void createAlwaysProducesDraft() throws Exception {
        var args = json.readTree("{\"title\":\"a\",\"content\":\"b\"}");
        tools.call("articles_create", args, token("ARTICLE_WRITE"));
        verify(service).createArticle(argThat(dto -> dto.getStatus() == 0));
    }
    @Test void publishedArticleRequiresPublishPermission() throws Exception {
        Article current = new Article(); current.setStatus(1); when(service.getById(3L)).thenReturn(current);
        var args = json.readTree("{\"id\":3,\"version\":0,\"title\":\"a\",\"content\":\"b\"}");
        assertThrows(RuntimeException.class, () -> tools.call("articles_update", args, token("ARTICLE_WRITE")));
        verify(service, never()).updateArticle(anyLong(), any());
    }
    @Test void updateAndPublishRequireVersion() throws Exception {
        Article current = new Article(); current.setStatus(0); when(service.getById(3L)).thenReturn(current);
        assertThrows(IllegalArgumentException.class, () -> tools.call("articles_update", json.readTree("{\"id\":3,\"title\":\"a\",\"content\":\"b\"}"), token("ARTICLE_WRITE")));
        assertThrows(IllegalArgumentException.class, () -> tools.call("articles_publish", json.readTree("{\"id\":3}"), token("ARTICLE_PUBLISH")));
        verify(service, never()).updateArticle(anyLong(), any(ArticleDTO.class));
    }
}
