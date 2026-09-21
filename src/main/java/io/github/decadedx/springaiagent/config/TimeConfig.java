package io.github.decadedx.springaiagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 提供可替换的业务时钟，保证预约规则统一按 Asia/Shanghai 判断且测试可固定当前时刻。
 */
@Configuration
public class TimeConfig {

    /** 预约规则使用的业务时区。 */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 创建生产环境系统时钟。
     *
     * @return Asia/Shanghai 时区的系统时钟
     */
    @Bean
    public Clock businessClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
