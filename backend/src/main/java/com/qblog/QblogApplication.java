package com.qblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Q 博客系统启动类
 * Redis 为可选依赖，通过 spring.redis.enabled 配置控制
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class  // 排除 Redis 自动配置，通过 RedisConfig 手动控制
})
@MapperScan("com.qblog.mapper")
@EnableScheduling
public class QblogApplication {

    public static void main(String[] args) {
        SpringApplication.run(QblogApplication.class, args);
        System.out.println("====================================");
        System.out.println("    Q 博客系统启动成功！");
        System.out.println("    API 文档：http://localhost:8081/doc.html");
        System.out.println("====================================");
    }
}
