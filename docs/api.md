# REST 接口文档

## 通用约定

- 基路径为 `/api`；除登录和健康检查外都要求 `Authorization: Bearer <accessToken>`。
- 操作者身份只来自 Token；所有请求体均不定义 `userId`。时间使用 ISO-8601 且带 `+08:00`，例如 `2026-09-17T14:00:00+08:00`。
- 所有 Controller 使用 [后端开发规范](backend-conventions.md) 中的 `Result<T>`：`{"code":"OK","message":"成功","data": {...}, "requestId": "req_xxx"}`；错误响应见 [HTTP 返回码与业务码](http-error-codes.md)。Controller 接收 DTO、返回 VO，不能以 Entity 作为请求或响应模型。
- 路径参数、DTO/VO 的字段含义以本接口文档为准；持久化表、时隙唯一键、UTC 时间存储和动作幂等记录必须遵从 [数据库设计](database-design.md)，不得由 Controller 或前端绕过。
- 分页参数：`page` 从 1 开始，默认 20、最大 50；列表 `data` 为 `{items,page,size,total}`。
- 所有草案创建与动作确认只由当前用户操作。`actionId` 是确认接口的持久化幂等键；客户端无需也不应传 `userId`。

## 认证

### `POST /api/auth/login`

请求：`{"username":"student01","password":"<password>"}`。

成功响应 `200`：

```json
{
  "code": "OK",
  "message": "成功",
  "data": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresAt": "2026-09-16T16:00:00+08:00",
    "user": {"id": 1, "username": "student01", "role": "STUDENT", "trainingStatus": "PASSED"}
  },
  "requestId": "req_x"
}
```

## 实验室与预约

| 方法 | 路径 | 权限 | 请求/查询参数 | 成功 |
|---|---|---|---|---|
| GET | `/api/labs` | 已登录 | `name?`、`equipment?`、`minCapacity?`、分页 | 200，实验室摘要列表 |
| GET | `/api/labs/{labId}/availability` | 已登录 | `date` 必填，`from?`、`to?`（`HH:mm`） | 200，开放时间与可用结果 |
| GET | `/api/reservations/me` | STUDENT | `status?`、`from?`、`to?`、分页 | 200，只返回本人 |
| POST | `/api/reservation-drafts` | STUDENT | 下方预约草案请求 | 201，待确认动作 |
| POST | `/api/reservations/{reservationId}/cancellation-draft` | STUDENT | 空 body | 201，取消待确认动作 |

预约草案请求：

```json
{
  "labId": "LAB-B402",
  "startTime": "2026-09-17T14:00:00+08:00",
  "endTime": "2026-09-17T16:00:00+08:00",
  "participantCount": 3
}
```

草案响应（预约、取消、报修共用形状）：

```json
{
  "code": "OK",
  "message": "成功",
  "data": {
    "actionId": "7a9db38a-0d13-4da9-8be1-5ba1c3d8bfa4",
    "actionType": "CREATE_RESERVATION",
    "sessionId": "chat_01J...",
    "expiresAt": "2026-09-16T15:05:00+08:00",
    "payload": {
      "labId": "LAB-B402", "labName": "人工智能实验室",
      "startTime": "2026-09-17T14:00:00+08:00",
      "endTime": "2026-09-17T16:00:00+08:00", "participantCount": 3
    },
    "notices": ["预约人已通过安全培训。"]
  },
  "requestId": "req_x"
}
```

可用性查询只用于展示，绝不承诺最终可预约；确认时由数据库时隙唯一键决定最终结果。

## 报修与工单

| 方法 | 路径 | 权限 | 请求 | 成功 |
|---|---|---|---|---|
| POST | `/api/repair-drafts` | STUDENT | `{labId,equipmentInfo,description,safetyRisk?}` | 201，报修待确认动作 |
| GET | `/api/repair-tickets/me` | STUDENT | `status?`、分页 | 200，本人工单 |
| PATCH | `/api/admin/repair-tickets/{ticketId}` | ADMIN | `{status,resolutionNote}` | 200，更新后的工单 |

`safetyRisk` 只能提示，服务端遇到“冒烟”“漏电”“焦糊味”会强制置为 `true`，并在草案中加入停止使用、避免自行维修、联系管理员的固定安全提示。工单状态仅允许 `SUBMITTED -> PROCESSING -> RESOLVED` 的相邻转移；管理员每次处理都必须提供 `resolutionNote`。

## 动作确认

### `POST /api/actions/{actionId}/confirm`

权限：草案所属用户。请求必须携带草案创建时所在的会话：`{"sessionId":"chat_01J..."}`；普通业务页面创建的草案使用服务端创建的业务会话 ID。该字段防止同一用户的草案被另一会话使用。

首次确认成功和客户端未收到响应后的幂等重放均返回 `200`：

```json
{
  "code": "OK",
  "message": "成功",
  "data": {
    "actionId": "7a9db38a-0d13-4da9-8be1-5ba1c3d8bfa4",
    "actionType": "CREATE_RESERVATION",
    "executionStatus": "SUCCEEDED",
    "idempotentReplay": false,
    "result": {"reservationId": 10001, "reservationNo": "RSV-10001", "status": "CONFIRMED"}
  },
  "requestId": "req_x"
}
```

同一 `actionId` 已成功时，即使 Redis 草案已过期，也返回已持久化的原结果并将 `idempotentReplay` 置为 `true`。尚未执行而 Redis 草案过期时返回 `409 ACTION_DRAFT_EXPIRED`。

## 管理实验室与知识库

| 方法 | 路径 | 请求/参数 | 成功 |
|---|---|---|---|
| PATCH | `/api/admin/labs/{labId}` | `{name?,capacity?,equipmentDescription?,openTime?,closeTime?,status?}` | 200；只允许字段白名单 |
| POST | `/api/admin/knowledge/documents` | `multipart/form-data`：`file`、`logicalDocumentCode`、`title`、`version`、`effectiveAt`、`applicableLabIds?` | 202，返回文档版本与 `indexStatus=PENDING` |
| GET | `/api/admin/knowledge/documents` | `logicalDocumentCode?`、`publishStatus?`、`indexStatus?`、分页 | 200，含分块数和失败原因 |
| POST | `/api/admin/knowledge/documents/{documentVersionId}/publish` | 空 body | 200，发布索引成功的版本并停用旧版本 |
| POST | `/api/admin/knowledge/documents/{documentVersionId}/disable` | 空 body | 200，停止参与新检索 |
| GET | `/api/admin/agent-traces` | `requestId?`、`sessionId?`、分页 | 200，脱敏工具执行记录 |

上传仅接受 UTF-8 的 `.md`、`.txt`。索引任务异步执行；客户端以文档列表中的 `indexStatus` 查看 `PENDING`、`INDEXING`、`SUCCEEDED`、`FAILED` 和失败原因。管理员修改实验室状态后应立即清理该实验室缓存。

## 独立 RAG 问答

### `POST /api/knowledge/questions`

请求：`{"question":"人工智能实验室预约前需要什么条件？"}`。响应 `200`：

```json
{
  "code": "OK",
  "message": "成功",
  "data": {
    "answer": "预约人工智能实验室的人员需要先通过安全培训。",
    "citations": [{
      "documentTitle": "人工智能实验室使用指南", "version": "v1.0",
      "chunkId": "docv-12-003", "excerpt": "……预约人工智能实验室的人员应先完成安全培训……"
    }],
    "retrieval": {"topK": 5, "hitCount": 3}
  },
  "requestId": "req_x"
}
```

引用仅能来自本次检索的、已发布且索引成功的分块。无依据时仍为 `200`，但 `answer` 固定为“当前知识库中没有足够信息”，`citations` 为空。涉及实时空闲/本人预约的问题应引导至聊天 Agent，由业务工具查询。

## Agent 会话与聊天

| 方法 | 路径 | 权限 | 请求 | 成功 |
|---|---|---|---|---|
| POST | `/api/chat/sessions` | 已登录 | `{}` 或 `{name?}` | 201，绑定当前用户的 `sessionId` |
| POST | `/api/chat/sessions/{sessionId}/messages` | 会话所有者 | `{"content":"我们 3 人下周三 14 点需要 GPU 实验室"}` | 200，一轮 Agent 结果 |

消息响应 `data` 至少包含 `sessionId`、`messageId`、`answer`、`citations`、`toolCallCount`，并可包含上文的 `draft` 结构。系统最多执行 8 次工具调用；不返回模型思维链、完整工具参数、Redis Key 或向量记录 ID。聊天响应中没有可代替确认接口的 `confirmed=true` 字段。
