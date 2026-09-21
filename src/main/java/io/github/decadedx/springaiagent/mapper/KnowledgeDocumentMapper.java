package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.KnowledgeDocument;
import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识文档版本的持久化入口；版本状态的合法性由服务层统一判断。
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /**
     * 按管理员筛选条件分页查询文档版本，并填充分块数量。
     *
     * @param logicalDocumentCode 可选逻辑文档编号
     * @param publishStatus 可选发布状态
     * @param indexStatus 可选索引状态
     * @param offset 从零开始的分页偏移
     * @param size 本页数量
     * @return 文档版本列表
     */
    List<KnowledgeDocument> selectPage(@Param("logicalDocumentCode") String logicalDocumentCode,
                                       @Param("publishStatus") KnowledgePublishStatus publishStatus,
                                       @Param("indexStatus") KnowledgeIndexStatus indexStatus,
                                       @Param("offset") long offset, @Param("size") int size);

    /**
     * 统计符合管理员筛选条件的文档版本数。
     *
     * @param logicalDocumentCode 可选逻辑文档编号
     * @param publishStatus 可选发布状态
     * @param indexStatus 可选索引状态
     * @return 版本总数
     */
    long countPage(@Param("logicalDocumentCode") String logicalDocumentCode,
                   @Param("publishStatus") KnowledgePublishStatus publishStatus,
                   @Param("indexStatus") KnowledgeIndexStatus indexStatus);

    /**
     * 锁定指定文档版本，供发布、停用和后台索引状态转换使用。
     *
     * @param documentId 文档版本主键
     * @return 被锁定文档版本；不存在时为空
     */
    KnowledgeDocument selectByIdForUpdate(@Param("documentId") Long documentId);

    /**
     * 锁定同一逻辑文档的所有版本，保证发布新版本时最多保留一个已发布版本。
     *
     * @param logicalDocumentCode 逻辑文档编号
     * @return 被锁定的全部版本
     */
    List<KnowledgeDocument> selectByLogicalCodeForUpdate(@Param("logicalDocumentCode") String logicalDocumentCode);

    /**
     * 原子领取待处理索引任务，避免上传事件和启动恢复重复处理同一版本。
     *
     * @param documentId 文档版本主键
     * @return 领取成功时为 1
     */
    int claimPendingForIndexing(@Param("documentId") Long documentId);

    /**
     * 标记索引成功并清除上次失败原因。
     *
     * @param documentId 文档版本主键
     * @return 更新行数
     */
    int markIndexSucceeded(@Param("documentId") Long documentId);

    /**
     * 标记索引失败并记录经过截断的失败原因。
     *
     * @param documentId 文档版本主键
     * @param reason 面向管理员展示的失败摘要
     * @return 更新行数
     */
    int markIndexFailed(@Param("documentId") Long documentId, @Param("reason") String reason);

    /**
     * 在应用启动时将中断的索引任务恢复为待处理状态。
     *
     * @return 被恢复的任务数量
     */
    int resetIndexingToPending();

    /**
     * 查询等待后台索引的文档版本主键。
     *
     * @return 所有待索引文档版本主键
     */
    List<Long> selectPendingIds();

    /**
     * 查询所有当前可被向量检索使用的文档版本主键。
     *
     * @return 已发布且索引成功的文档版本主键
     */
    List<Long> selectPublishedSucceededIds();

    /**
     * 将同一逻辑文档下已发布的旧版本停用。
     *
     * @param logicalDocumentCode 逻辑文档编号
     * @param excludingId 即将发布的新版本主键
     * @return 被停用的旧版本数量
     */
    int disablePublishedByLogicalCode(@Param("logicalDocumentCode") String logicalDocumentCode,
                                      @Param("excludingId") Long excludingId);

    /**
     * 将指定版本发布；调用方必须先完成版本行锁与索引状态校验。
     *
     * @param documentId 文档版本主键
     * @return 更新行数
     */
    int publish(@Param("documentId") Long documentId);

    /**
     * 停用指定已发布版本。
     *
     * @param documentId 文档版本主键
     * @return 更新行数
     */
    int disable(@Param("documentId") Long documentId);
}
