# 模块 03：设备报修与工单流转

## 目标

学生可生成并确认自己的报修草案、查询自己的工单；管理员可处理工单并决定实验室是否进入维护。报修行为不自动修改实验室状态。

## 数据与对象

`repair_ticket(id,user_id,lab_id,equipment_info,description,safety_risk,status,resolution_note,processed_by,created_at,updated_at)`。`status` 只允许 `SUBMITTED -> PROCESSING -> RESOLVED`；`resolution_note` 在管理员改变状态时必填。DTO 包含 `labId`、`equipmentInfo`、`description`、`safetyRisk`，不包含 `userId`。

实现 `RepairTicketService.prepareCreate`、`confirmCreate`、`findMine`、`process(ticketId, status, resolutionNote, CurrentUser)`。草案创建调用公共动作草案服务，真实 insert 仅在确认事务中发生。

`RepairTicket` 是持久化 Entity，字段、索引和 `version` 以 [数据库设计](../database-design.md) 为准；Controller 使用 `RepairDraftCreateDTO`、`RepairTicketUpdateDTO`，响应使用 `RepairTicketVO`、`ActionDraftVO` 与 `Result<T>`。Controller 不处理状态流转，也不直接调用 Mapper；实现目录和命名遵从 [后端开发规范](../backend-conventions.md)。

## 业务规则和安全处理

1. 实验室必须存在；设备名称/资产号与故障描述不得为空。
2. 描述（可先采用不区分大小写关键词）含“冒烟”“漏电”“焦糊味”时，服务端强制 `safetyRisk=true`，响应的 `safetyNotice` 固定为停止使用、避免自行维修、联系管理员。模型不得改写为继续操作指导。
3. 高风险提示与草案可同时返回，但不会自动提交工单。
4. 学生列表查询永远带 `user_id=currentUser.id`；管理员处理前检查角色，记录 `processed_by`。
5. 实验室进入 `MAINTENANCE` 是独立管理员操作；之后新预约被拒绝，既有预约按课程约定保留并由管理员线下处理。

## 接口和响应

- `POST /api/repair-drafts`：创建 `CREATE_REPAIR_TICKET` 草案，返回安全提示及完整 payload。
- `GET /api/repair-tickets/me`：当前用户的工单。
- `PATCH /api/admin/repair-tickets/{id}`：只允许相邻状态转移并强制处理说明。
- 真实提交仍由 `POST /api/actions/{actionId}/confirm` 统一完成。

错误码：`REPAIR_REQUIRED_FIELD_MISSING`、`REPAIR_TICKET_NOT_FOUND`、`INVALID_TICKET_TRANSITION`、`RESOLUTION_NOTE_REQUIRED`、`FORBIDDEN`。

## 验收测试

- 缺实验室、设备或描述时不能生成草案。
- “B402 的 GPU-03 开机后有焦糊味”返回安全提示、风险标记与待确认草案，确认后只创建一张工单。
- 同一工单不能从 `SUBMITTED` 直接变 `RESOLVED`；处理说明为空失败。
- 学生无法查询他人工单或调用管理员处理接口；报修后实验室状态不变。
