package com.qblog.controller;

import com.qblog.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Tag(name = "健康检查")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 存活探针 - 检查服务是否运行
     */
    @Operation(summary = "存活探针")
    @GetMapping
    public Result<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        return Result.success(status);
    }

    /**
     * 就绪探针 - 检查服务是否准备好接收请求
     */
    @Operation(summary = "就绪探针（检查数据库与 Redis）")
    @GetMapping("/ready")
    public Result<Map<String, String>> ready() {
        Map<String, String> status = new HashMap<>();

        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                status.put("database", "UP");
            } else {
                status.put("database", "DOWN");
                status.put("status", "DOWN");
            }
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
            status.put("status", "DOWN");
        }

        // 检查 Redis 连接（如果启用）
        if (redisConnectionFactory != null) {
            try (var conn = redisConnectionFactory.getConnection()) {
                conn.ping();
                status.put("redis", "UP");
            } catch (Exception e) {
                status.put("redis", "DOWN: " + e.getMessage());
                // Redis 不可用不影响服务运行
            }
        } else {
            status.put("redis", "DISABLED");
        }

        if (!status.containsKey("status")) {
            status.put("status", "UP");
        }

        return Result.success(status);
    }
}