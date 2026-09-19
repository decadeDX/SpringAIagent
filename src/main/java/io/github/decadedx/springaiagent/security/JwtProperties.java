package io.github.decadedx.springaiagent.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT 签名与有效期配置；密钥只能从环境变量注入，缺失时应用必须拒绝启动。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** Base64 编码的 HS256 对称签名密钥，解码后至少为 32 字节。 */
    @NotBlank(message = "JWT_SECRET 不能为空")
    private String secret;

    /** 用于隔离本系统令牌的 JWT issuer。 */
    @NotBlank(message = "JWT issuer 不能为空")
    private String issuer;

    /** 访问令牌有效期，当前固定配置为两小时。 */
    @NotNull(message = "JWT 有效期不能为空")
    private Duration ttl;
}
