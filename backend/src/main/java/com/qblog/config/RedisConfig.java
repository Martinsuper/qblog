package com.qblog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Redis 可选开关保留为 spring.redis.enabled；连接参数遵循 Boot 3 的
 * spring.data.redis。启用时复用 Boot 配置，避免手工工厂忽略认证、超时等参数。
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    @Import(RedisAutoConfiguration.class)
    static class EnabledRedisConfiguration {
    }
}
