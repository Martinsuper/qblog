package com.qblog.service;

import com.qblog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 浏览量计数服务
 * 使用 Redis 进行计数（Redis 启用时），定时同步到数据库
 * Redis 禁用时直接写入数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String VIEW_COUNT_KEY_PREFIX = "article:views:";

    // 本地计数器（Redis 禁用时使用）
    private final java.util.Map<Long, Long> localViewCounts = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 增加文章浏览量
     * Redis 启用时写入 Redis，禁用时直接写入数据库
     */
    public void incrementViewCount(Long articleId) {
        if (redisTemplate != null) {
            // Redis 启用，写入 Redis
            String key = VIEW_COUNT_KEY_PREFIX + articleId;
            redisTemplate.opsForValue().increment(key);
        } else {
            // Redis 禁用，使用本地计数器
            localViewCounts.merge(articleId, 1L, Long::sum);
        }
    }

    /**
     * 定时任务：每 5 分钟同步浏览量到数据库
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDb() {
        log.debug("Starting view count sync...");

        try {
            if (redisTemplate != null) {
                // Redis 启用，同步 Redis 数据到数据库
                syncFromRedis();
            } else {
                // Redis 禁用，同步本地计数器到数据库
                syncFromLocal();
            }
        } catch (Exception e) {
            log.error("View count sync failed", e);
        }
    }

    /**
     * 从 Redis 同步浏览量到数据库
     */
    private void syncFromRedis() {
        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        int syncCount = 0;
        for (String key : keys) {
            try {
                String idStr = key.replace(VIEW_COUNT_KEY_PREFIX, "");
                Long articleId = Long.parseLong(idStr);
                String countStr = redisTemplate.opsForValue().getAndSet(key, "0");

                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    if (count > 0) {
                        articleMapper.incrementViewCount(articleId, count);
                        syncCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to sync view count for key: {}", key, e);
            }
        }

        if (syncCount > 0) {
            log.info("Synced view counts from Redis for {} articles", syncCount);
        }
    }

    /**
     * 从本地计数器同步浏览量到数据库
     */
    private void syncFromLocal() {
        if (localViewCounts.isEmpty()) {
            return;
        }

        int syncCount = 0;
        for (java.util.Map.Entry<Long, Long> entry : localViewCounts.entrySet()) {
            try {
                Long articleId = entry.getKey();
                Long count = entry.getValue();

                if (count > 0) {
                    articleMapper.incrementViewCount(articleId, count);
                    localViewCounts.put(articleId, 0L); // 重置计数器
                    syncCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync local view count for article: {}", entry.getKey(), e);
            }
        }

        if (syncCount > 0) {
            log.info("Synced view counts from local cache for {} articles", syncCount);
        }
    }
}