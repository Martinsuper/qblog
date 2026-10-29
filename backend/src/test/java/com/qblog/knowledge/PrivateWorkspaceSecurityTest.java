package com.qblog.knowledge;

import com.qblog.common.JwtUtil;
import com.qblog.config.SecurityConfig;
import com.qblog.entity.User;
import com.qblog.filter.JwtAuthenticationFilter;
import com.qblog.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeTokenController.class)
@org.springframework.test.context.ContextConfiguration(classes={KnowledgeTokenController.class, McpController.class, SecurityConfig.class, JwtAuthenticationFilter.class, com.qblog.common.GlobalExceptionHandler.class})
class PrivateWorkspaceSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean KnowledgeTokenService tokens;
    @MockBean KnowledgeService notes;
    @MockBean JwtUtil jwt;
    @MockBean UserService users;
    private void jwt(int role) {
        Claims claims=mock(Claims.class); when(jwt.parseClaims("browser")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1"); when(claims.get("pwdVer",Integer.class)).thenReturn(0);
        User user=new User(); user.setId(1L); user.setUsername("admin"); user.setStatus(1); user.setRole(role); user.setPasswordVersion(0);
        when(users.getById(1L)).thenReturn(user);
    }
    @Test void allLegacyReadsAndDocsPrivate() throws Exception {
        for(String path:List.of("/articles","/categories","/tags","/doc.html","/v3/api-docs","/health/ready","/knowledge/tokens"))
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    @Test void registrationIsClosedEvenWithoutBodyOrCredentials() throws Exception { mvc.perform(post("/auth/register")).andExpect(status().isForbidden()); }
    @Test void adminJwtOnlyForTokenManagement() throws Exception {
        jwt(0); mvc.perform(get("/knowledge/tokens").header("Authorization","Bearer browser")).andExpect(status().isForbidden());
        jwt(1); when(tokens.list()).thenReturn(List.of());
        mvc.perform(get("/knowledge/tokens").header("Authorization","Bearer browser")).andExpect(status().isOk());
        mvc.perform(get("/knowledge/tokens").header("Authorization","Bearer qnt_"+"a".repeat(43))).andExpect(status().isForbidden());
    }
    @Test void mcpRequiresApiTokenJsonAndTrustedOrigin() throws Exception {
        String secret="qnt_"+"a".repeat(43);
        var p=new KnowledgeTokenService.Principal(1L,1L,Set.of("READ"),new KnowledgeTokenService.DirectoryScope(true,false,Set.of()));
        when(tokens.authenticate(secret)).thenReturn(p);
        String ping="{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";
        mvc.perform(post("/mcp").contentType("application/json").header("Authorization","Bearer "+secret).content(ping)).andExpect(status().isOk()).andExpect(jsonPath("result").exists());
        mvc.perform(post("/mcp").contentType("text/plain").header("Authorization","Bearer "+secret).content(ping)).andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/mcp").contentType("application/json").header("Authorization","Bearer "+secret).header("Origin","https://evil.example").content(ping)).andExpect(status().isForbidden());
        mvc.perform(get("/mcp").header("Authorization","Bearer "+secret)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/mcp").contentType("application/json").header("Authorization","Bearer browser").content(ping)).andExpect(status().isUnauthorized());
    }
    @Test void untrustedOriginDeniedButCliAllowed() throws Exception {
        jwt(1); when(tokens.list()).thenReturn(List.of());
        mvc.perform(get("/knowledge/tokens").header("Authorization","Bearer browser").header("Origin","https://evil.example")).andExpect(status().isForbidden());
        mvc.perform(get("/knowledge/tokens").header("Authorization","Bearer browser")).andExpect(status().isOk());
        mvc.perform(options("/mcp").header("Origin","https://evil.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
}
