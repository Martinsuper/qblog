package com.qblog.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(RedisConfig.class);

    @Test void disabledDoesNotCreateConnection() {
        runner.withPropertyValues("spring.redis.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(LettuceConnectionFactory.class);
            assertThat(context).doesNotHaveBean(StringRedisTemplate.class);
        });
    }

    @Test void bootThreeConnectionPropertiesAreApplied() {
        runner.withPropertyValues("spring.redis.enabled=true", "spring.data.redis.host=redis.internal",
                "spring.data.redis.port=6380", "spring.data.redis.database=2",
                "spring.data.redis.password=test-password", "spring.data.redis.timeout=2s",
                "spring.data.redis.connect-timeout=1s").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(StringRedisTemplate.class);
            LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
            assertThat(factory.getHostName()).isEqualTo("redis.internal");
            assertThat(factory.getPort()).isEqualTo(6380);
            assertThat(factory.getDatabase()).isEqualTo(2);
            assertThat(factory.getStandaloneConfiguration().getPassword().isPresent()).isTrue();
            assertThat(factory.getClientConfiguration().getCommandTimeout()).isEqualTo(Duration.ofSeconds(2));
        });
    }
}
