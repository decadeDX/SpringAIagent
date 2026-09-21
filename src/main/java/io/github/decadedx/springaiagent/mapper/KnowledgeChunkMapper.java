package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识分块的持久化入口；向量本体由独立 Redis Stack 存储。
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * 批量保存单一文档版本切分出的全部分块。
     *
     * @param chunks 待写入分块
     * @return 写入行数
     */
    int insertBatch(@Param("chunks") List<KnowledgeChunk> chunks);

    /**
     * 按稳定序号读取一个文档版本的全部分块。
     *
     * @param documentId 文档版本主键
     * @return 有序分块列表
     */
    List<KnowledgeChunk> selectByDocumentId(@Param("documentId") Long documentId);

    /**
     * 统计指定文档版本已经持久化的分块数量。
     *
     * @param documentId 文档版本主键
     * @return 分块数量
     */
    int countByDocumentId(@Param("documentId") Long documentId);

    /**
     * 删除文档版本的已持久化分块，供索引失败补偿和重试清理使用。
     *
     * @param documentId 文档版本主键
     * @return 删除行数
     */
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
