package com.qblog.service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 获取缓存值（返回 null 如果不存在）
     */
    <T> T get(String key, Class<T> type);

    /**
     * 获取缓存列表
     */
    <T> List<T> getList(String key, Class<T> elementType);

    /**
     * 设置缓存值
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 删除匹配模式的所有缓存（使用 SCAN 避免阻塞）
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

    /**
     * 获取缓存，如果不存在则执行 loader 并缓存结果（带分布式锁防止缓存击穿）
     */
    <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader);

    /**
     * 获取缓存列表，如果不存在则执行 loader 并缓存结果（带分布式锁防止缓存击穿）
     */
    <T> List<T> getOrLoadList(String key, Class<T> elementType, Duration ttl, Supplier<List<T>> loader);
}