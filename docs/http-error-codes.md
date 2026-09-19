# HTTP 返回码与业务错误码

## 响应格式

成功响应使用 `Result<T>`：

```json
{"code":"OK","message":"成功","data":{},"requestId":"req_x"}
```

失败响应不返回 `data`：

```json
{"code":"RESERVATION_CONFLICT","message":"该实验室在指定时段已被预约，请重新选择。","requestId":"req_x"}
```

`code` 供前端和测试稳定判断，`message` 面向用户。响应和日志不得包含密码、Token、密钥、SQL 或堆栈。

## 状态码

| HTTP | 业务码 | 使用场景 |
|---:|---|---|
| 200 | `OK` | 查询、更新、RAG 正常拒答、确认成功或幂等重放。 |
| 201 | `OK` | 创建会话或动作草案。 |
| 202 | `OK` | 已接收异步文档索引任务。 |
| 400 | `BAD_REQUEST` | 请求体、枚举、时间或分页参数格式错误。 |
| 401 | `UNAUTHENTICATED` | 未登录、Token 无效或登录失败。 |
| 403 | `FORBIDDEN` | 角色不足或访问非本人资源。 |
| 404 | `NOT_FOUND` | 资源不存在；也可用于隐藏非本人资源。 |
| 409 | `RESERVATION_CONFLICT`、`BUSINESS_CONFLICT` | 时隙冲突、预约上限、草案失效、重复确认处理中或状态流转冲突。 |
| 413 | `FILE_TOO_LARGE` | 上传文档超过大小限制。 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 上传文件不是 UTF-8 Markdown 或 TXT。 |
| 422 | `BUSINESS_RULE_VIOLATION` | 请求可解析，但不满足预约、培训、取消或工单规则。 |
| 429 | `RATE_LIMITED` | 聊天接口超过每用户每分钟 20 次。 |
| 500 | `INTERNAL_ERROR` | 未预期的服务端错误。 |
| 503 | `DEPENDENCY_UNAVAILABLE` | Redis、向量库或模型不可用，且不能安全降级。 |
| 504 | `DEPENDENCY_TIMEOUT` | 模型或向量库调用超时。 |

所有异常由 `@RestControllerAdvice` 统一转换；Controller 不自行捕获业务异常，也不得以 HTTP 200 表示失败。
