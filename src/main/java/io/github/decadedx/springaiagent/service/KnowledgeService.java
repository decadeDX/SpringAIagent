package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.KnowledgeDocumentQueryDTO;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentUploadDTO;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentPageVO;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentVO;

/**
 * 定义知识文档上传、版本查询和发布状态流转的领域边界。
 */
public interface KnowledgeService {

    /**
     * 保存管理员上传的源文件和待索引版本记录。
     *
     * @param uploadDTO multipart 上传元数据与文件
     * @return 已创建且处于 PENDING 状态的文档版本
     */
    KnowledgeDocumentVO upload(KnowledgeDocumentUploadDTO uploadDTO);

    /**
     * 分页查询管理员可见的知识文档版本。
     *
     * @param queryDTO 文档编号、状态和分页条件
     * @return 文档版本分页结果
     */
    KnowledgeDocumentPageVO findPage(KnowledgeDocumentQueryDTO queryDTO);

    /**
     * 发布已完成索引的文档版本，并停用同一逻辑文档的旧发布版本。
     *
     * @param documentId 待发布文档版本主键
     * @return 发布后的文档版本
     */
    KnowledgeDocumentVO publish(Long documentId);

    /**
     * 停用当前已发布文档版本。
     *
     * @param documentId 待停用文档版本主键
     * @return 停用后的文档版本
     */
    KnowledgeDocumentVO disable(Long documentId);
}
