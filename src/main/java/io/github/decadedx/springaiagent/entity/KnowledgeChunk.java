package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 已切分的知识原文片段；向量本体位于 Redis Stack，MySQL 只保存可验证的原文与关联信息。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    /** 分块数据库主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属知识文档版本主键。 */
    @TableField("document_id")
    private Long documentId;

    /** 在所属版本内从 1 开始的稳定分块序号。 */
    @TableField("sequence_no")
    private Integer sequenceNo;

    /** 供引用校验使用的完整原文。 */
    private String content;

    /** 预留给后续摘要展示的可空摘要文本。 */
    private String summary;

    /** Redis Stack 中对应向量文档的稳定记录标识。 */
    @TableField("vector_record_id")
    private String vectorRecordId;

    /** 原文 SHA-256 摘要，用于追踪切分输入。 */
    @TableField("content_hash")
    private String contentHash;

    /** 分块创建时刻，数据库以 UTC 保存。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
