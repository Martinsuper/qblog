package com.qblog;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encoded = encoder.encode(rawPassword);
        System.out.println("BCrypt encoded password for 'admin123': " + encoded);
        
        // 验证
        boolean matches = encoder.matches(rawPassword, encoded);
        System.out.println("Password matches: " + matches);
    }
}
