package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.config.SecurityConfig;
import io.github.decadedx.springaiagent.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void shouldIssueAndParseTrustedIdentity() {
        JwtTokenService service = createTokenService(Duration.ofHours(2));

        IssuedJwtToken token = service.issue(3L, UserRole.ADMIN, "session-3");
        AuthenticatedUser user = service.parse(token.value());

        assertEquals(3L, user.id());
        assertEquals(UserRole.ADMIN, user.role());
        assertEquals("session-3", user.sessionId());
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtProperties properties = createProperties(Duration.ofHours(2));
        SecurityConfig securityConfig = new SecurityConfig();
        JwtTokenService service = new JwtTokenService(
                securityConfig.jwtEncoder(properties), securityConfig.jwtDecoder(properties), properties);
        Instant now = Instant.now();
        String token = securityConfig.jwtEncoder(properties).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer(properties.getIssuer())
                        .subject("1")
                        .issuedAt(now.minus(Duration.ofHours(2)))
                        .expiresAt(now.minus(Duration.ofHours(1)))
                        .claim("role", UserRole.STUDENT.name())
                        .claim("sid", "expired-session")
                        .build())).getTokenValue();

        assertThrows(JwtException.class, () -> service.parse(token));
    }

    private JwtTokenService createTokenService(Duration ttl) {
        JwtProperties properties = createProperties(ttl);
        SecurityConfig securityConfig = new SecurityConfig();
        return new JwtTokenService(securityConfig.jwtEncoder(properties), securityConfig.jwtDecoder(properties), properties);
    }

    private JwtProperties createProperties(Duration ttl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        properties.setIssuer("lab-assistant");
        properties.setTtl(ttl);
        return properties;
    }
}
