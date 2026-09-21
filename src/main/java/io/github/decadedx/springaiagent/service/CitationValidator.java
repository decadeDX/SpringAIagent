package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.vo.KnowledgeCitationVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 将模型候选引用限制在本轮检索分块内，并验证证据片段确实来自其声明的原文。
 */
@Component
public class CitationValidator {

    /** 向量元数据中保存文档标题的键。 */
    private static final String TITLE_KEY = "title";

    /** 向量元数据中保存文档版本的键。 */
    private static final String VERSION_KEY = "version";

    /**
     * 验证模型回答至少包含一条有效引用，并转换为对外引用对象。
     *
     * @param response 模型结构化输出
     * @param retrievedChunks 本轮 TopK 分块，以 chunkId 为键
     * @return 所有引用均有效时返回转换结果，否则为空
     */
    public Optional<List<KnowledgeCitationVO>> validate(RagModelResponse response,
                                                        Map<String, KnowledgeVectorDocument> retrievedChunks) {
        if (response == null || !StringUtils.hasText(response.answer()) || response.citations() == null
                || response.citations().isEmpty()) {
            return Optional.empty();
        }
        Set<String> seenChunkIds = new LinkedHashSet<>();
        List<KnowledgeCitationVO> citations = response.citations().stream().map(candidate -> {
            if (candidate == null || !StringUtils.hasText(candidate.chunkId()) || !StringUtils.hasText(candidate.excerpt())
                    || !seenChunkIds.add(candidate.chunkId())) {
                return null;
            }
            KnowledgeVectorDocument chunk = retrievedChunks.get(candidate.chunkId());
            if (chunk == null || !chunk.content().contains(candidate.excerpt().trim())) {
                return null;
            }
            Object rawTitle = chunk.metadata().get(TITLE_KEY);
            Object rawVersion = chunk.metadata().get(VERSION_KEY);
            if (!(rawTitle instanceof String title) || !(rawVersion instanceof String version)
                    || !StringUtils.hasText(title) || !StringUtils.hasText(version)) {
                return null;
            }
            return new KnowledgeCitationVO(title, version, candidate.chunkId(), candidate.excerpt().trim());
        }).toList();
        return citations.stream().anyMatch(item -> item == null) ? Optional.empty() : Optional.of(citations);
    }

}
