package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.entity.KnowledgeChunk;
import io.github.decadedx.springaiagent.entity.KnowledgeDocument;
import io.github.decadedx.springaiagent.mapper.KnowledgeChunkMapper;
import io.github.decadedx.springaiagent.mapper.KnowledgeDocumentMapper;
import io.github.decadedx.springaiagent.service.KnowledgeFileStorage;
import io.github.decadedx.springaiagent.service.KnowledgeIndexRequestedEvent;
import io.github.decadedx.springaiagent.service.KnowledgeVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIndexServiceImplTest {

    @Test
    void shouldSplitOnParagraphBoundaryWithStableSequenceAndOverlap() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(9001L);
        String source = "甲".repeat(605) + "。\n" + "乙".repeat(700) + "。\n" + "丙".repeat(120);
        KnowledgeIndexServiceImpl service = new KnowledgeIndexServiceImpl(null, null, null, null, null);

        List<KnowledgeChunk> chunks = service.createChunks(document, source);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(KnowledgeChunk::getSequenceNo).containsExactly(1, 2, 3);
        assertThat(chunks).extracting(KnowledgeChunk::getVectorRecordId)
                .containsExactly("knowledge-9001-1", "knowledge-9001-2", "knowledge-9001-3");
        assertThat(chunks.get(0).getContent()).endsWith("。");
        assertThat(chunks.get(1).getContent()).contains("乙");
    }

    @Test
    void shouldPersistChunksAndMarkSucceededAfterVectorWrite() throws Exception {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeFileStorage fileStorage = mock(KnowledgeFileStorage.class);
        KnowledgeVectorStore vectorStore = mock(KnowledgeVectorStore.class);
        KnowledgeDocument document = document(9010L);
        when(documentMapper.claimPendingForIndexing(9010L)).thenReturn(1);
        when(documentMapper.selectById(9010L)).thenReturn(document);
        when(chunkMapper.selectByDocumentId(9010L)).thenReturn(List.of());
        when(fileStorage.readUtf8(anyString())).thenReturn("预约前必须完成安全培训。".repeat(50));
        KnowledgeIndexServiceImpl service = new KnowledgeIndexServiceImpl(documentMapper, chunkMapper, fileStorage,
                vectorStore, mock(ApplicationEventPublisher.class));

        service.schedule(9010L);

        verify(chunkMapper).insertBatch(any());
        verify(vectorStore).add(any());
        verify(documentMapper).markIndexSucceeded(9010L);
    }

    @Test
    void shouldCompensateChunksAndMarkFailedWhenVectorWriteFails() throws Exception {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeFileStorage fileStorage = mock(KnowledgeFileStorage.class);
        KnowledgeVectorStore vectorStore = mock(KnowledgeVectorStore.class);
        KnowledgeDocument document = document(9020L);
        when(documentMapper.claimPendingForIndexing(9020L)).thenReturn(1);
        when(documentMapper.selectById(9020L)).thenReturn(document);
        when(chunkMapper.selectByDocumentId(9020L)).thenReturn(List.of());
        when(fileStorage.readUtf8(anyString())).thenReturn("正文".repeat(400));
        org.mockito.Mockito.doThrow(new IllegalStateException("Redis Stack unavailable")).when(vectorStore).add(any());
        KnowledgeIndexServiceImpl service = new KnowledgeIndexServiceImpl(documentMapper, chunkMapper, fileStorage,
                vectorStore, mock(ApplicationEventPublisher.class));

        service.schedule(9020L);

        verify(chunkMapper).deleteByDocumentId(9020L);
        verify(documentMapper).markIndexFailed(anyLong(), anyString());
    }

    @Test
    void shouldResetInterruptedTasksAndRepublishEveryPendingDocumentAtStartup() {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(documentMapper.selectPendingIds()).thenReturn(List.of(10L, 11L));
        KnowledgeIndexServiceImpl service = new KnowledgeIndexServiceImpl(documentMapper,
                mock(KnowledgeChunkMapper.class), mock(KnowledgeFileStorage.class), mock(KnowledgeVectorStore.class),
                eventPublisher);

        service.recoverPending();

        verify(documentMapper).resetIndexingToPending();
        verify(eventPublisher).publishEvent(new KnowledgeIndexRequestedEvent(10L));
        verify(eventPublisher).publishEvent(new KnowledgeIndexRequestedEvent(11L));
    }

    private KnowledgeDocument document(Long documentId) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(documentId);
        document.setLogicalDocumentCode("SAFETY-GUIDE");
        document.setTitle("安全指南");
        document.setVersion("v1");
        document.setSourceFilePath(documentId + "/source.md");
        return document;
    }
}
