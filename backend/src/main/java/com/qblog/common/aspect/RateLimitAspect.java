package com.qblog.common.aspect;

import com.qblog.common.SecurityUtils;
import com.qblog.common.WebUtils;
import com.qblog.common.annotation.RateLimit;
import com.qblog.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流切面
 * Redis 启用时使用 Redis 固定窗口计数；Redis 禁用时降级为本地内存限流，
 * 保证登录等敏感接口在无 Redis 环境下仍有暴力破解防护
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /** 本地限流：key -> (窗口起始秒, 计数) */
    private final ConcurrentHashMap<String, LocalWindow> localWindows = new ConcurrentHashMap<>();

    /** 本地窗口表容量保护，防止恶意构造大量 key 导致内存膨胀 */
    private static final int MAX_LOCAL_WINDOWS = 100_000;

    @Before("@annotation(com.qblog.common.annotation.RateLimit)")
    public void doBefore(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return;
        }

        String key = buildKey(rateLimit, signature);
        long count = rateLimit.count();
        long period = rateLimit.period();

        boolean allowed = redisTemplate != null
                ? checkRateLimitWithRedis(key, count, period)
                : checkRateLimitLocally(key, count, period);

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }
    }

    /**
     * 构建限流 key
     */
    private String buildKey(RateLimit rateLimit, MethodSignature signature) {
        StringBuilder key = new StringBuilder(RATE_LIMIT_KEY_PREFIX);

        if (!rateLimit.key().isEmpty()) {
            key.append(rateLimit.key());
        } else {
            key.append(signature.getDeclaringTypeName())
               .append(".")
               .append(signature.getName());
        }

        switch (rateLimit.limitType()) {
            case IP -> key.append(":").append(getClientIP());
            case USER -> key.append(":").append(getUserId());
            case GLOBAL -> key.append(":global");
        }

        return key.toString();
    }

    /**
     * Redis 固定窗口计数限流
     */
    private boolean checkRateLimitWithRedis(String key, long maxCount, long period) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                return true;
            }

            // 第一次访问，设置过期时间
            if (currentCount == 1) {
                redisTemplate.expire(key, period, TimeUnit.SECONDS);
            }

            return currentCount <= maxCount;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}, falling back to local limiter", key, e);
            // Redis 故障时降级到本地限流，而不是直接放行
            return checkRateLimitLocally(key, maxCount, period);
        }
    }

    /**
     * 本地内存固定窗口限流（单机语义，足够应对单实例部署）
     */
    private boolean checkRateLimitLocally(String key, long maxCount, long period) {
        if (localWindows.size() > MAX_LOCAL_WINDOWS) {
            localWindows.clear();
        }
        long now = System.currentTimeMillis();
        long windowMillis = period * 1000;

        LocalWindow window = localWindows.compute(key, (k, w) -> {
            if (w == null || now - w.start >= windowMillis) {
                return new LocalWindow(now);
            }
            return w;
        });
        return window.count.incrementAndGet() <= maxCount;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIP() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown";
        }
        return WebUtils.getClientIp(attributes.getRequest());
    }

    /**
     * 获取用户 ID（从 SecurityContext 中获取，未登录时退化为 IP）
     */
    private String getUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        return getClientIP();
    }

    /** 本地固定窗口 */
    private static final class LocalWindow {
        final long start;
        final AtomicLong count = new AtomicLong();

        LocalWindow(long start) {
            this.start = start;
        }
    }
}
