package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理员上传知识文档时的 multipart 元数据；文件内容和身份均不由客户端决定业务状态。
 *
 * @param file UTF-8 Markdown 或 TXT 源文件
 * @param logicalDocumentCode 跨版本稳定的逻辑文档编号
 * @param title 文档展示标题
 * @param version 本次上传的版本文本
 * @param effectiveAt 文档版本生效时刻
 * @param applicableLabIds 可选适用实验室编号集合
 */
public record KnowledgeDocumentUploadDTO(
        MultipartFile file,
        @NotBlank(message = "逻辑文档编号不能为空") @Size(max = 64, message = "逻辑文档编号不能超过64个字符")
        String logicalDocumentCode,
        @NotBlank(message = "文档标题不能为空") @Size(max = 255, message = "文档标题不能超过255个字符")
        String title,
        @NotBlank(message = "文档版本不能为空") @Size(max = 64, message = "文档版本不能超过64个字符")
        String version,
        @NotNull(message = "生效时间不能为空") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime effectiveAt,
        List<@Size(max = 32, message = "实验室编号不能超过32个字符") String> applicableLabIds
) {
}
