package com.qblog.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key 前缀（默认使用方法签名）
     */
    String key() default "";

    /**
     * 时间窗口（秒）
     */
    int period() default 60;

    /**
     * 时间窗口内最大请求次数
     */
    int count() default 100;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.IP;

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 根据 IP 限流
         */
        IP,
        /**
         * 根据用户 ID 限流
         */
        USER,
        /**
         * 全局限流
         */
        GLOBAL
    }
}
