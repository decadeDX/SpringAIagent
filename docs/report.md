# 第九部分测试报告

运行日期：2026-09-24。业务、Agent 与并发测试使用项目原有 Spring Boot 配置和 Testcontainers 隔离的 MySQL 8.4、Redis；没有应用 RAG A/B 参数。

## 已执行：业务、Agent 与并发

执行命令：

```powershell
.\mvnw.cmd test "-Dtest=ReservationControllerIntegrationTest,RepairTicketControllerIntegrationTest,RepairTicketServiceIntegrationTest,ActionConfirmationConcurrencyIntegrationTest,ActionConfirmationServiceImplTest,AgentOrchestrationServiceImplTest,AgentTraceServiceImplTest"
```

结果：17 项通过，0 失败，0 错误，0 跳过。

| 范围 | 实际结果 | 证据 |
| --- | --- | --- |
| 预约、归属隔离、确认重放 | 通过 | `ReservationControllerIntegrationTest`（3 项） |
| 报修、归属隔离、高风险规则 | 通过 | `RepairTicketControllerIntegrationTest`、`RepairTicketServiceIntegrationTest`（各 4 项） |
| 20 用户并发、单用户上限、5 次同动作重放 | 通过 | `ActionConfirmationConcurrencyIntegrationTest`（3 项） |
| Agent 防伪造数据库失败、trace 脱敏 | 通过 | 两个单元测试（各 1 项） |

并发验收的 20 用户用例断言并实际通过：1 个成功、19 个 `RESERVATION_CONFLICT`、1 条预约、2 条时隙、0 条孤立时隙；没有出现 `DeadlockLoserDataAccessException`。同一用户三草案并发为 2 成功、1 个 `RESERVATION_LIMIT_REACHED`。

完整可机器读取结果见 [业务与 Agent 结果](../tests/api-tests/results/2026-09-24-business-agent-testcontainers.json) 和 [并发结果](../tests/concurrency/results/2026-09-24-action-confirmation-testcontainers.json)。

## 已执行：真实 Agent HTTP E2E

本地 MySQL 仅以 `mysqldump --single-transaction` 读取并复制到临时 Docker MySQL；测试实例使用临时 MySQL、临时 Redis Stack 和端口 `18080`，真实模型凭据只在测试进程内加载，未写入证据文件。

| 场景 | 结果 | 实际证据 |
| --- | --- | --- |
| 多轮补参后预约草案 | 通过 | 首轮只追问；第二轮调用 3 个工具并生成未确认草案 |
| 工具链 | 部分通过 | `searchAvailableLabs`、`getLabAvailability`、`prepareReservation` 均成功；本请求未触发知识检索 |
| 提示注入 | 通过 | 拒绝替他人确认；草案数为 0，预约数保持 2 |
| 真实工具失败 | 通过 | `prepareRepairTicket` 对无效 `B402` 记录 `LAB_NOT_FOUND`，回复没有伪造成功 |
| 高风险报修 | 通过 | 提示停止使用、禁止自行维修并生成 `safetyRisk=true` 草案，未提交工单 |

临时库 `agent_trace` 由 42 增至 47；5 条新增记录的参数均为 `REDACTED`。逐项请求、响应摘要、trace 与数据库计数见 [真实 Agent E2E 证据](../tests/api-tests/results/2026-09-24-real-agent-e2e.json)。

## 已执行：HTTP 并发验收

在临时 MySQL 导入 `concurrency01` 至 `concurrency20` 后，HTTP 脚本为每位用户生成草案，并在同一 UTC 起始时刻确认。目标时段为 `2026-09-25T14:00:00+08:00` 至 `16:00:00+08:00`。

- 20 个 HTTP 请求：1 个 `200/200`，19 个 `409/40901 RESERVATION_CONFLICT`。
- 数据库查询：`confirmed_reservation_count=1`；`slot_count=2`、`distinct_slot_count=2`、`reservation_reference_count=1`；`orphan_slot_count=0`。
- 未出现 `DeadlockLoserDataAccessException`。

完整原始响应摘要与一致性查询结果见 [HTTP 并发证据](../tests/concurrency/results/2026-09-24-http-concurrency.json)。


