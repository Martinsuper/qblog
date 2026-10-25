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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 登录/注册
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                // 公开只读接口（博客内容对所有人可见）
                .requestMatchers(HttpMethod.GET,
                    "/articles", "/articles/hot", "/articles/latest",
                    "/articles/{id}", "/articles/{id}/related",
                    "/categories", "/categories/{id}",
                    "/tags", "/tags/{id}").permitAll()
                // 健康检查与 API 文档
                .requestMatchers("/health", "/health/**",
                    "/doc.html", "/doc/**", "/webjars/**",
                    "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                // 管理接口：仅管理员（含草稿列表、备份的完整读写）
                .requestMatchers("/articles/admin/**", "/backup/**").hasRole("ADMIN")
                // 内容写操作：仅管理员
                .requestMatchers(HttpMethod.POST, "/articles", "/categories", "/tags").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/articles/**", "/categories/**", "/tags/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/articles/**", "/categories/**", "/tags/**").hasRole("ADMIN")
                // 其余接口（个人中心等）：登录即可
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleUnauthorized)
                .accessDeniedHandler(this::handleForbidden))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
