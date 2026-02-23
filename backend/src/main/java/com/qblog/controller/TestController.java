package com.qblog.controller;

import com.qblog.common.Result;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private final PasswordEncoder passwordEncoder;

    public TestController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 测试接口 - 公开访问
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello, Q Blog!");
    }

    /**
     * 测试密码加密
     */
    @PostMapping("/password/check")
    public Result<Map<String, Object>> checkPassword(@RequestBody Map<String, String> request) {
        String rawPassword = request.get("password");
        String encodedPassword = request.get("encodedPassword");
        
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        
        Map<String, Object> result = new HashMap<>();
        result.put("matches", matches);
        result.put("rawPassword", rawPassword);
        
        // 生成新密码
        String newEncoded = passwordEncoder.encode(rawPassword);
        result.put("newEncodedPassword", newEncoded);
        
        return Result.success(result);
    }
}
