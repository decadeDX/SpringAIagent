package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.decadedx.springaiagent.entity.KnowledgeChunk;
import io.github.decadedx.springaiagent.entity.KnowledgeDocument;
import io.github.decadedx.springaiagent.mapper.KnowledgeChunkMapper;
import io.github.decadedx.springaiagent.mapper.KnowledgeDocumentMapper;
import io.github.decadedx.springaiagent.service.KnowledgeFileStorage;
import io.github.decadedx.springaiagent.service.KnowledgeIndexService;
import io.github.decadedx.springaiagent.service.KnowledgeIndexRequestedEvent;
import io.github.decadedx.springaiagent.service.KnowledgeVectorDocument;
import io.github.decadedx.springaiagent.service.KnowledgeVectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 负责将已提交源文件确定性切分、持久化分块并写入 Redis Stack 向量索引。
 */
@Service
public class KnowledgeIndexServiceImpl implements KnowledgeIndexService {

    /** 单块正文最大字符数。 */
    private static final int CHUNK_SIZE = 700;

    /** 相邻分块间保留的字符重叠数。 */
    private static final int CHUNK_OVERLAP = 100;

    /** 为保留自然段边界而允许提前结束的最小分块长度。 */
    private static final int MIN_BOUNDARY_CHUNK_SIZE = 600;

    /** 文档版本元数据访问入口。 */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 分块元数据访问入口。 */
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /** 受控源文件读取服务。 */
    private final KnowledgeFileStorage knowledgeFileStorage;

    /** Redis Stack 向量读写适配器。 */
    private final KnowledgeVectorStore knowledgeVectorStore;

    /** 重放待索引任务的事件发布器。 */
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建知识索引服务。
     *
     * @param knowledgeDocumentMapper 文档版本 Mapper
     * @param knowledgeChunkMapper 分块 Mapper
     * @param knowledgeFileStorage 源文件存储服务
     * @param knowledgeVectorStore 向量存储适配器
     * @param applicationEventPublisher 待索引任务事件发布器
     */
    public KnowledgeIndexServiceImpl(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                     KnowledgeChunkMapper knowledgeChunkMapper,
                                     KnowledgeFileStorage knowledgeFileStorage,
                                     KnowledgeVectorStore knowledgeVectorStore,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeFileStorage = knowledgeFileStorage;
        this.knowledgeVectorStore = knowledgeVectorStore;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async("knowledgeIndexExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void schedule(Long documentId) {
        if (documentId == null || knowledgeDocumentMapper.claimPendingForIndexing(documentId) != 1) {
            return;
        }
        List<String> vectorRecordIds = new ArrayList<>();
        try {
            KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
            if (document == null) {
                return;
            }
            clearPreviousIndex(documentId, vectorRecordIds);
            List<KnowledgeChunk> chunks = createChunks(document, knowledgeFileStorage.readUtf8(document.getSourceFilePath()));
            knowledgeChunkMapper.insertBatch(chunks);
            vectorRecordIds.addAll(chunks.stream().map(KnowledgeChunk::getVectorRecordId).toList());
            knowledgeVectorStore.add(toVectorDocuments(document, chunks));
            knowledgeDocumentMapper.markIndexSucceeded(documentId);
        } catch (Exception exception) {
            cleanupFailedIndex(documentId, vectorRecordIds, exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recoverPending() {
        knowledgeDocumentMapper.resetIndexingToPending();
        knowledgeDocumentMapper.selectPendingIds().forEach(documentId ->
                applicationEventPublisher.publishEvent(new KnowledgeIndexRequestedEvent(documentId)));
    }

    /**
     * 删除上次失败或被中断任务留下的分块和向量记录，确保重试不会产生重复索引。
     *
     * @param documentId 文档版本主键
     * @param vectorRecordIds 本轮清理后仍需在失败时补偿的向量记录集合
     */
    private void clearPreviousIndex(Long documentId, List<String> vectorRecordIds) {
        List<KnowledgeChunk> existingChunks = knowledgeChunkMapper.selectByDocumentId(documentId);
        if (!existingChunks.isEmpty()) {
            List<String> existingIds = existingChunks.stream().map(KnowledgeChunk::getVectorRecordId).toList();
            knowledgeVectorStore.delete(existingIds);
            knowledgeChunkMapper.deleteByDocumentId(documentId);
        }
        vectorRecordIds.clear();
    }

    /**
     * 按固定长度、重叠量和自然段边界生成确定性的知识分块。
     *
     * @param document 所属知识文档版本
     * @param source 原始 UTF-8 文本
     * @return 待持久化分块
     */
    List<KnowledgeChunk> createChunks(KnowledgeDocument document, String source) {
        String content = normalizeSource(source);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int start = 0;
        int sequenceNo = 1;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            if (end < content.length()) {
                int boundary = lastBoundary(content, start + MIN_BOUNDARY_CHUNK_SIZE, end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            String chunkContent = content.substring(start, end).trim();
            if (!chunkContent.isEmpty()) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setId(IdWorker.getId());
                chunk.setDocumentId(document.getId());
                chunk.setSequenceNo(sequenceNo);
                chunk.setContent(chunkContent);
                chunk.setSummary(chunkContent.substring(0, Math.min(chunkContent.length(), 160)));
                chunk.setVectorRecordId("knowledge-" + document.getId() + "-" + sequenceNo);
                chunk.setContentHash(sha256(chunkContent));
                chunks.add(chunk);
                sequenceNo++;
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("知识源文件不包含可索引正文");
        }
        return chunks;
    }

    /**
     * 将持久化分块转换为 Redis Stack 向量记录，并写入版本过滤与引用展示元数据。
     *
     * @param document 所属文档版本
     * @param chunks 已切分正文
     * @return 向量存储写入载荷
     */
    private List<KnowledgeVectorDocument> toVectorDocuments(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        return chunks.stream().map(chunk -> new KnowledgeVectorDocument(chunk.getVectorRecordId(), chunk.getContent(),
                Map.of("documentId", String.valueOf(document.getId()),
                        "chunkId", document.getId() + "-" + chunk.getSequenceNo(),
                        "logicalDocumentCode", document.getLogicalDocumentCode(),
                        "title", document.getTitle(), "version", document.getVersion())))
                .toList();
    }

    /**
     * 收敛索引异常：清理已写向量和分块，并将文档版本标记为 FAILED。
     *
     * @param documentId 文档版本主键
     * @param vectorRecordIds 本轮可能写入的向量记录标识
     * @param exception 原始异常
     */
    private void cleanupFailedIndex(Long documentId, List<String> vectorRecordIds, Exception exception) {
        try {
            if (!vectorRecordIds.isEmpty()) {
                knowledgeVectorStore.delete(vectorRecordIds);
            }
        } catch (Exception ignored) {
            // FAILED 状态不会进入检索；下次重试会再次清理残留记录。
        }
        knowledgeChunkMapper.deleteByDocumentId(documentId);
        knowledgeDocumentMapper.markIndexFailed(documentId, failureReason(exception));
    }

    /**
     * 归一化换行和控制字符，保留正文语义但消除分块时的系统差异。
     *
     * @param source 原始源文件文本
     * @return 可稳定切分的正文
     */
    private String normalizeSource(String source) {
        return source.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "").trim();
    }

    /**
     * 在允许区间内寻找最近的段落或句末边界。
     *
     * @param content 完整文本
     * @param minimum 最小可接受边界位置
     * @param maximum 理想结束位置
     * @return 边界后一位位置；未找到时返回 -1
     */
    private int lastBoundary(String content, int minimum, int maximum) {
        for (int index = maximum - 1; index >= minimum; index--) {
            char value = content.charAt(index);
            if (value == '\n' || value == '。' || value == '！' || value == '？') {
                return index + 1;
            }
        }
        return -1;
    }

    /**
     * 计算分块原文的 SHA-256 十六进制摘要。
     *
     * @param content 分块原文
     * @return 小写十六进制摘要
     */
    private String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    /**
     * 截断基础设施失败细节，避免数据库保存凭据或冗长堆栈。
     *
     * @param exception 原始异常
     * @return 最多 1000 字符的失败摘要
     */
    private String failureReason(Exception exception) {
        String message = exception.getMessage();
        String reason = exception.getClass().getSimpleName() + (message == null ? "" : ": " + message);
        return reason.substring(0, Math.min(reason.length(), 1000));
    }
}
