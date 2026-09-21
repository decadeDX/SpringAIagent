package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 一份可追溯知识文档的具体版本；正文分块与向量记录均归属该版本。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("knowledge_document")
public class KnowledgeDocument {

    /** 文档版本数据库主键，同时用于生成稳定分块标识。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 跨版本保持不变的逻辑文档编号。 */
    @TableField("logical_document_code")
    private String logicalDocumentCode;

    /** 同一逻辑文档下由管理员维护的版本文本。 */
    private String version;

    /** 对外展示和引用使用的文档标题。 */
    private String title;

    /** 文档版本生效时刻，数据库以 UTC 保存。 */
    @TableField("effective_at")
    private LocalDateTime effectiveAt;

    /** 适用实验室编号数组的 JSON 文本，服务层负责序列化与反序列化。 */
    @TableField("applicable_lab_ids")
    private String applicableLabIds;

    /** 决定该版本是否能够参与检索的发布状态。 */
    @TableField("publish_status")
    private KnowledgePublishStatus publishStatus;

    /** 文档版本当前的异步索引状态。 */
    @TableField("index_status")
    private KnowledgeIndexStatus indexStatus;

    /** 相对于知识目录的安全源文件路径，不接受客户端直接指定。 */
    @TableField("source_file_path")
    private String sourceFilePath;

    /** 最近一次索引失败的简短原因；成功时必须为空。 */
    @TableField("index_failure_reason")
    private String indexFailureReason;

    /** 上传该文档版本的管理员主键。 */
    @TableField("created_by")
    private Long createdBy;

    /** 文档版本记录创建时刻，数据库以 UTC 保存。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 文档版本记录最近更新时刻，数据库以 UTC 保存。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 列表查询填充的分块数量，不参与数据库持久化。 */
    @TableField(exist = false)
    private Integer chunkCount;
}
