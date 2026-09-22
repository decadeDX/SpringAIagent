package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.config.TimeConfig;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 在服务端将有限中文相对日期转为上海日期，避免由模型自行猜测日历。
 */
@Component
public class RelativeDateResolver {

    /** 可替换业务时钟。 */
    private final Clock clock;

    /** 下周中文星期到 ISO 星期的映射。 */
    private static final Map<String, DayOfWeek> NEXT_WEEK_DAYS = Map.of(
            "下周一", DayOfWeek.MONDAY, "下周二", DayOfWeek.TUESDAY, "下周三", DayOfWeek.WEDNESDAY,
            "下周四", DayOfWeek.THURSDAY, "下周五", DayOfWeek.FRIDAY, "下周六", DayOfWeek.SATURDAY,
            "下周日", DayOfWeek.SUNDAY, "下周天", DayOfWeek.SUNDAY);

    /**
     * 创建相对日期解析器。
     *
     * @param clock 业务时钟
     */
    public RelativeDateResolver(Clock clock) {
        this.clock = clock;
    }

    /**
     * 找出消息中支持的相对日期及其绝对日期。
     *
     * @param message 用户原始消息
     * @return 可展示给模型和用户的解析结果
     */
    public List<String> resolve(String message) {
        LocalDate today = LocalDate.now(clock.withZone(TimeConfig.BUSINESS_ZONE));
        List<String> resolved = new ArrayList<>();
        if (message.contains("明天")) {
            resolved.add("明天=" + today.plusDays(1));
        }
        if (message.contains("后天")) {
            resolved.add("后天=" + today.plusDays(2));
        }
        LocalDate nextMonday = today.plusDays(8L - today.getDayOfWeek().getValue());
        NEXT_WEEK_DAYS.forEach((text, day) -> {
            if (message.contains(text)) {
                resolved.add(text + "=" + nextMonday.plusDays(day.getValue() - 1L));
            }
        });
        return List.copyOf(resolved);
    }
}
