package com.qblog.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * 安全上下文工具类
 * 统一从 SecurityContext 中获取当前登录用户信息，避免各处重复解析
 */
public final class SecurityUtils {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID（未登录返回 null）
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Map<?, ?> details) {
            Object userId = details.get("userId");
            if (userId instanceof Long id) {
                return id;
            }
        }
        return null;
    }

    /**
     * 当前登录用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前登录用户是否为管理员
     */
    public static boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }
}
