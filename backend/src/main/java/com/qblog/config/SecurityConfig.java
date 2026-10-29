package com.qblog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.common.Result;
import com.qblog.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 配置
 * 采用声明式授权规则：公开只读接口放行，管理/写接口要求 ADMIN，其余要求登录
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final com.qblog.knowledge.KnowledgeTokenService knowledgeTokens;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .logout(logout -> logout.disable())
            .requestCache(cache -> cache.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register").denyAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers("/mcp").hasAuthority("MCP_TOKEN")
                // A private shared workspace: legacy reads, docs and readiness are admin-only too.
                .anyRequest().hasRole("ADMIN"))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleUnauthorized)
                .accessDeniedHandler(this::handleForbidden))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new com.qblog.filter.KnowledgeApiTokenFilter(knowledgeTokens), JwtAuthenticationFilter.class);

        return http.build();
    }

    private void handleUnauthorized(jakarta.servlet.http.HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.core.AuthenticationException e) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未授权，请先登录");
    }

    private void handleForbidden(jakarta.servlet.http.HttpServletRequest request,
                                 HttpServletResponse response,
                                 org.springframework.security.access.AccessDeniedException e) throws IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, "无权限访问该资源");
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(status, message)));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 安全：使用环境变量配置允许的源，避免允许所有源
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        } else {
            // 开发环境默认值
            configuration.setAllowedOrigins(List.of("http://localhost:3001", "http://localhost:3000"));
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
