# HTTP 返回码与业务错误码

## 错误响应形状

```json
{
  "code": "RESERVATION_CONFLICT",
  "message": "该实验室在指定时段已被预约，请重新选择。",
  "requestId": "req_x"
}
```

`code` 供前端、测试脚本稳定判断；`message` 面向用户，不包含 SQL、Token、路径、模型 Key 或堆栈。所有异常经 `@RestControllerAdvice` 映射，禁止使用 HTTP 200 表示失败。

所有成功与失败响应均是 [后端开发规范](backend-conventions.md) 规定的 `Result<T>` 形状：成功时 `code=OK`、`message=成功`，失败时 `data` 为 `null` 或省略。Controller 不在接口内自行 `try/catch` 业务异常。

## HTTP 状态码

| HTTP | 使用场景 | 代表业务码 |
|---:|---|---|
| 200 | 查询、状态变更、RAG 正常拒答、确认首次成功或幂等重放 | `OK` |
| 201 | 成功创建会话或待确认草案 | `ACTION_DRAFT_CREATED`、`CHAT_SESSION_CREATED` |
| 202 | 已接受异步文档索引任务 | `KNOWLEDGE_INDEX_ACCEPTED` |
| 400 | JSON 格式、枚举、日期格式或分页参数无法解析 | `MALFORMED_REQUEST`、`INVALID_ENUM` |
| 401 | 未提供、无效或过期的访问令牌；登录凭据错误 | `UNAUTHENTICATED`、`AUTH_INVALID_CREDENTIALS` |
| 403 | 已认证但角色不符，或访问非本人资源 | `FORBIDDEN`、`RESOURCE_NOT_OWNED` |
| 404 | 资源不存在；对非本人资源可用 404 隐藏其存在性 | `LAB_NOT_FOUND`、`RESERVATION_NOT_FOUND`、`CHAT_SESSION_NOT_FOUND` |
| 409 | 预约时隙冲突、预约上限、草案过期/会话不符、动作正在执行、非法状态转移 | `RESERVATION_CONFLICT`、`ACTION_DRAFT_EXPIRED` |
| 413 | 上传文件超过上限 | `DOCUMENT_FILE_TOO_LARGE` |
| 415 | 非 `.md`/`.txt` 或 MIME 类型不支持 | `DOCUMENT_MEDIA_TYPE_UNSUPPORTED` |
| 422 | 请求可解析但违反领域输入规则或前置条件 | `INVALID_RESERVATION_TIME`、`TRAINING_REQUIRED` |
| 429 | 聊天接口每用户每分钟超过 20 次 | `CHAT_RATE_LIMITED` |
| 500 | 未预期的内部错误 | `INTERNAL_ERROR` |
| 503 | Redis、向量库或模型服务不可用，且安全降级不可行 | `REDIS_UNAVAILABLE`、`MODEL_UNAVAILABLE` |
| 504 | 已调用外部模型/向量服务但超时 | `MODEL_TIMEOUT`、`VECTOR_STORE_TIMEOUT` |

## 业务错误码表

| 业务码 | HTTP | 触发条件与前端处理 |
|---|---:|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | 用户名或密码错误；展示通用登录失败提示。 |
| `UNAUTHENTICATED` | 401 | 缺失/失效 Token；跳转登录。 |
| `FORBIDDEN` | 403 | 角色无权限；不重试。 |
| `RESOURCE_NOT_OWNED` | 403/404 | 用户访问他人预约、工单、会话或草案；不泄露资源详情。 |
| `LAB_NOT_FOUND` | 404 | 实验室编号不存在。 |
| `LAB_MAINTENANCE` | 422 | 实验室维护中，不能准备或确认预约。 |
| `INVALID_RESERVATION_TIME` | 422 | 非整点、非工作日、跨天、时长非 1/2/3 小时、非未来或超 7 天、开放时间外。 |
| `INVALID_PARTICIPANT_COUNT` | 422 | 人数小于 1 或超过容量。 |
| `TRAINING_REQUIRED` | 422 | 当前用户未通过 B402/C205 所需安全培训。 |
| `RESERVATION_LIMIT_REACHED` | 409 | 当前用户未结束的已确认预约已达两条。 |
| `RESERVATION_CONFLICT` | 409 | 插入时隙触发唯一键冲突；提示选择其他时段。 |
| `RESERVATION_NOT_CANCELLABLE` | 409 | 已取消、已开始或已结束预约不能取消。 |
| `CANCELLATION_TOO_LATE` | 422 | 距开始不足 30 分钟。 |
| `ACTION_DRAFT_NOT_FOUND` | 404 | 不存在的草案动作且无成功执行记录。 |
| `ACTION_DRAFT_EXPIRED` | 409 | 首次确认时 Redis 草案已过期；重新生成草案。 |
| `ACTION_SESSION_MISMATCH` | 409 | 草案与确认请求会话不一致。 |
| `ACTION_IN_PROGRESS` | 409 | 相同动作正在另一个请求中执行；客户端短暂重试。 |
| `ACTION_PAYLOAD_INVALID` | 422 | 草案 payload 与动作类型不匹配或内容已失效。 |
| `REPAIR_REQUIRED_FIELD_MISSING` | 422 | 缺实验室、设备信息或故障描述。 |
| `REPAIR_TICKET_NOT_FOUND` | 404 | 工单不存在。 |
| `INVALID_TICKET_TRANSITION` | 409 | 工单状态非相邻转移。 |
| `RESOLUTION_NOTE_REQUIRED` | 422 | 管理员处理未提供处理说明。 |
| `DOCUMENT_FILE_TOO_LARGE` | 413 | 超过上传大小限制。 |
| `DOCUMENT_MEDIA_TYPE_UNSUPPORTED` | 415 | 文件不是 UTF-8 Markdown/TXT。 |
| `DOCUMENT_INDEX_NOT_READY` | 409 | 未索引成功版本尝试发布。 |
| `DOCUMENT_VERSION_CONFLICT` | 409 | 逻辑文档版本重复或并发发布冲突。 |
| `KNOWLEDGE_DOCUMENT_NOT_FOUND` | 404 | 文档版本不存在。 |
| `CHAT_SESSION_NOT_FOUND` | 404 | 会话不存在或对当前用户不可见。 |
| `CHAT_RATE_LIMITED` | 429 | 一分钟内第 21 次及以后聊天请求；展示稍后再试。 |
| `AGENT_TOOL_LIMIT_EXCEEDED` | 422 | 单轮达到 8 次工具调用上限；展示未完成信息。 |
| `REDIS_UNAVAILABLE` | 503 | 草案/首次确认无法安全执行；不得转为直写业务表。 |
| `VECTOR_STORE_UNAVAILABLE` | 503 | RAG 检索依赖不可用。 |
| `MODEL_UNAVAILABLE` | 503 | Chat/Embedding 服务拒绝或不可连接。 |
| `MODEL_TIMEOUT` | 504 | 模型调用超时；不得虚构成功结果。 |
| `INTERNAL_ERROR` | 500 | 未预期异常；响应仅带 requestId，完整堆栈写服务端日志。 |
