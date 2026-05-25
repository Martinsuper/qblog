package com.qblog.service.impl;

import cn.hutool.json.JSONUtil;
import com.qblog.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 本地缓存服务实现（基于 ConcurrentHashMap）
 * 当 Redis 未启用时使用此实现作为降级方案
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "false", matchIfMissing = false)
public class LocalCacheServiceImpl implements CacheService {

    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    // 计数器存储
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /**
     * 缓存条目（包含值和过期时间）
     */
    private static class CacheEntry {
        final String value;
        final long expireTime;

        CacheEntry(String value, Duration ttl) {
            this.value = value;
            this.expireTime = ttl != null ? System.currentTimeMillis() + ttl.toMillis() : Long.MAX_VALUE;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                if (entry != null) {
                    cache.remove(key); // 清理过期条目
                }
                return null;
            }
            return JSONUtil.toBean(entry.value, type);
        } catch (Exception e) {
            log.warn("Failed to get local cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                if (entry != null) {
                    cache.remove(key);
                }
                return null;
            }
            return JSONUtil.toList(entry.value, elementType);
        } catch (Exception e) {
            log.warn("Failed to get list from local cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        try {
            String json = JSONUtil.toJsonStr(value);
            cache.put(key, new CacheEntry(json, ttl));
        } catch (Exception e) {
            log.warn("Failed to set local cache for key: {}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        // 本地缓存不支持 pattern 匹配，简单实现：遍历所有 key
        String regexPattern = pattern.replace("*", ".*");
        List<String> keysToDelete = new ArrayList<>();
        for (String key : cache.keySet()) {
            if (key.matches(regexPattern)) {
                keysToDelete.add(key);
            }
        }
        for (String key : keysToDelete) {
            cache.remove(key);
        }
        log.debug("Deleted {} local cache entries matching pattern: {}", keysToDelete.size(), pattern);
    }

    @Override
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                cache.remove(key);
            }
            return false;
        }
        return true;
    }

    @Override
    public Long increment(String key) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        return counter.incrementAndGet();
    }

    @Override
    public Long getAndReset(String key) {
        AtomicLong counter = counters.get(key);
        if (counter == null) {
            return 0L;
        }
        return counter.getAndSet(0);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        // 本地缓存没有分布式锁，直接实现
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，直接加载
        T data = loader.get();
        if (data != null) {
            set(key, data, ttl);
        } else {
            // 缓存空值，防止穿透（设置较短的 TTL）
            set(key, data, Duration.ofMinutes(1));
        }
        return data;
    }

    @Override
    public <T> List<T> getOrLoadList(String key, Class<T> elementType, Duration ttl, Supplier<List<T>> loader) {
        // 本地缓存没有分布式锁，直接实现
        List<T> cached = getList(key, elementType);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，直接加载
        List<T> data = loader.get();
        if (data != null && !data.isEmpty()) {
            set(key, data, ttl);
        } else {
            // 缓存空列表，防止穿透（设置较短的 TTL）
            set(key, data, Duration.ofMinutes(1));
        }
        return data;
    }

    /**
     * 定期清理过期缓存（每5分钟执行）
     */
    @Scheduled(fixedRate = 300000)
    public void cleanExpiredCache() {
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }
        for (String key : expiredKeys) {
            cache.remove(key);
        }
        if (!expiredKeys.isEmpty()) {
            log.debug("Cleaned {} expired local cache entries", expiredKeys.size());
        }
    }
}