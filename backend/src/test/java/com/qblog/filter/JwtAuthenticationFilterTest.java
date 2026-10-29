package com.qblog.filter;

import com.qblog.common.JwtUtil;
import com.qblog.entity.User;
import com.qblog.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void authenticate(Integer status, Integer currentRole, Integer currentVersion, Integer tokenVersion) throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        UserService users = mock(UserService.class);
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("7");
        when(claims.get("pwdVer", Integer.class)).thenReturn(tokenVersion);
        User user = new User();
        user.setId(7L); user.setStatus(status); user.setRole(currentRole);
        user.setUsername("current-name"); user.setPasswordVersion(currentVersion);
        when(users.getById(7L)).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        FilterChain chain = mock(FilterChain.class);
        new JwtAuthenticationFilter(jwt, users).doFilter(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(eq(request), any());
        verify(users, never()).getPasswordVersion(any());
        verify(claims, never()).get("role", Integer.class);
    }

    @Test void disabledUserCannotAuthenticate() throws Exception {
        authenticate(0, 1, 0, 0);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void demotionUsesCurrentRoleAndUsername() throws Exception {
        authenticate(1, 0, 0, 0);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("current-name", auth.getName());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
    }
    @Test void currentAdminKeepsAdminAccess() throws Exception {
        authenticate(1, 1, 0, 0);
        assertEquals("ROLE_ADMIN", SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority());
    }
    @Test void missingStatusFailsClosed() throws Exception {
        authenticate(null, 1, 0, 0);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void legacyTokenCannotBypassChangedPassword() throws Exception {
        authenticate(1, 1, 1, null);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void changedPasswordRejectsOldToken() throws Exception {
        authenticate(1, 1, 1, 0);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void legacyTokenOnlyMatchesUnchangedVersion() throws Exception {
        authenticate(1, 0, null, null);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
