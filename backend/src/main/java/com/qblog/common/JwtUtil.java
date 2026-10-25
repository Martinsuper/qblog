package com.qblog.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    public void validateSecretKey() {
        // fail-fast：密钥过短直接拒绝启动，而不是仅告警后带病运行
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException(String.format(
                "JWT secret 长度不足（当前 %d 字符），HS256 至少需要 32 字符。请通过 JWT_SECRET 环境变量配置",
                secret == null ? 0 : secret.trim().length()));
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token（携带角色与密码版本，用于服务端鉴权和改密后失效旧 Token）
     */
    public String generateToken(Long userId, String username, Integer role, Integer passwordVersion) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role == null ? 0 : role)
                .claim("pwdVer", passwordVersion == null ? 0 : passwordVersion)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token，无效或过期时返回 null（一次解析，避免重复验签）
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        return parseClaims(token) != null;
    }
}
