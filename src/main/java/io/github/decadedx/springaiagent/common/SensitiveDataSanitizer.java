package io.github.decadedx.springaiagent.common;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 将可能进入日志或审计文本的密码、Token、API Key 与业务详情替换为固定掩码。
 */
@Component
public class SensitiveDataSanitizer {

    /** Bearer 凭据的通用文本形式。 */
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)Bearer\\s+[^\\s,}]+");

    /** 常见密钥字段的 JSON 或键值文本形式。 */
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)(password|authorization|api[-_]?key|access[_-]?token)\\s*([:=])\\s*(\\\"[^\\\"]*\\\"|[^,\\s}]+)");

    /**
     * 脱敏普通诊断文本中的认证凭据；调用方不得将完整业务请求体传入日志。
     *
     * @param value 原始诊断文本
     * @return 不含凭据的文本
     */
    public String sanitizeLogText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return SECRET_FIELD.matcher(BEARER_TOKEN.matcher(value).replaceAll("Bearer ***"))
                .replaceAll("$1$2***");
    }

    /**
     * 审计中不保留模型或用户提供的参数详情，仅保留调用发生这一事实。
     *
     * @param value 原始参数或摘要
     * @return 固定脱敏文本
     */
    public String sanitizeAuditText(String value) {
        return StringUtils.hasText(value) ? "[已脱敏]" : value;
    }
}
