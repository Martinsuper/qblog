package com.qblog.common.aspect;

import com.qblog.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final JwtUtil jwtUtil;

    /**
     * 记录登录操作
     */
    @AfterReturning(pointcut = "execution(* com.qblog.controller.AuthController.login(..))", returning = "result")
    public void logLogin(JoinPoint joinPoint, Object result) {
        try {
            Object[] args = joinPoint.getArgs();
            String username = extractUsername(args);
            String ip = getClientIP();

            log.info("用户登录成功 - 用户名: {}, IP: {}", username, ip);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    /**
     * 记录登录失败
     */
    @AfterThrowing(pointcut = "execution(* com.qblog.controller.AuthController.login(..))", throwing = "ex")
    public void logLoginFailure(JoinPoint joinPoint, Exception ex) {
        try {
            Object[] args = joinPoint.getArgs();
            String username = extractUsername(args);
            String ip = getClientIP();

            log.warn("用户登录失败 - 用户名: {}, IP: {}, 原因: {}", username, ip, ex.getMessage());
        } catch (Exception e) {
            log.error("记录登录失败日志失败", e);
        }
    }

    /**
     * 记录注册操作
     */
    @AfterReturning(pointcut = "execution(* com.qblog.controller.AuthController.register(..))")
    public void logRegister(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            String username = extractUsername(args);
            String ip = getClientIP();

            log.info("用户注册成功 - 用户名: {}, IP: {}", username, ip);
        } catch (Exception e) {
            log.error("记录注册日志失败", e);
        }
    }

    /**
     * 记录文章创建
     */
    @AfterReturning(pointcut = "execution(* com.qblog.controller.ArticleController.createArticle(..))", returning = "result")
    public void logArticleCreate(JoinPoint joinPoint, Object result) {
        try {
            String username = getCurrentUsername();
            log.info("文章创建 - 操作用户: {}", username);
        } catch (Exception e) {
            log.error("记录文章创建日志失败", e);
        }
    }

    /**
     * 记录文章更新
     */
    @AfterReturning(pointcut = "execution(* com.qblog.controller.ArticleController.updateArticle(..))", returning = "result")
    public void logArticleUpdate(JoinPoint joinPoint, Object result) {
        try {
            Object[] args = joinPoint.getArgs();
            Long articleId = (Long) args[0];
            String username = getCurrentUsername();

            log.info("文章更新 - 文章ID: {}, 操作用户: {}", articleId, username);
        } catch (Exception e) {
            log.error("记录文章更新日志失败", e);
        }
    }

    /**
     * 记录文章删除
     */
    @AfterReturning(pointcut = "execution(* com.qblog.controller.ArticleController.deleteArticle(..))")
    public void logArticleDelete(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            Long articleId = (Long) args[0];
            String username = getCurrentUsername();

            log.info("文章删除 - 文章ID: {}, 操作用户: {}", articleId, username);
        } catch (Exception e) {
            log.error("记录文章删除日志失败", e);
        }
    }

    /**
     * 记录密码修改
     */
    @AfterReturning(pointcut = "execution(* com.qblog.controller.AuthController.changePassword(..))")
    public void logPasswordChange(JoinPoint joinPoint) {
        try {
            String username = getCurrentUsername();
            String ip = getClientIP();

            log.info("密码修改 - 用户: {}, IP: {}", username, ip);
        } catch (Exception e) {
            log.error("记录密码修改日志失败", e);
        }
    }

    /**
     * 从参数中提取用户名
     */
    private String extractUsername(Object[] args) {
        if (args != null && args.length > 0) {
            Object dto = args[0];
            try {
                // 使用反射获取 username 字段
                var field = dto.getClass().getDeclaredField("username");
                field.setAccessible(true);
                return (String) field.get(dto);
            } catch (Exception e) {
                return "unknown";
            }
        }
        return "unknown";
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader("Authorization");

                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    return jwtUtil.getUsernameFromToken(token);
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户名失败", e);
        }
        return "anonymous";
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

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
