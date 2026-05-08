package com.qblog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置类
 * 根据 redis.enabled 配置决定是否启用 Redis
 * 主启动类已排除 RedisAutoConfiguration，这里手动配置
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * Redis Connection Factory Bean
     * 仅在 redis.enabled=true 时创建
     */
    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        log.info("Redis enabled: Creating RedisConnectionFactory");

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.setPassword(redisProperties.getPassword());
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Redis Template Bean
     * 仅在 redis.enabled=true 时创建
     */
    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("Redis enabled: Creating StringRedisTemplate");
        return new StringRedisTemplate(redisConnectionFactory);
    }
}