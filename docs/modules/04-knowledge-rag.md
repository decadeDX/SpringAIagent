# 模块 04：知识文档、版本化索引与 RAG 回答

## 目标

管理员上传 UTF-8 Markdown/TXT，建立可追溯的向量索引，发布与停用版本；用户问答必须只依据本次检索到的有效分块，并返回可验证引用。现有启动即加载资源文件的 `SimpleVectorStore` 仅作学习样例，不能用于此模块。

## 数据与状态

`knowledge_document` 保存逻辑文档号、标题、版本、生效时间、适用实验室、发布状态、索引状态、源文件位置、失败原因；`knowledge_chunk` 保存文档版本 ID、分块序号、原文、摘要、向量记录 ID。建议约束 `unique(logical_document_code, version)`、`unique(document_id, sequence_no)`。

持久化表名、唯一索引、MySQL 与向量库职责以 [数据库设计](../database-design.md) 为准：`KnowledgeDocument`、`KnowledgeChunk` 仅作 Entity，向量不写入 MySQL。上传接口接收 `KnowledgeDocumentUploadDTO`，列表/问答返回 `KnowledgeDocumentVO`、`RagAnswerVO` 的 `Result<T>`；`KnowledgeServiceImpl` 负责发布事务和索引编排，`KnowledgeDocumentMapper`/`KnowledgeChunkMapper` 不承担版本状态判断。目录与命名遵从 [后端开发规范](../backend-conventions.md)。

状态：导入时 `DRAFT/PENDING`，异步索引中 `INDEXING`，成功 `SUCCEEDED`；仅索引成功的版本可发布。发布新版本以事务把同逻辑文档的旧 `PUBLISHED` 版本改为 `DISABLED`，新版本改为 `PUBLISHED`。停用后从向量库删除或在检索条件中严格过滤，且清除含该版本的 RAG 缓存。

## 导入和检索流程

1. `POST /api/admin/knowledge/documents` 校验管理员、扩展名、UTF-8、元数据。存原文件并创建导入记录。
2. `KnowledgeIndexService` 清洗文本，按 500–800 中文字符、80–120 字重叠切分，为每块生成稳定的 `chunkId`（如 `文档版本ID-序号`），写 MySQL 后调用 Embedding 与向量库。失败记录原因，避免发布半成品。
3. `RagService.ask(question)` 仅检索 `PUBLISHED + SUCCEEDED` 元数据过滤的 TopK 4–6 块。将 `chunkId`、标题、版本和原文置入明确的上下文边界。
4. Prompt 指令模型只能用提供的块回答，并按指定 chunkId 引用；模型返回后 `CitationValidator` 校验每个引用都在本次集合且支持结论。无足够依据时返回固定拒答语，不伪造引用。
5. 响应返回 `requestId`、回答、引用数组 `{title,version,chunkId,excerpt}` 与检索耗时。实时问题由 Agent/业务工具查询，不得由 RAG 猜测。

## 接口、运维和安全

提供文档上传、列表（含分块数量/失败原因）、发布、停用和问答接口。文件内容是非可信数据：提示词中明确标记为资料，不当作系统指令；不得让其触发工具。向量库必须支持元数据过滤与持久化（Redis Stack、PGVector 等 Spring AI 兼容实现），普通 Redis 键值服务不满足要求。

缓存键须含文档有效版本集合或全局 `knowledge:version`；发布/停用时失效。缓存不得跨用户保存含预约等个人实时信息的回答。

## 验收测试

- 导入至少 6 篇、合计至少 8,000 汉字，含要求的文档类型与 15 组 FAQ。
- 发布成功版本能检索，停用后同问题不再引用；索引失败版本不能发布。
- 每个引用确实存在于本轮 TopK 的有效块；无答案问题返回“当前知识库中没有足够信息”。
- 执行 30 题固定测试集并比较两组切分/检索参数，报告命中、正确、引用准确、拒答率及 P50/P95。
