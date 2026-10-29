package com.qblog.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.entity.User;
import com.qblog.filter.KnowledgeApiTokenFilter;
import com.qblog.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeTokenSecurityTest {
    private static final String SECRET="qnt_"+"a".repeat(43);
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final UserService users=mock(UserService.class);
    private final KnowledgeTokenService tokens=new KnowledgeTokenService(jdbc,new ObjectMapper(),users);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void invalidExpiredOrRevokedHasNoPrincipal() {
        when(jdbc.query(anyString(),org.mockito.ArgumentMatchers.<RowMapper<KnowledgeTokenService.Principal>>any(),eq(KnowledgeTokenService.hash(SECRET)))).thenReturn(List.of());
        assertNull(tokens.authenticate(SECRET));
        verify(jdbc).query(contains("revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP"),org.mockito.ArgumentMatchers.<RowMapper<KnowledgeTokenService.Principal>>any(),any());
        verifyNoInteractions(users);
    }
    @Test void disabledOrDemotedOwnerRejectedImmediately() {
        var p=new KnowledgeTokenService.Principal(1L,2L,Set.of("READ"),new KnowledgeTokenService.DirectoryScope(true,false,Set.of()));
        when(jdbc.query(anyString(),org.mockito.ArgumentMatchers.<RowMapper<KnowledgeTokenService.Principal>>any(),any())).thenReturn(List.of(p));
        User owner=new User(); owner.setRole(1); owner.setStatus(1); when(users.getById(2L)).thenReturn(owner);
        assertNotNull(tokens.authenticate(SECRET)); owner.setStatus(0); assertNull(tokens.authenticate(SECRET));
        owner.setStatus(1); owner.setRole(0); assertNull(tokens.authenticate(SECRET));
        verify(users,times(3)).getById(2L);
    }
    @Test void emptyDirectoryScopeAndTooLongExpiryRejected() {
        assertThrows(RuntimeException.class,() -> tokens.create(new KnowledgeTokenService.CreateRequest("x",Instant.now().plusSeconds(1000),Set.of("READ"),new KnowledgeTokenService.DirectoryScope(false,false,Set.of()))));
        assertThrows(RuntimeException.class,() -> tokens.create(new KnowledgeTokenService.CreateRequest("x",Instant.now().plusSeconds(366L*86400),Set.of("READ"),new KnowledgeTokenService.DirectoryScope(true,false,Set.of()))));
        verifyNoInteractions(jdbc);
    }
    @Test void apiTokenCannotUseAnyOtherEndpointEvenPublicLogin() throws Exception {
        for(String path:List.of("/knowledge/tokens","/articles","/auth/login","/health","/mcp/")) {
            MockHttpServletRequest request=new MockHttpServletRequest("GET",path); request.addHeader("Authorization","Bearer "+SECRET);
            MockHttpServletResponse response=new MockHttpServletResponse(); var chain=mock(jakarta.servlet.FilterChain.class);
            new KnowledgeApiTokenFilter(tokens).doFilter(request,response,chain);
            assertEquals(403,response.getStatus()); verifyNoInteractions(chain);
        }
        verifyNoInteractions(jdbc);
    }
    @Test void browserJwtCannotUseMcpAndRegisterAlwaysForbidden() throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest("POST","/mcp"); request.addHeader("Authorization","Bearer jwt");
        MockHttpServletResponse response=new MockHttpServletResponse(); var chain=mock(jakarta.servlet.FilterChain.class);
        new KnowledgeApiTokenFilter(tokens).doFilter(request,response,chain); assertEquals(401,response.getStatus());
        request=new MockHttpServletRequest("POST","/auth/register"); response=new MockHttpServletResponse();
        new KnowledgeApiTokenFilter(tokens).doFilter(request,response,chain); assertEquals(403,response.getStatus()); verifyNoInteractions(chain);
    }
    @Test void creationReturnsSecretOnceButPersistsOnlyHash() throws Exception {
        User owner=new User(); owner.setStatus(1); owner.setRole(1); when(users.getById(2L)).thenReturn(owner);
        var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin",null,List.of());
        auth.setDetails(Map.of("userId",2L)); SecurityContextHolder.getContext().setAuthentication(auth);
        java.sql.Connection connection=mock(java.sql.Connection.class); java.sql.PreparedStatement statement=mock(java.sql.PreparedStatement.class);
        when(connection.prepareStatement(anyString(),eq(java.sql.Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        doAnswer(invocation -> {
            org.springframework.jdbc.core.PreparedStatementCreator creator=invocation.getArgument(0);
            org.springframework.jdbc.support.KeyHolder key=invocation.getArgument(1);
            creator.createPreparedStatement(connection); key.getKeyList().add(Map.of("id",1L)); return 1;
        }).when(jdbc).update(any(org.springframework.jdbc.core.PreparedStatementCreator.class),any(org.springframework.jdbc.support.KeyHolder.class));
        var response=tokens.create(new KnowledgeTokenService.CreateRequest("test",Instant.now().plusSeconds(3600),Set.of("READ"),new KnowledgeTokenService.DirectoryScope(false,true,Set.of())));
        assertTrue(response.secret().matches("qnt_[A-Za-z0-9_-]{43}"));
        verify(statement).setString(3,KnowledgeTokenService.hash(response.secret()));
        verify(statement,never()).setString(anyInt(),eq(response.secret()));
        assertEquals(1L,response.id());
    }
    @Test void hashIsSha256AndDoesNotStoreSecret() { assertEquals(64,KnowledgeTokenService.hash(SECRET).length()); assertFalse(KnowledgeTokenService.hash(SECRET).contains("qnt_")); }
}
