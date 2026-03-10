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
        registry.addInterceptor(new CacheControlInterceptor())
                .addPathPatterns("/articles/**", "/categories/**", "/tags/**");
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