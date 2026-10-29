package com.qblog.filter;

import com.qblog.knowledge.KnowledgeTokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class KnowledgeApiTokenFilter extends OncePerRequestFilter {
    private final KnowledgeTokenService tokens;
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/auth/register".equals(path)) { reject(response, 403); return; }
        if (authorization != null && authorization.startsWith("Bearer qnt_")) {
            // Never let an API credential fall through into browser authentication, even on public endpoints.
            if (!"/mcp".equals(path)) { reject(response, 403); return; }
            KnowledgeTokenService.Principal principal;
            try { principal = tokens.authenticate(authorization.substring(7)); }
            catch (RuntimeException failure) { principal = null; }
            if (principal == null) { reject(response, 401); return; }
            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("MCP_TOKEN")));
            auth.setDetails(java.util.Map.of("userId", principal.userId()));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if ("/mcp".equals(path)) { reject(response, 401); return; }
        chain.doFilter(request,response);
    }
    private void reject(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":"+status+",\"message\":\"Access denied\",\"data\":null}");
    }
}
