package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.config.KnowledgeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.CharacterCodingException;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFileStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreAndReadUtf8SourceUsingOnlyGeneratedRelativePath() throws Exception {
        KnowledgeFileStorage storage = storage();

        String relativePath = storage.store(123L, "md", "# 实验室规则".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(relativePath).isEqualTo("123/source.md");
        assertThat(storage.readUtf8(relativePath)).isEqualTo("# 实验室规则");
    }

    @Test
    void shouldRejectInvalidUtf8Bytes() {
        KnowledgeFileStorage storage = storage();

        assertThatThrownBy(() -> storage.decodeUtf8(new byte[]{(byte) 0x80}))
                .isInstanceOf(CharacterCodingException.class);
    }

    private KnowledgeFileStorage storage() {
        return new KnowledgeFileStorage(new KnowledgeProperties(false, tempDirectory, Duration.ofMinutes(10)));
    }
}
