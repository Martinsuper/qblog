package com.qblog.filter;

import com.qblog.common.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证过滤器
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 跳过登录、注册等公开接口
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/register") ||
            path.startsWith("/api/test/") ||
            path.startsWith("/api/doc") ||
            path.startsWith("/api/webjars/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 其他接口需要验证 token
        String token = getTokenFromRequest(request);

        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            // token 无效，返回 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\"}");
            return;
        }

        // token 有效，设置认证信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
        authentication.setDetails(attributes);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
