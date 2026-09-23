package io.github.decadedx.springaiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 定义知识文档源文件、RAG 基础设施开关与公开问答缓存的运行参数。
 *
 * @param ragEnabled 只有 Redis Stack、聊天模型与嵌入模型均完成配置时才启用真实 RAG 适配器
 * @param storagePath 上传原文件的本地根目录
 * @param cacheTtl 不含个人数据的问答缓存有效期
 */
@ConfigurationProperties(prefix = "knowledge")
public record KnowledgeProperties(boolean ragEnabled, Path storagePath, Duration cacheTtl) {
}
