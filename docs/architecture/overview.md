# 校园实验室智能服务平台：设计总览

## 模板判定与改造边界

现有工程只能复用 Maven、Spring Boot、Spring AI、Redis 的基础接入方式；正式项目以 Spring Boot + MyBatis-Plus 实现关系数据访问，不得复用 `Department`、`LoginUser`、`WarrantyTools` 的领域模型或将内存 `SimpleVectorStore` 作为正式实现。业务实现放入新包 `cn.edu.nwpu.labassistant`，保留旧演示代码仅用于学习对照，不能暴露在最终接口中。

## 分层与依赖方向

`controller -> application/service -> domain -> mapper/infrastructure`。Controller 负责认证上下文、HTTP 参数和响应；Service 是所有规则、事务与权限校验的唯一入口；MyBatis-Plus Mapper 只做数据访问；Spring AI 工具只调用 Service 的只读或“创建草案”方法。模型、Controller、Redis 都不能绕过 Service 直接写业务表。

## 后端实现约束

所有模块必须遵从 [后端开发规范](../backend-conventions.md) 与 [数据库设计](../database-design.md)。代码目录采用 `controller`、`service`/`service.impl`、`mapper`、`entity`、`dto`、`vo`、`config`、`common`、`exception`、`enums`、`util`；不新增 BO、DO、PO、Assembler 等无必要中间层。请求使用按场景拆分的 DTO，响应使用 VO，Controller 统一返回 `Result<T>`，不得直接暴露 Entity 或调用 Mapper。Service 是唯一的业务校验、Redis 调用和 `@Transactional` 边界；Mapper 仅保留单表 MyBatis-Plus CRUD 与必要的 XML/注解 SQL。

## 模块和实施顺序

1. [基础设施、认证与错误处理](../modules/01-foundation-auth.md)：数据库迁移、认证上下文、统一异常和权限。
2. [实验室与预约](../modules/02-labs-reservations.md)：实验室查询、预约草案、并发安全的预约/取消执行。
3. [设备报修](../modules/03-repair-tickets.md)：工单草案、风险提示和管理员流转。
4. [知识库与 RAG](../modules/04-knowledge-rag.md)：文档导入、版本发布、向量检索、引用校验。
5. [Agent 与确认](../modules/05-agent-confirmation.md)：工具调用、多轮会话、草案确认与审计。
6. [Redis 与降级](../modules/06-redis-resilience.md)：缓存、会话、草案和限流的 Redis 实现。
7. [接口与安全](../modules/07-api-security.md)：接口契约、鉴权、错误码和脱敏。
8. [测试与部署](../modules/08-testing-deployment.md)：自动化测试、评价集、并发脚本和部署材料。

前 3 个模块完成后，必须能在完全不调用模型的情况下完成普通业务与并发验证；随后再接入 RAG 与 Agent。

## 核心数据与约束

- `reservation_slot` 建立唯一索引 `(lab_id, slot_start_time)`，是跨用户抢同一时段时的最终防线。
- `action_execution.action_id` 为主键或唯一键。一次确认从已成功记录返回原结果，绝不重复创建业务记录。
- `knowledge_document` 以“逻辑文档编号 + 版本”管理；只有 `PUBLISHED` 且索引成功的版本允许检索。
- 时间统一采用 `Asia/Shanghai`。Java API 使用 `OffsetDateTime`，数据库使用能保留时区语义的 UTC 时间戳或明确约定的 `datetime`，不得混用。

类、关联及服务依赖见 [class-diagram.puml](class-diagram.puml)。可用 IntelliJ PlantUML 插件、PlantUML CLI 或 CI 渲染。

编码前还应以以下材料为准：[后端开发规范](../backend-conventions.md)、[模块审查与技术基线](../review/module-review.md)、[数据库设计](../database-design.md)、[接口文档](../api.md)、[HTTP 返回码与业务码](../http-error-codes.md)。
