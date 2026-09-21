package io.github.decadedx.springaiagent.enums;

/**
 * 知识文档版本对检索的发布状态；只有 PUBLISHED 版本能参与问答。
 */
public enum KnowledgePublishStatus {
    /** 已上传或已索引但尚未对用户生效的版本。 */
    DRAFT,
    /** 当前对用户生效的版本。 */
    PUBLISHED,
    /** 已停止参与新检索的历史版本。 */
    DISABLED
}
