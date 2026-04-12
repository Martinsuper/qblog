package com.qblog.common.aspect;

import com.qblog.common.annotation.RateLimit;
import com.qblog.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * Redis 启用时使用 Redis 实现滑动窗口限流
 * Redis 禁用时跳过限流检查
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Before("@annotation(com.qblog.common.annotation.RateLimit)")
    public void doBefore(JoinPoint joinPoint) {
        // Redis 未启用，跳过限流检查
        if (redisTemplate == null) {
            log.debug("Redis disabled, skipping rate limit check");
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit != null) {
            String key = buildKey(rateLimit, signature);
            long count = rateLimit.count();
            long period = rateLimit.period();

            // 使用 Redis 实现滑动窗口限流
            boolean allowed = checkRateLimit(key, count, period);

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {}", key);
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
        }
    }

    /**
     * 构建限流 key
     */
    private String buildKey(RateLimit rateLimit, MethodSignature signature) {
        StringBuilder key = new StringBuilder(RATE_LIMIT_KEY_PREFIX);

        // 添加自定义 key 或方法签名
        if (!rateLimit.key().isEmpty()) {
            key.append(rateLimit.key());
        } else {
            key.append(signature.getDeclaringTypeName())
               .append(".")
               .append(signature.getName());
        }

        // 根据限流类型添加后缀
        switch (rateLimit.limitType()) {
            case IP -> key.append(":").append(getClientIP());
            case USER -> key.append(":").append(getUserId());
            case GLOBAL -> key.append(":global");
        }

        return key.toString();
    }

    /**
     * 检查是否超过限流阈值（使用 Redis 计数器）
     */
    private boolean checkRateLimit(String key, long maxCount, long period) {
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
            log.error("Rate limit check failed for key: {}", key, e);
            // 限流检查失败时，允许请求通过（降级策略）
            return true;
        }
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

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个 IP 的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取用户 ID（从 JWT 或 Session 中获取）
     */
    private String getUserId() {
        // TODO: 从 JWT token 中解析用户 ID
        // 这里暂时返回 IP 作为替代
        return getClientIP();
    }
}
