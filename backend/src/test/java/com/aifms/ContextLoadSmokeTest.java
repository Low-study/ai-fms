package com.aifms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文加载冒烟测试。
 * 验证 Spring Boot 3.5.x 升级后应用上下文能正常启动。
 */
@SpringBootTest
@ActiveProfiles("test")
class ContextLoadSmokeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }
}
