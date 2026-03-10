package com.qblog.service;

import com.qblog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 浏览量计数服务
 * 使用 Redis 进行计数，定时同步到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final StringRedisTemplate redisTemplate;
    private final ArticleMapper articleMapper;

    private static final String VIEW_COUNT_KEY_PREFIX = "article:views:";

    /**
     * 增加文章浏览量（写入 Redis）
     */
    public void incrementViewCount(Long articleId) {
        String key = VIEW_COUNT_KEY_PREFIX + articleId;
        redisTemplate.opsForValue().increment(key);
    }

    /**
     * 定时任务：每 5 分钟同步浏览量到数据库
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDb() {
        log.debug("Starting view count sync...");

        try {
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
                log.info("Synced view counts for {} articles", syncCount);
            }
        } catch (Exception e) {
            log.error("View count sync failed", e);
        }
    }
}