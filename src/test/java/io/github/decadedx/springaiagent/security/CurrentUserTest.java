package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReadIdentityFromAuthenticatedPrincipal() {
        AuthenticatedUser user = new AuthenticatedUser(2L, UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        assertEquals(2L, CurrentUser.requireId());
        assertEquals(UserRole.STUDENT, CurrentUser.requireRole());
    }

    @Test
    void shouldRejectMissingAuthentication() {
        BusinessException exception = assertThrows(BusinessException.class, CurrentUser::requireId);

        assertEquals("UNAUTHENTICATED", exception.getCode());
    }
}
