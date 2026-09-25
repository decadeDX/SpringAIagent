# 校园实验室智能服务平台：架构总览

## 工程边界

本项目后端实际包名为 `io.github.decadedx.springaiagent`，位于仓库根目录的 `src/`；`frontend/` 为独立 Vue 3 应用。系统不保留或依赖旧模板领域模型，也不使用内存向量库作为正式检索实现。

## 分层与依赖方向

```text
Vue 前端 / HTTP 测试脚本
            ↓
Controller（认证、DTO 校验、Result<VO>）
            ↓
Service / ServiceImpl（规则、权限、事务、编排）
      ↙          ↓           ↘
MyBatis Mapper   Redis       Spring AI 适配器
      ↓          ↓           ↓
    MySQL   缓存/会话/草案   聊天模型、Embedding、Redis Stack 向量索引
```

Controller 不直接调用 Mapper；Mapper 不承载业务规则；模型与 Agent 工具只调用 Service，不能取得 SQL、Mapper 或最终写入权限。所有对外响应使用 `Result<T>`，请求使用 DTO、响应使用 VO。

## 模块映射

1. [基础设施、认证与统一错误处理](../modules/01-foundation-auth.md)：JWT、Redis 单账号会话、`requestId` 与统一异常。
2. [实验室与预约](../modules/02-labs-reservations.md)：检索、可用性、预约/取消草案和时隙并发控制。
3. [设备报修](../modules/03-repair-tickets.md)：高风险提示、报修草案和管理员状态流转。
4. [知识库与 RAG](../modules/04-knowledge-rag.md)：文档版本、异步索引、向量检索和引用校验。
5. [Agent 与确认](../modules/05-agent-confirmation.md)：多轮会话、受控工具、草案确认与执行审计。
6. [Redis 与降级](../modules/06-redis-resilience.md)：缓存、限流、会话、草案和依赖故障边界。
7. [接口与安全](../modules/07-api-security.md)：REST 契约、归属校验、脱敏和可观测性。
8. [测试与部署](../modules/08-testing-deployment.md)：Testcontainers、真实 Agent、并发脚本与交付材料。

## 核心一致性边界

- `reservation_slot` 的唯一约束 `(lab_id, slot_start_time)` 是跨用户抢占同一时隙的最终保障。
- `action_execution.action_id` 是动作确认的持久化幂等依据；成功后即使 Redis 草案过期也能重放原结果。
- MySQL 保存预约、工单、文档元数据、时隙、执行结果和 trace；Redis 只保存缓存、会话、草案、限流及向量索引。
- 预约、时隙写入与动作执行结果在同一事务中完成；取消在同一事务中更新预约并删除时隙。
- 只有 `PUBLISHED` 且 `SUCCEEDED` 的知识文档版本允许参与检索；发布新版会停用旧版并失效相关缓存。

## 关键流程

### 草案—确认

用户或 Agent 调用准备接口仅得到绑定用户和会话、有效期五分钟的 Redis 草案。`POST /api/actions/{actionId}/confirm` 先处理已完成的 `action_execution` 重放；首次执行则校验草案归属和时效，并在事务内重新校验领域规则后写入预约、取消或工单结果。

### RAG

管理员上传 Markdown/TXT 后异步切分、Embedding 并写入 Redis Stack；问答只检索有效版本的 TopK 分块。模型输出必须引用本轮上下文中的 `chunkId`，后端通过引用校验器拒绝伪造或失效引用；无依据时返回固定拒答语。

### Agent

`AgentOrchestrationService` 提供有限会话历史和不可覆盖的系统约束。`LabAssistantTools` 只暴露知识检索、实验室/个人记录查询及“准备草案”工具；工具调用数上限为八次，审计只保存工具名、脱敏参数、结果摘要、耗时和错误码。

详细类关系见 [class-diagram.puml](class-diagram.puml)，数据约束见 [数据库设计](../database-design.md)，接口形状见 [API 文档](../api.md)。
