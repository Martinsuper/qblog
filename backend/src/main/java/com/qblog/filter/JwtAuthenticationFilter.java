package com.qblog.filter;

import com.qblog.common.JwtUtil;
import com.qblog.common.SecurityUtils;
import com.qblog.service.UserService;
import com.qblog.entity.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 认证过滤器
 * 只负责"认证"：解析 Token 并把用户身份/角色写入 SecurityContext；
 * "授权"（哪些路径需要什么角色）统一由 SecurityConfig 的声明式规则处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        // Integration credentials are authenticated only by the exact-/mcp API-token filter.
        if (StringUtils.hasText(token) && !token.startsWith("qnt_")) {
            try {
                Claims claims = jwtUtil.parseClaims(token);
                if (claims != null) {
                    Long userId = Long.parseLong(claims.getSubject());
                    // 密码版本校验：改密后旧 Token 立即失效
                    Integer tokenPwdVer = claims.get("pwdVer", Integer.class);
                    // Always read one current DB snapshot: never trust cached revocation data or token roles.
                    User user = userService.getById(userId);
                    Integer currentPwdVer = user == null ? null
                            : (user.getPasswordVersion() == null ? 0 : user.getPasswordVersion());
                    Integer expected = tokenPwdVer == null ? 0 : tokenPwdVer;

                    if (user != null && Integer.valueOf(1).equals(user.getStatus())
                            && expected.equals(currentPwdVer)) {
                        Integer role = user.getRole();
                        String username = user.getUsername();

                        List<SimpleGrantedAuthority> authorities = role != null && role == 1
                                ? List.of(new SimpleGrantedAuthority(SecurityUtils.ROLE_ADMIN))
                                : List.of(new SimpleGrantedAuthority(SecurityUtils.ROLE_USER));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(username, null, authorities);

                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("userId", userId);
                        authentication.setDetails(attributes);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (NumberFormatException e) {
                log.debug("Token subject 不是合法的用户 ID，忽略该 Token");
            } catch (Exception e) {
                // 认证失败不阻断请求，交由授权规则决定是否返回 401
                log.debug("Token 认证失败: {}", e.getMessage());
            }
        }

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
