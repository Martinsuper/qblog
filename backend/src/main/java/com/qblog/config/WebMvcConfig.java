package com.qblog.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Component
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 安全 Headers 拦截器
        registry.addInterceptor(new SecurityHeadersInterceptor())
                .addPathPatterns("/**");

        // 缓存控制拦截器
        registry.addInterceptor(new CacheControlInterceptor())
                .addPathPatterns("/articles/**", "/categories/**", "/tags/**");
    }

    /**
     * 安全 Headers 拦截器
     */
    private static class SecurityHeadersInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 防止 XSS
            response.setHeader("X-XSS-Protection", "1; mode=block");
            // 防止内容类型嗅探
            response.setHeader("X-Content-Type-Options", "nosniff");
            // 防止点击劫持
            response.setHeader("X-Frame-Options", "DENY");
            // HSTS（仅 HTTPS）
            if (request.isSecure()) {
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }
            return true;
        }
    }

    /**
     * HTTP 缓存控制拦截器
     */
    private static class CacheControlInterceptor implements HandlerInterceptor {

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            // 只对 GET 请求添加缓存头
            if (!"GET".equals(request.getMethod())) {
                return;
            }

            String uri = request.getRequestURI();

            // 文章详情 API: 缓存 2 分钟
            if (uri.matches("/api/articles/\\d+$")) {
                response.setHeader("Cache-Control", "public, max-age=120");
                return;
            }

            // 文章列表 API: 缓存 1 分钟
            if (uri.equals("/api/articles")) {
                response.setHeader("Cache-Control", "public, max-age=60");
                return;
            }

            // 分类和标签列表: 缓存 5 分钟
            if (uri.equals("/api/categories") || uri.equals("/api/tags")) {
                response.setHeader("Cache-Control", "public, max-age=300");
            }
        }
    }
}