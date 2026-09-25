# 系统设计说明

## 1. 设计目标与业务边界

系统把“知识回答”和“真实业务办理”分开处理：RAG 只回答可由已发布文档支持的问题；实时空闲、本人预约和工单状态由受控业务工具查询。预约、取消和报修都必须经过“草案—确认—执行”，模型文本不能替代确认请求。

角色仅有学生与管理员。当前操作者只从 Bearer JWT 建立的认证上下文取得，聊天内容、请求体和工具参数均不能指定或伪造其他用户。

## 2. 技术与部署结构

| 层次 | 实现 | 职责 |
| --- | --- | --- |
| 前端 | Vue 3、TypeScript、Vite | 登录、聊天、草案确认、预约/报修列表与管理页面 |
| 后端 | Spring Boot、Spring Security、Bean Validation | REST、认证授权、统一错误、事务和业务编排 |
| 关系数据 | MySQL 8.4、MyBatis-Plus、Flyway | 业务事实、时隙约束、执行幂等、文档元数据和审计 |
| 短期状态与向量检索 | Redis Stack、Spring Data Redis、Spring AI Redis Vector Store | 缓存、单账号会话、聊天上下文、草案、限流和向量索引 |
| 模型 | OpenAI 兼容 Chat/Embedding 适配器 | 回答生成、向量化和 Spring AI 工具调用 |

部署依赖定义在 `deploy/docker-compose.yml`；真实密钥只通过 `deploy/.env` 或进程环境注入，仓库仅保留 `.env.example`。

## 3. 后端分层

```text
controller → service / service.impl → mapper → MySQL
                         ↘
                 Redis / Vector Store / Model Client
```

- Controller 只接收 DTO、执行格式校验并返回 `Result<VO>`；不直接调用 Mapper。
- Service 是资源归属、培训状态、预约规则、工单状态机、事务与 Redis 访问的唯一入口。
- Entity 映射表结构；DTO 承担请求；VO 仅暴露前端需要的字段。
- Mapper 使用 MyBatis-Plus 完成简单 CRUD，锁定、时隙和批量 SQL 位于 XML；不承载业务判断。
- `LabAssistantTools` 只能调用 Service 的查询或准备草案方法，模型不拥有 SQL、文件系统、任意 URL 或最终写入能力。

目录和命名约定见 [后端开发规范](backend-conventions.md)，各业务模块的实现约束见 [模块文档](modules/)。

## 4. 数据模型与一致性

| 表 | 事实职责 |
| --- | --- |
| `sys_user`、`lab` | 身份、角色、培训状态、实验室开放条件与维护状态 |
| `reservation`、`reservation_slot` | 预约事实及逐小时占用；`uk_lab_slot(lab_id, slot_start_time)` 防止重复占用 |
| `repair_ticket` | 报修事实、风险标记、处理人和相邻状态流转 |
| `knowledge_document`、`knowledge_chunk` | 文档版本、索引状态、原文分块与向量记录追溯 |
| `action_execution` | 确认动作的执行状态、结果摘要与持久化幂等 |
| `agent_trace` | 脱敏的工具调用审计，不保存思维链、密钥或原始参数 |

数据库时刻以 UTC `DATETIME(3)` 存储，对外以 `Asia/Shanghai` 的带偏移 ISO-8601 表示。预约创建会锁定当前用户以检查未结束 `CONFIRMED` 预约上限，并在同一事务中写预约、时隙和动作执行结果；任一时隙冲突使事务整体回滚。

## 5. 关键业务流程

### 5.1 预约、取消与报修

1. 学生调用草案接口，Service 校验输入和当前规则，只将绑定用户/会话的草案写入 Redis，TTL 为五分钟。
2. 前端完整展示 payload、限制和安全提示，用户主动调用确认接口。
3. 确认服务先读取 `action_execution`：已成功则返回原结果；否则校验 Redis 草案的用户、会话和时效。
4. 领域 Service 在事务内重新读取数据库并校验当下事实；成功后更新 `action_execution`，随后删除草案。

高风险描述含“冒烟”“漏电”或“焦糊味”时，后端强制风险标记并返回停止使用、避免自行维修、联系管理员的提示；报修本身不自动把实验室改为维护状态。

### 5.2 RAG 与版本切换

1. 管理员上传 UTF-8 Markdown/TXT 以及逻辑文档号、标题、版本、生效时间和适用实验室。
2. 异步任务按配置切分文本，写 `knowledge_chunk`、调用 Embedding 并写 Redis Stack；失败原因回写文档记录。
3. 仅 `PUBLISHED + SUCCEEDED` 文档可检索。发布新版本在事务中停用旧版本，并清理相关 RAG 缓存。
4. RAG 将本轮 TopK 分块作为带边界的上下文，要求模型输出 `chunkId` 和原文摘录；后端仅接受本轮有效候选中的引用。

### 5.3 Agent 与执行追踪

Agent 从会话历史补齐日期、时间、时长、人数和设备；相对日期由服务端解析为上海时区的绝对日期。工具可检索制度、查询候选实验室/可用性/本人记录，或准备草案；工具失败必须如实说明。每次调用产生脱敏 `agent_trace`，最多八次工具调用。

## 6. Redis 与故障边界

| Key 类型 | 典型 TTL | 故障策略 |
| --- | --- | --- |
| `lab:detail:{labId}` 缓存 | 10 分钟 | 可回源 MySQL |
| 聊天会话 | 30 分钟滑动续期 | 不能跨用户读取；不可用时按安全策略拒绝聊天 |
| `agent:action:{actionId}` 草案 | 5 分钟 | 草案生成与首次确认明确失败，不能绕过确认写库 |
| 聊天限流 | 每分钟窗口 | 超过 20 次返回 429 |

Redis 不保存最终预约、工单或幂等事实；这些事实始终由 MySQL 约束和事务保证。

## 7. 安全、异常与可观测性

- Spring Security 区分认证、角色权限和 Service 层资源归属校验；`/me` 接口不接受外来用户 ID。
- 文档、用户消息、模型输出与工具参数都视为不可信输入；提示注入不能改变系统规则或直接确认动作。
- 统一异常响应为 `{code,message,requestId}`，细节见 [HTTP 返回码与业务码](http-error-codes.md)。
- 密码、Bearer Token、API Key、原始工具参数与模型思维链不写日志；诊断使用 `requestId` 和脱敏 `agent_trace`。

## 8. 测试与当前边界

Testcontainers 覆盖预约、报修、确认和数据库一致性；真实模型 Agent 与 HTTP 并发结果记录在 [测试报告](report.md)。
