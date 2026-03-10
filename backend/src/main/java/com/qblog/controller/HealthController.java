package com.qblog.controller;

import com.qblog.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
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
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 存活探针 - 检查服务是否运行
     */
    @GetMapping
    public Result<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        return Result.success(status);
    }

    /**
     * 就绪探针 - 检查服务是否准备好接收请求
     */
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

        // 检查 Redis 连接
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            conn.ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN: " + e.getMessage());
            // Redis 不可用不影响服务运行
        }

        if (!status.containsKey("status")) {
            status.put("status", "UP");
        }

        return Result.success(status);
    }
}