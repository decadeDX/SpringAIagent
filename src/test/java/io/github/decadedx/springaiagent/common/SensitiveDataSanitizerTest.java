package io.github.decadedx.springaiagent.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证日志和审计字段不会保留认证凭据或用户业务详情。
 */
class SensitiveDataSanitizerTest {

    /**
     * Token、密码和 API Key 必须替换为掩码。
     */
    @Test
    void masksCredentialsInDiagnosticText() {
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

        String value = sanitizer.sanitizeLogText(
                "password=student01 Authorization: Bearer eyJ-secret api-key=sk-secret");

        assertThat(value).doesNotContain("student01", "eyJ", "sk-secret");
        assertThat(value).contains("***");
    }

    /**
     * Agent 审计不得保存完整预约或报修参数。
     */
    @Test
    void removesPersonalBusinessDetailsFromAuditText() {
        String value = new SensitiveDataSanitizer().sanitizeAuditText(
                "LAB-B402 预约 2026-09-22 14:00；设备 GPU-01 冒烟");

        assertThat(value).isEqualTo("[已脱敏]");
    }
}
