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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleServiceRegressionTest {
    CacheService cache = mock(CacheService.class);
    ArticleTagService associations = mock(ArticleTagService.class);
    ArticleServiceImpl service = spy(new ArticleServiceImpl(mock(UserService.class),
            mock(CategoryService.class), mock(TagService.class), associations, cache,
            mock(ViewCountService.class)));

    @BeforeEach void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Article.class);
    }
    @AfterEach void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    @Test void updateInvalidatesOnlyAfterCommit() {
        Article article = new Article(); article.setId(1L); article.setStatus(1);
        doReturn(article).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(Article.class));
        ArticleDTO dto = new ArticleDTO(); dto.setStatus(0);
        TransactionSynchronizationManager.initSynchronization();
        service.updateArticle(1L, dto);
        verifyNoInteractions(cache);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cache).delete("article:detail:1");
        verify(cache).deleteByPattern("article:hot*");
        verify(cache).deleteByPattern("article:latest*");
    }

    @Test void rolledBackUpdateDoesNotInvalidate() {
        Article article = new Article(); article.setId(1L); article.setStatus(1);
        doReturn(article).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(Article.class));
        ArticleDTO dto = new ArticleDTO(); dto.setStatus(0);
        TransactionSynchronizationManager.initSynchronization();
        service.updateArticle(1L, dto);
        TransactionSynchronizationManager.getSynchronizations().forEach(s ->
                s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(cache);
    }

    @Test void creationAlsoInvalidatesAfterCommit() {
        var auth = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        auth.setDetails(Map.of("userId", 7L));
        SecurityContextHolder.getContext().setAuthentication(auth);
        doAnswer(call -> { ((Article)call.getArgument(0)).setId(2L); return true; })
                .when(service).save(any(Article.class));
        ArticleDTO dto = new ArticleDTO(); dto.setStatus(1);
        TransactionSynchronizationManager.initSynchronization();
        service.createArticle(dto);
        verifyNoInteractions(cache);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cache).deleteByPattern("article:latest*");
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
        verifyNoInteractions(cache);
    }

    @Test void tagCountUsesFilteredArticleSide() throws Exception {
        String sql = String.join(" ", TagMapper.class.getMethod("selectTagListWithArticleCount")
                .getAnnotation(Select.class).value());
        assertTrue(sql.contains("COUNT(a.id)"));
        assertTrue(sql.contains("a.status = 1"));
        assertFalse(sql.contains("COUNT(at.article_id)"));
    }
}
