package io.github.decadedx.springaiagent.config;

import io.github.decadedx.springaiagent.security.ApiAccessDeniedHandler;
import io.github.decadedx.springaiagent.security.ApiAuthenticationEntryPoint;
import io.github.decadedx.springaiagent.security.JwtAuthenticationFilter;
import io.github.decadedx.springaiagent.security.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * 配置无状态 JWT 认证、角色授权、密码摘要和前端跨域访问规则。
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, SecurityConfig.CorsProperties.class})
public class SecurityConfig {

    /**
     * 创建 BCrypt 密码编码器，供第二阶段的登录服务比对数据库摘要。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建只接受 HS256 的 JWT 编码器。
     *
     * @param jwtProperties JWT 配置
     * @return JWT 编码器
     */
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return NimbusJwtEncoder.withSecretKey(signingKey(jwtProperties))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * 创建校验 HS256 签名、issuer 和标准到期时间的 JWT 解码器。
     *
     * @param jwtProperties JWT 配置
     * @return JWT 解码器
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer()));
        return decoder;
    }

    /**
     * 配置 REST API 的认证边界，所有业务 API 均不创建 HTTP Session。
     *
     * @param http Spring Security HTTP 配置器
     * @param jwtAuthenticationFilter Bearer Token 认证过滤器
     * @param authenticationEntryPoint 401 响应处理器
     * @param accessDeniedHandler 403 响应处理器
     * @return 安全过滤器链
     * @throws Exception Spring Security 配置失败
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ApiAuthenticationEntryPoint authenticationEntryPoint,
                                                   ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 仅向已配置的前端来源开放 Bearer 请求头与请求追踪头。
     *
     * @param corsProperties 跨域来源配置
     * @return CORS 配置来源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 将 Base64 密钥转换为 HMAC SHA-256 所需的对称密钥，并拒绝弱密钥。
     *
     * @param jwtProperties JWT 配置
     * @return HS256 对称签名密钥
     */
    private SecretKey signingKey(JwtProperties jwtProperties) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET 解码后至少需要32字节");
            }
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET 必须是Base64编码", exception);
        }
    }

    /**
     * 将逗号分隔的跨域来源环境变量绑定为不可变来源列表。
     *
     * @param allowedOrigins 允许访问 API 的前端来源
     */
    @ConfigurationProperties(prefix = "security.cors")
    public record CorsProperties(List<String> allowedOrigins) {
    }
}
