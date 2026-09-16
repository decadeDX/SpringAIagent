# 模块 07：REST 接口契约、安全与可观测性

## 目标

交付可由前端、Agent 和测试脚本共同使用的稳定 API，并把认证、权限、资源归属、参数校验和错误码变成可测试契约。以 OpenAPI 3 作为最终接口事实来源，本文是实现约束。

Controller 接收按场景拆分的 DTO、以 `@Valid` 做格式校验、调用 Service 并返回 `Result<VO>`；不直接接收/返回 Entity、调用 Mapper 或编写业务判断。业务异常由 `GlobalExceptionHandler` 转换为统一响应。详细规范见 [后端开发规范](../backend-conventions.md)，端点形状以 [接口文档](../api.md) 为准，持久化字段不与 [数据库设计](../database-design.md) 冲突。

## 最低端点

| 范围 | 端点 |
|---|---|
| 认证 | `POST /api/auth/login` |
| 实验室/预约 | `GET /api/labs`、`GET /api/labs/{id}/availability`、`GET /api/reservations/me`、`POST /api/reservation-drafts`、`POST /api/reservations/{id}/cancellation-draft` |
| 报修 | `POST /api/repair-drafts`、`GET /api/repair-tickets/me` |
| 确认 | `POST /api/actions/{actionId}/confirm` |
| 管理端 | `PATCH /api/admin/repair-tickets/{id}`、`PATCH /api/admin/labs/{id}`、文档上传/查询/发布/停用 |
| 聊天 | `POST /api/chat/sessions`、`POST /api/chat/sessions/{id}/messages` |

所有成功响应应包含数据和 `requestId`；错误统一为 `{code,message,requestId}`。HTTP 400 为格式/字段错误，401 未认证，403 无权限，404 不存在，409 业务冲突/状态冲突，429 限流，503 Redis/模型等外部依赖不可用。禁止以 200 加“失败文本”代替失败状态。

## 认证、授权与归属

- 从 Bearer Token 获得用户；`/me` 固定为令牌拥有者，忽略任何外来 userId。
- 管理端必须角色为 ADMIN；学生不能调用管理员文档、实验室状态和工单处理功能。
- `/{id}` 资源由 Service 读取后再验证所有者，不能只依赖前端隐藏按钮。
- CORS 仅允许实际前端来源、方法和 Header；生产环境禁止 `*` 配合凭据。
- 上传限制扩展名、大小、UTF-8 编码和存储路径；文件名不得直接拼接到路径。

## 模型与日志安全

模型 Key 从环境变量/secret 注入；HTTP、SQL 与 Agent 日志使用脱敏器过滤 password、authorization、api-key、完整预约个人信息。RAG 资料、用户文本和模型返回均是不可信输入：工具 schema 必须强类型，实验室 ID、日期、时长等再次由 Service 校验。不给模型 SQL、Mapper、写操作 Token 或任意 URL/文件系统访问权限。

## 可观测性

请求入口创建 `requestId` 并透传至 Agent Trace；记录调用模型/Embedding 的耗时、重试和不含敏感内容的失败码。健康检查分别报告 MySQL、Redis、向量库和模型配置状态；不在健康端点泄露密钥或内部地址。为预约冲突、确认幂等命中、RAG 无依据拒答、工具失败、限流建立计数指标。

## 验收测试

- 错误 body、HTTP 状态和 requestId 与契约一致。
- 篡改路径 ID、body userId 或 sessionId 均不能跨用户访问。
- 缺少权限的管理请求和恶意上传均被拒绝。
- 日志与错误响应抽查不包含密码、令牌或 API Key；模型超时返回明确 503，不说预约成功。
