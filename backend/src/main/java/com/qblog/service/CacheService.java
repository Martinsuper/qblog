package com.qblog.service;

import java.time.Duration;
import java.util.Optional;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 获取缓存值
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 设置缓存值
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 删除匹配模式的所有缓存
     */
    void deleteByPattern(String pattern);

    /**
     * 检查缓存是否存在
     */
    boolean exists(String key);

    /**
     * 增加计数器
     */
    Long increment(String key);

    /**
     * 获取计数器值并重置
     */
    Long getAndReset(String key);
}