package com.qblog.service.impl;

import cn.hutool.json.JSONUtil;
import com.qblog.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 缓存服务实现
 * 仅在 redis.enabled=true 时生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return JSONUtil.toBean(value, type);
        } catch (Exception e) {
            log.warn("Failed to get cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return JSONUtil.toList(value, elementType);
        } catch (Exception e) {
            log.warn("Failed to get list cache for key: {}", key, e);
            return null;
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
            // 使用 SCAN 替代 KEYS，避免阻塞 Redis
            List<String> keysToDelete = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

            Cursor<String> cursor = redisTemplate.scan(options);
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
                // 批量删除，避免一次性删除太多
                if (keysToDelete.size() >= 100) {
                    redisTemplate.delete(keysToDelete);
                    keysToDelete.clear();
                }
            }
            cursor.close();

            // 删除剩余的 key
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
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

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        // 先尝试从缓存获取
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = LOCK_PREFIX + key;
        boolean locked = false;

        try {
            // 尝试获取锁
            locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT)
            );

            if (locked) {
                // 获取锁成功，执行 loader 加载数据
                T data = loader.get();
                if (data != null) {
                    set(key, data, ttl);
                } else {
                    // 缓存空值，防止缓存穿透（设置较短的 TTL）
                    set(key, data, Duration.ofMinutes(1));
                }
                return data;
            } else {
                // 获取锁失败，等待一小段时间后重试读取缓存
                Thread.sleep(100);
                cached = get(key, type);
                if (cached != null) {
                    return cached;
                }
                // 如果还是没有，直接执行 loader（降级策略）
                return loader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for cache lock: {}", key);
            return loader.get();
        } catch (Exception e) {
            log.error("Failed to get or load cache for key: {}", key, e);
            return loader.get();
        } finally {
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    public <T> List<T> getOrLoadList(String key, Class<T> elementType, Duration ttl, Supplier<List<T>> loader) {
        // 先尝试从缓存获取
        List<T> cached = getList(key, elementType);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = LOCK_PREFIX + key;
        boolean locked = false;

        try {
            // 尝试获取锁
            locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT)
            );

            if (locked) {
                // 获取锁成功，执行 loader 加载数据
                List<T> data = loader.get();
                if (data != null && !data.isEmpty()) {
                    set(key, data, ttl);
                } else {
                    // 缓存空列表，防止缓存穿透（设置较短的 TTL）
                    set(key, data, Duration.ofMinutes(1));
                }
                return data;
            } else {
                // 获取锁失败，等待一小段时间后重试读取缓存
                Thread.sleep(100);
                cached = getList(key, elementType);
                if (cached != null) {
                    return cached;
                }
                // 如果还是没有，直接执行 loader（降级策略）
                return loader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for cache lock: {}", key);
            return loader.get();
        } catch (Exception e) {
            log.error("Failed to get or load list cache for key: {}", key, e);
            return loader.get();
        } finally {
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}