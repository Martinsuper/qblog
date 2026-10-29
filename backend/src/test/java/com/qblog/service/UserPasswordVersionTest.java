package com.qblog.service;

import com.qblog.common.JwtUtil;
import com.qblog.entity.User;
import com.qblog.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPasswordVersionTest {
    @Test void passwordRevocationNeverReadsOrRepopulatesCache() {
        CacheService cache = mock(CacheService.class);
        UserServiceImpl service = spy(new UserServiceImpl(mock(PasswordEncoder.class), mock(JwtUtil.class), cache));
        User old = new User(); old.setPasswordVersion(0);
        User changed = new User(); changed.setPasswordVersion(1);
        doReturn(old, changed, null).when(service).getById(7L);
        assertEquals(0, service.getPasswordVersion(7L));
        assertEquals(1, service.getPasswordVersion(7L));
        assertEquals(-1, service.getPasswordVersion(7L));
        verifyNoInteractions(cache);
    }
}
