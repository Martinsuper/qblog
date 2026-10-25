package com.qblog.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 请求工具类
 */
public final class WebUtils {

    private WebUtils() {
    }

    /**
     * 获取客户端真实 IP
     * 仅当直连方是内网/回环地址（通常是反向代理）时才信任 X-Forwarded-For / X-Real-IP，
     * 防止客户端伪造代理头绕过基于 IP 的限流与统计
     */
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // XFF 可能包含多级代理，第一个为原始客户端
                return ip.split(",")[0].trim();
            }
            ip = request.getHeader("X-Real-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    /**
     * 判断是否为可信代理地址（回环 / 内网网段）
     */
    private static boolean isTrustedProxy(String ip) {
        if (ip == null) {
            return false;
        }
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)
                || ip.startsWith("10.") || ip.startsWith("192.168.")
                || isPrivate172(ip);
    }

    private static boolean isPrivate172(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        try {
            int second = Integer.parseInt(ip.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
