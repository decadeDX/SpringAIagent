package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.KnowledgeProperties;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentUploadDTO;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.KnowledgeChunkMapper;
import io.github.decadedx.springaiagent.mapper.KnowledgeDocumentMapper;
import io.github.decadedx.springaiagent.mapper.LabMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.service.KnowledgeFileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class KnowledgeServiceImplTest {

    @TempDir
    Path tempDirectory;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectUnsupportedExtensionAndSourceLargerThanFiveMiB() {
        authenticateAdmin();
        KnowledgeServiceImpl service = service();

        assertThatThrownBy(() -> service.upload(upload(new MockMultipartFile("file", "rules.pdf", "application/pdf",
                        "content".getBytes()))))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo(ApiCode.UNSUPPORTED_MEDIA_TYPE));
        assertThatThrownBy(() -> service.upload(upload(new MockMultipartFile("file", "rules.txt", "text/plain",
                        new byte[5 * 1024 * 1024 + 1]))))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo(ApiCode.FILE_TOO_LARGE));
    }

    @Test
    void shouldRejectSourceThatIsNotStrictUtf8() {
        authenticateAdmin();
        KnowledgeServiceImpl service = service();

        assertThatThrownBy(() -> service.upload(upload(new MockMultipartFile("file", "rules.md", "text/markdown",
                        new byte[]{(byte) 0x80}))))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo(ApiCode.BAD_REQUEST));
    }

    private KnowledgeServiceImpl service() {
        return new KnowledgeServiceImpl(mock(KnowledgeDocumentMapper.class), mock(KnowledgeChunkMapper.class),
                mock(LabMapper.class), new KnowledgeFileStorage(new KnowledgeProperties(false, tempDirectory,
                Duration.ofMinutes(10))), mock(ApplicationEventPublisher.class),
                new ObjectMapper());
    }

    private KnowledgeDocumentUploadDTO upload(MockMultipartFile file) {
        return new KnowledgeDocumentUploadDTO(file, "LAB-RULES", "实验室规则", "v1",
                OffsetDateTime.parse("2026-09-20T09:00:00+08:00"), List.of());
    }

    private void authenticateAdmin() {
        AuthenticatedUser user = new AuthenticatedUser(3L, UserRole.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
