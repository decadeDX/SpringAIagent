package io.github.decadedx.springaiagent.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CitationValidatorTest {

    private final CitationValidator citationValidator = new CitationValidator();

    @Test
    void shouldAcceptCitationOnlyWhenItBelongsToRetrievedChunkAndQuotesSource() {
        KnowledgeVectorDocument chunk = new KnowledgeVectorDocument("record-1", "预约前必须完成安全培训。",
                Map.of("chunkId", "101-1", "title", "实验室指南", "version", "v1"));

        assertThat(citationValidator.validate(new RagModelResponse("需要先培训",
                        List.of(new RagModelCitation("101-1", "必须完成安全培训"))), Map.of("101-1", chunk)))
                .hasValueSatisfying(citations -> assertThat(citations).hasSize(1));
    }

    @Test
    void shouldRejectForgedChunkIdEvidenceAndIncompleteMetadata() {
        KnowledgeVectorDocument chunk = new KnowledgeVectorDocument("record-1", "预约前必须完成安全培训。",
                Map.of("chunkId", "101-1", "title", "实验室指南"));

        assertThat(citationValidator.validate(new RagModelResponse("需要先培训",
                        List.of(new RagModelCitation("101-2", "必须完成安全培训"))), Map.of("101-1", chunk)))
                .isEmpty();
        assertThat(citationValidator.validate(new RagModelResponse("需要先培训",
                        List.of(new RagModelCitation("101-1", "必须完成安全培训"))), Map.of("101-1", chunk)))
                .isEmpty();
    }
}
