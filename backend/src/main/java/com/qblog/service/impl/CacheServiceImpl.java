package com.qblog.service.impl;

import cn.hutool.json.JSONUtil;
import com.qblog.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(JSONUtil.toBean(value, type));
        } catch (Exception e) {
            log.warn("Failed to get cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        try {
            String json = JSONUtil.toJsonStr(value);
            redisTemplate.opsForValue().set(key, json, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Failed to set cache for key: {}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete cache for key: {}", key, e);
        }
    }

    @Override
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to delete cache by pattern: {}", pattern, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check cache existence for key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Failed to increment counter for key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Long getAndReset(String key) {
        try {
            String value = redisTemplate.opsForValue().getAndSet(key, "0");
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to get and reset counter for key: {}", key, e);
            return 0L;
        }
    }
}