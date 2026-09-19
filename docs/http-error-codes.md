# HTTP 返回码与业务错误码

## 响应格式

成功响应使用 `Result<T>`：

```json
{"code":200,"message":"成功","data":{},"requestId":"req_x"}
```

失败响应不返回 `data`：

```json
{"code":40901,"message":"该实验室在指定时段已被预约，请重新选择。","requestId":"req_x"}
```

响应体 `code` 是供前端和测试稳定判断的整数。后端仅使用下表的内部字母码，统一响应层负责映射；响应和日志不得包含密码、Token、密钥、SQL 或堆栈。

## 映射表

| HTTP | 内部字母码 | 响应 `code` | 使用场景 |
|---:|---|---:|---|
| 200/201/202 | `OK` | 200 | 查询、创建、更新、异步受理、正常拒答、确认成功或幂等重放。 |
| 400 | `BAD_REQUEST` | 40000 | 请求体、枚举、时间或分页参数格式错误。 |
| 400 | `REPAIR_REQUIRED_FIELD_MISSING` | 40001 | 报修请求缺少必填业务字段。 |
| 401 | `UNAUTHENTICATED` | 40100 | 未登录、Token 无效或登录失败。 |
| 403 | `FORBIDDEN` | 40300 | 角色不足或访问非本人资源。 |
| 403 | `RESERVATION_NOT_OWNED` | 40301 | 预约不属于当前用户。 |
| 404 | `NOT_FOUND` | 40400 | 通用资源不存在或对当前用户不可见。 |
| 404 | `LAB_NOT_FOUND` | 40401 | 实验室不存在。 |
| 404 | `REPAIR_TICKET_NOT_FOUND` | 40402 | 工单不存在。 |
| 409 | `BUSINESS_CONFLICT` | 40900 | 通用状态冲突。 |
| 409 | `RESERVATION_CONFLICT` | 40901 | 时隙已被占用。 |
| 409 | `RESERVATION_LIMIT_REACHED` | 40902 | 当前用户未结束预约已达上限。 |
| 409 | `ACTION_DRAFT_EXPIRED` | 40903 | 待确认草案过期。 |
| 409 | `INVALID_TICKET_TRANSITION` | 40904 | 工单状态不能按当前转换规则流转。 |
| 413 | `FILE_TOO_LARGE` | 41300 | 上传文档超过大小限制。 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 41500 | 上传文件不是 UTF-8 Markdown 或 TXT。 |
| 422 | `BUSINESS_RULE_VIOLATION` | 42200 | 可解析但不满足通用领域规则。 |
| 422 | `LAB_MAINTENANCE` | 42201 | 实验室处于维护状态。 |
| 422 | `TRAINING_REQUIRED` | 42202 | 当前用户未通过安全培训。 |
| 422 | `INVALID_RESERVATION_TIME` | 42203 | 预约时间不在开放时段或不满足时长规则。 |
| 422 | `CANCELLATION_TOO_LATE` | 42204 | 已不足预约开始前 30 分钟，不能取消。 |
| 422 | `RESOLUTION_NOTE_REQUIRED` | 42205 | 管理员处理工单未提供处理说明。 |
| 429 | `RATE_LIMITED` | 42900 | 聊天接口超过每用户每分钟 20 次。 |
| 500 | `INTERNAL_ERROR` | 50000 | 未预期的服务端错误。 |
| 503 | `DEPENDENCY_UNAVAILABLE` | 50300 | Redis、向量库或模型不可用且不能安全降级。 |
| 503 | `REDIS_UNAVAILABLE` | 50301 | 依赖 Redis 的草案或首次确认无法安全执行。 |
| 504 | `DEPENDENCY_TIMEOUT` | 50400 | 模型或向量库调用超时。 |

所有异常由 `@RestControllerAdvice` 统一转换；Controller 不自行捕获业务异常，也不得以 HTTP 200 表示失败。
