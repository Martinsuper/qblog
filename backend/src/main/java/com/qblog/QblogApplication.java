package com.qblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Q 博客系统启动类
 */
@SpringBootApplication
@MapperScan("com.qblog.mapper")
public class QblogApplication {

    public static void main(String[] args) {
        SpringApplication.run(QblogApplication.class, args);
        System.out.println("====================================");
        System.out.println("    Q 博客系统启动成功！");
        System.out.println("    API 文档：http://localhost:8080/doc.html");
        System.out.println("====================================");
    }
}
