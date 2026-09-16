# 模块 08：测试、评价、部署与演示材料

## 目标

把课程的验收条件变为可重复执行的证据：单元/集成测试证明业务规则，真实依赖环境证明并发与 RAG，README 让另一台机器能完成核心演示。

## 测试分层

- 单元测试：时间窗口、开放时间、培训资格、预约上限、取消边界、工单状态机、风险关键词、引用校验。
- Mapper/Service 集成测试：使用 Testcontainers MySQL，验证 `uk_lab_slot`、事务回滚、相同 actionId 的幂等返回。
- Web 集成测试：认证/归属/错误码/草案过期/Redis 不可用；模型和向量库可用受控 fake 以稳定覆盖异常分支。
- 端到端测试：接真实模型与向量库，运行固定 RAG 集、Agent 场景和界面确认。

测试实现也遵从 [后端开发规范](../backend-conventions.md)：Web 测试经 Controller DTO/`Result<VO>` 断言，不直接把 Entity 当 API payload；Mapper 集成测试以 [数据库设计](../database-design.md) 中的表名、唯一键、索引和 UTC 时间存储为断言依据。

## 必须交付的测试资产

`tests/rag-evaluation` 中维护至少 30 条 JSON/CSV/YAML 题目，字段为问题、类别、预期要点、必要 source chunk、是否拒答；类别数量严格满足单文档 10、跨文档 5、无答案 5、变体/错别字 5、旧/冲突版本 5。以相同数据比较两组 chunk/overlap/TopK 参数，记录 TopK 命中、跨文档覆盖、答案正确、引用准确、拒答率、P50/P95。

`tests/concurrency` 提供脚本：20 位已培训账号先各自生成 LAB-B402 同一时段草案，再并发确认。断言恰好一个成功，其余为 `RESERVATION_CONFLICT`，并查询 `reservation` 和 `reservation_slot` 无孤立/重复。另写同一用户的并发草案确认以验证两条上限。

`tests/api-tests` 覆盖题目列出的 16 项业务与 Agent 必测情形，尤其是 5 次同 actionId、确认前被占用、响应丢失后的重试、注入、工具失败和高风险报修。

## 部署结构

最终目录遵循题目结构：`backend/`（或清晰说明现有 Spring Boot 子目录）、`frontend/`、`knowledge/`、`database/schema.sql`、`database/seed.sql`、`deploy/docker-compose.yml`、`deploy/.env.example`、`tests/`、`docs/`、`README.md`。docker-compose 至少启动 MySQL、Redis 和实际选择的向量组件；应用由环境变量注入数据库、Redis、模型和 Embedding 配置。

README 必须逐步说明 JDK/Spring Boot/Spring AI/模型/向量库版本、启动依赖、初始化库、导入并等待索引、预置账号、完成一次预约、运行测试、Token/费用估算、已知限制。不要将真实密钥、实际用户数据或大模型调用日志提交到仓库。

## 演示验收清单

按题目顺序录制/现场操作：文档发布与索引、带引用问答、无依据拒答、培训合格者 GPU 草案和确认、重复确认幂等、未培训拒绝、提示注入拒绝、高风险报修、版本切换、并发结果。每一步保存请求/响应、数据库查询或日志截图作为证据，而不是预录模型回答。
