package com.qblog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/Knife4j 配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Q 博客系统 API 文档")
                        .description("基于 Spring Boot 和 Vue.js 3.0 的博客系统")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Q 博客")
                                .email("admin@qblog.com")));
    }
}
