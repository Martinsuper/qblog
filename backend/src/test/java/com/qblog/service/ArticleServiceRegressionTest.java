package com.qblog.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qblog.entity.Article;
import com.qblog.mapper.TagMapper;
import com.qblog.model.dto.ArticleDTO;
import com.qblog.service.impl.ArticleServiceImpl;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleServiceRegressionTest {
    ArticleTagService associations = mock(ArticleTagService.class);
    ArticleServiceImpl service = spy(new ArticleServiceImpl(mock(UserService.class),
            mock(CategoryService.class), mock(TagService.class), associations,
            mock(ViewCountService.class)));

    @BeforeEach void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Article.class);
    }
    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test void staleVersionIsRejectedBeforeWrite() {
        Article article = new Article(); article.setId(1L); article.setStatus(0); article.setVersion(2L);
        doReturn(article).when(service).getById(1L);
        ArticleDTO dto = new ArticleDTO(); dto.setVersion(1L);
        var error = assertThrows(com.qblog.common.exception.BusinessException.class, () -> service.updateArticle(1L, dto));
        assertEquals(409, error.getCode());
        verify(service, never()).update(any(Article.class), any());
    }

    @Test void concurrentUpdateFailureIsRejected() {
        Article article = new Article(); article.setId(1L); article.setStatus(0); article.setVersion(2L);
        doReturn(article).when(service).getById(1L);
        doReturn(false).when(service).update(any(Article.class), any());
        ArticleDTO dto = new ArticleDTO(); dto.setVersion(2L);
        var error = assertThrows(com.qblog.common.exception.BusinessException.class, () -> service.updateArticle(1L, dto));
        assertEquals(409, error.getCode());
        // 条件更新失败后不得继续改标签
        verify(associations, never()).remove(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test void partialUpdateKeepsUntouchedFields() {
        Article article = new Article(); article.setId(1L); article.setStatus(1); article.setVersion(0L);
        article.setSummary("keep"); article.setPublishTime(LocalDateTime.now());
        doReturn(article).when(service).getById(1L);
        doReturn(true).when(service).update(any(Article.class), any());
        // 全空的 DTO：status/tagIds 为 null 表示保持原状态，不得清空摘要或删光标签
        ArticleDTO partial = new ArticleDTO(); partial.setVersion(0L);
        service.updateArticle(1L, partial);
        var captor = ArgumentCaptor.forClass(Article.class);
        verify(service).update(captor.capture(), any());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals("keep", captor.getValue().getSummary());
        verify(associations, never()).remove(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test void createWithoutStatusDefaultsToDraft() {
        var auth = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        auth.setDetails(Map.of("userId", 7L));
        SecurityContextHolder.getContext().setAuthentication(auth);
        doReturn(true).when(service).save(any(Article.class));
        service.createArticle(new ArticleDTO());
        var captor = ArgumentCaptor.forClass(Article.class);
        verify(service).save(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertNull(captor.getValue().getPublishTime());
    }

    @Test @SuppressWarnings("unchecked")
    void hotAndLatestUseFreshProjectedQueriesInsteadOfSnapshotCache() {
        doAnswer(call -> {
            LambdaQueryWrapper<Article> query = call.getArgument(1);
            assertFalse(query.getSqlSelect().contains("content"));
            assertTrue(query.getSqlSelect().contains("summary"));
            return new Page<Article>(1, 5).setRecords(List.of());
        }).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));
        assertTrue(service.getHotArticles(5).isEmpty());
        assertTrue(service.getLatestArticles(5).isEmpty());
    }

    @Test void tagCountUsesFilteredArticleSide() throws Exception {
        String sql = String.join(" ", TagMapper.class.getMethod("selectTagListWithArticleCount")
                .getAnnotation(Select.class).value());
        assertTrue(sql.contains("COUNT(a.id)"));
        assertTrue(sql.contains("a.status = 1"));
        assertFalse(sql.contains("COUNT(at.article_id)"));
    }
}
