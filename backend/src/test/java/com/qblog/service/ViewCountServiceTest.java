package com.qblog.service;

import com.qblog.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ViewCountServiceTest {
    @Test void incrementsDuringDbWriteAreNotOverwritten() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        ViewCountService service = new ViewCountService(mapper);
        service.incrementViewCount(1L, "visitor-1");
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.getAndSet(false)) service.incrementViewCount(1L, "visitor-2");
            return 1;
        }).when(mapper).incrementViewCount(1L, 1L);
        service.syncViewCountToDb();
        service.syncViewCountToDb();
        verify(mapper, times(2)).incrementViewCount(1L, 1L);
    }

    @Test void failedLocalDrainIsRestoredAlongsideNewViews() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        ViewCountService service = new ViewCountService(mapper);
        service.incrementViewCount(1L, "visitor-1");
        doAnswer(call -> {
            service.incrementViewCount(1L, "visitor-2");
            throw new IllegalStateException("DB unavailable");
        }).when(mapper).incrementViewCount(1L, 1L);
        service.syncViewCountToDb();
        service.syncViewCountToDb();
        verify(mapper).incrementViewCount(1L, 2L);
    }

    @Test @SuppressWarnings("unchecked")
    void redisFailedBatchIsNotAcknowledgedAndCanBeRetried() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(startsWith("article:viewlock:"), anyString(), any(Duration.class))).thenReturn(true);
        // Each scan opens a fresh cursor, including scans on the retry cycle.
        when(redis.scan(any(ScanOptions.class))).thenAnswer(call -> {
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenReturn(true, false);
            String pattern = ((ScanOptions)call.getArgument(0)).getPattern();
            when(cursor.next()).thenReturn(pattern.startsWith("article:views:")
                    ? "article:views:1" : "article:viewpending:1");
            return cursor;
        });
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenAnswer(call -> {
            RedisScript<?> script = call.getArgument(0);
            return script.getResultType() == String.class ? "5" : 1L;
        });
        doThrow(new IllegalStateException("DB unavailable")).doReturn(1)
                .when(mapper).incrementViewCount(1L, 5L);
        ViewCountService service = new ViewCountService(mapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        service.syncViewCountToDb();
        // No ack on failure: only DRAIN (string script) and token-safe unlock.
        verify(redis, never()).execute(argThat((RedisScript<?> script) ->
                script != null && script.getScriptAsString().contains("KEYS[2]) end")), anyList(), any(Object[].class));
        service.syncViewCountToDb();
        verify(mapper, times(2)).incrementViewCount(1L, 5L);
        verify(redis, never()).keys(anyString());
        verify(redis, never()).delete(anyString());
    }
}
