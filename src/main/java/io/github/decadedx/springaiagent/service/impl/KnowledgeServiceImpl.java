package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentQueryDTO;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentUploadDTO;
import io.github.decadedx.springaiagent.entity.KnowledgeDocument;
import io.github.decadedx.springaiagent.entity.Lab;
import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.KnowledgeChunkMapper;
import io.github.decadedx.springaiagent.mapper.KnowledgeDocumentMapper;
import io.github.decadedx.springaiagent.mapper.LabMapper;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.KnowledgeFileStorage;
import io.github.decadedx.springaiagent.service.KnowledgeIndexRequestedEvent;
import io.github.decadedx.springaiagent.service.KnowledgePublishedVersionChangedEvent;
import io.github.decadedx.springaiagent.service.KnowledgeService;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentPageVO;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 实现知识文档上传、版本查询与发布流转；向量索引由事务提交后的异步任务完成。
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 数据库时间字段使用的 UTC 偏移。 */
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    /** 单个知识源文件允许的最大字节数，等于 5 MiB。 */
    private static final long MAX_SOURCE_FILE_SIZE = 5L * 1024 * 1024;

    /** 文档版本元数据访问入口。 */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 分块数量访问入口。 */
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /** 实验室资料访问入口，用于校验适用实验室编号。 */
    private final LabMapper labMapper;

    /** 受控本地源文件存储服务。 */
    private final KnowledgeFileStorage knowledgeFileStorage;

    /** 用于在事务成功提交后发布异步索引请求。 */
    private final ApplicationEventPublisher applicationEventPublisher;

    /** 统一 JSON 序列化器，用于适用实验室数组。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建知识文档领域服务。
     *
     * @param knowledgeDocumentMapper 文档版本 Mapper
     * @param knowledgeChunkMapper 分块 Mapper
     * @param labMapper 实验室 Mapper
     * @param knowledgeFileStorage 源文件存储服务
     * @param applicationEventPublisher 事务事件发布器
     * @param objectMapper JSON 序列化器
     */
    public KnowledgeServiceImpl(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                KnowledgeChunkMapper knowledgeChunkMapper, LabMapper labMapper,
                                KnowledgeFileStorage knowledgeFileStorage,
                                ApplicationEventPublisher applicationEventPublisher, ObjectMapper objectMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.labMapper = labMapper;
        this.knowledgeFileStorage = knowledgeFileStorage;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO upload(KnowledgeDocumentUploadDTO uploadDTO) {
        Long adminId = requireAdminId();
        validateUpload(uploadDTO);
        List<String> applicableLabIds = normalizeApplicableLabIds(uploadDTO.applicableLabIds());
        byte[] source = readAndValidateSource(uploadDTO);
        String extension = extensionOf(uploadDTO.file().getOriginalFilename());

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(IdWorker.getId());
        document.setLogicalDocumentCode(uploadDTO.logicalDocumentCode().trim());
        document.setVersion(uploadDTO.version().trim());
        document.setTitle(uploadDTO.title().trim());
        document.setEffectiveAt(toUtc(uploadDTO.effectiveAt()));
        document.setApplicableLabIds(writeApplicableLabIds(applicableLabIds));
        document.setPublishStatus(KnowledgePublishStatus.DRAFT);
        document.setIndexStatus(KnowledgeIndexStatus.PENDING);
        document.setSourceFilePath(document.getId() + "/source." + extension);
        document.setCreatedBy(adminId);
        try {
            knowledgeDocumentMapper.insert(document);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "逻辑文档编号和版本不能重复");
        }
        try {
            knowledgeFileStorage.store(document.getId(), extension, source);
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.DEPENDENCY_UNAVAILABLE,
                    "知识源文件保存失败");
        }
        applicationEventPublisher.publishEvent(new KnowledgeIndexRequestedEvent(document.getId()));
        return toVO(document);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KnowledgeDocumentPageVO findPage(KnowledgeDocumentQueryDTO queryDTO) {
        requireAdminId();
        KnowledgeDocumentQueryDTO criteria = queryDTO == null
                ? new KnowledgeDocumentQueryDTO(null, null, null, null, null) : queryDTO;
        int page = criteria.page() == null ? 1 : criteria.page();
        int size = criteria.size() == null ? 20 : criteria.size();
        long total = knowledgeDocumentMapper.countPage(criteria.logicalDocumentCode(), criteria.publishStatus(),
                criteria.indexStatus());
        List<KnowledgeDocumentVO> items = knowledgeDocumentMapper.selectPage(criteria.logicalDocumentCode(),
                        criteria.publishStatus(), criteria.indexStatus(), (long) (page - 1) * size, size)
                .stream().map(this::toVO).toList();
        return new KnowledgeDocumentPageVO(items, page, size, total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO publish(Long documentId) {
        requireAdminId();
        KnowledgeDocument document = lockLogicalDocumentVersions(documentId);
        if (document.getIndexStatus() != KnowledgeIndexStatus.SUCCEEDED) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "只有索引成功的文档版本可以发布");
        }
        knowledgeDocumentMapper.disablePublishedByLogicalCode(document.getLogicalDocumentCode(), documentId);
        knowledgeDocumentMapper.publish(documentId);
        applicationEventPublisher.publishEvent(new KnowledgePublishedVersionChangedEvent(documentId));
        document.setPublishStatus(KnowledgePublishStatus.PUBLISHED);
        return toVO(document);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO disable(Long documentId) {
        requireAdminId();
        KnowledgeDocument document = requireLockedDocument(documentId);
        if (document.getPublishStatus() != KnowledgePublishStatus.PUBLISHED) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "只有已发布文档版本可以停用");
        }
        if (knowledgeDocumentMapper.disable(documentId) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "文档发布状态已变化，请刷新后重试");
        }
        applicationEventPublisher.publishEvent(new KnowledgePublishedVersionChangedEvent(documentId));
        document.setPublishStatus(KnowledgePublishStatus.DISABLED);
        return toVO(document);
    }

    /**
     * 校验管理员上传所需的业务字段、源文件扩展名和生效时间。
     *
     * @param uploadDTO 原始上传输入
     */
    private void validateUpload(KnowledgeDocumentUploadDTO uploadDTO) {
        if (uploadDTO == null || uploadDTO.file() == null || uploadDTO.file().isEmpty()
                || !StringUtils.hasText(uploadDTO.logicalDocumentCode()) || !StringUtils.hasText(uploadDTO.title())
                || !StringUtils.hasText(uploadDTO.version()) || uploadDTO.effectiveAt() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "知识文档上传参数不完整");
        }
        if (uploadDTO.file().getSize() > MAX_SOURCE_FILE_SIZE) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, ApiCode.FILE_TOO_LARGE,
                    "知识文档不能超过 5 MiB");
        }
        String extension = extensionOf(uploadDTO.file().getOriginalFilename());
        if (!"md".equals(extension) && !"txt".equals(extension)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiCode.UNSUPPORTED_MEDIA_TYPE,
                    "仅支持 UTF-8 的 .md 和 .txt 文档");
        }
    }

    /**
     * 读取上传文件并严格校验 UTF-8 编码和非空正文。
     *
     * @param uploadDTO 已完成基本字段校验的上传输入
     * @return 原始 UTF-8 字节
     */
    private byte[] readAndValidateSource(KnowledgeDocumentUploadDTO uploadDTO) {
        try {
            byte[] content = uploadDTO.file().getBytes();
            if (!StringUtils.hasText(knowledgeFileStorage.decodeUtf8(content))) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "知识文档正文不能为空");
            }
            return content;
        } catch (CharacterCodingException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "知识文档必须使用 UTF-8 编码");
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "无法读取上传知识文档");
        }
    }

    /**
     * 规范化可选适用实验室编号并验证每个编号均存在，禁止重复元数据。
     *
     * @param applicableLabIds 原始适用实验室集合
     * @return 保持输入顺序的去重实验室编号
     */
    private List<String> normalizeApplicableLabIds(List<String> applicableLabIds) {
        if (applicableLabIds == null || applicableLabIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
        for (String labId : applicableLabIds) {
            if (!StringUtils.hasText(labId) || !uniqueIds.add(labId.trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "适用实验室编号不能为空或重复");
            }
            Lab lab = labMapper.selectById(labId.trim());
            if (lab == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.LAB_NOT_FOUND, "适用实验室不存在");
            }
        }
        return List.copyOf(uniqueIds);
    }

    /**
     * 从文件名提取小写扩展名，缺少扩展名时返回空文本。
     *
     * @param originalFilename 客户端提交的原始文件名
     * @return 小写扩展名
     */
    private String extensionOf(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int position = originalFilename.lastIndexOf('.');
        return position < 0 ? "" : originalFilename.substring(position + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 锁定并获取文档版本，不存在时返回稳定的通用未找到错误。
     *
     * @param documentId 文档版本主键
     * @return 被锁定的文档版本
     */
    private KnowledgeDocument requireLockedDocument(Long documentId) {
        if (documentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "文档版本编号不能为空");
        }
        KnowledgeDocument document = knowledgeDocumentMapper.selectByIdForUpdate(documentId);
        if (document == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.NOT_FOUND, "知识文档版本不存在");
        }
        return document;
    }

    /**
     * 先读取逻辑文档号，再按稳定顺序锁定同一逻辑文档的全部版本，避免并发发布各自先锁目标行。
     *
     * @param documentId 待发布文档版本主键
     * @return 已位于完整逻辑版本锁集合中的目标版本
     */
    private KnowledgeDocument lockLogicalDocumentVersions(Long documentId) {
        if (documentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "文档版本编号不能为空");
        }
        KnowledgeDocument existing = knowledgeDocumentMapper.selectById(documentId);
        if (existing == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.NOT_FOUND, "知识文档版本不存在");
        }
        return knowledgeDocumentMapper.selectByLogicalCodeForUpdate(existing.getLogicalDocumentCode()).stream()
                .filter(item -> documentId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ApiCode.NOT_FOUND, "知识文档版本不存在"));
    }

    /**
     * 从认证上下文获取管理员身份；知识库管理不信任请求体内的用户标识。
     *
     * @return 当前管理员主键
     */
    private Long requireAdminId() {
        if (CurrentUser.requireRole() != UserRole.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "仅管理员可以管理知识库");
        }
        return CurrentUser.requireId();
    }

    /**
     * 将适用实验室集合安全序列化为数据库 JSON 字段。
     *
     * @param applicableLabIds 已验证实验室编号
     * @return JSON 数组文本或空值
     */
    private String writeApplicableLabIds(List<String> applicableLabIds) {
        if (applicableLabIds.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(applicableLabIds);
        } catch (JacksonException exception) {
            throw new IllegalStateException("适用实验室元数据序列化失败", exception);
        }
    }

    /**
     * 将数据库 JSON 字段解析为接口返回的实验室编号集合。
     *
     * @param applicableLabIds JSON 数组文本
     * @return 不可变实验室编号集合
     */
    private List<String> readApplicableLabIds(String applicableLabIds) {
        if (!StringUtils.hasText(applicableLabIds)) {
            return List.of();
        }
        try {
            String[] values = objectMapper.readValue(applicableLabIds, String[].class);
            return List.copyOf(List.of(values));
        } catch (JacksonException exception) {
            throw new IllegalStateException("知识文档适用实验室元数据损坏", exception);
        }
    }

    /**
     * 将数据库 UTC 时间转换为接口固定的上海偏移时间。
     *
     * @param value UTC 时间
     * @return 上海偏移时间
     */
    private OffsetDateTime toShanghai(LocalDateTime value) {
        return value == null ? null : value.atOffset(UTC).atZoneSameInstant(TimeConfig.BUSINESS_ZONE).toOffsetDateTime();
    }

    /**
     * 将带偏移输入时间转换为数据库使用的 UTC LocalDateTime。
     *
     * @param value 带偏移的输入时间
     * @return UTC LocalDateTime
     */
    private LocalDateTime toUtc(OffsetDateTime value) {
        return value.withOffsetSameInstant(UTC).toLocalDateTime();
    }

    /**
     * 将知识文档实体转换为不含文件路径的管理端响应。
     *
     * @param document 知识文档版本实体
     * @return 管理端文档摘要
     */
    private KnowledgeDocumentVO toVO(KnowledgeDocument document) {
        int chunkCount = document.getChunkCount() == null
                ? knowledgeChunkMapper.countByDocumentId(document.getId()) : document.getChunkCount();
        return new KnowledgeDocumentVO(document.getId(), document.getLogicalDocumentCode(), document.getVersion(),
                document.getTitle(), toShanghai(document.getEffectiveAt()), readApplicableLabIds(document.getApplicableLabIds()),
                document.getPublishStatus(), document.getIndexStatus(), chunkCount, document.getIndexFailureReason());
    }
}
