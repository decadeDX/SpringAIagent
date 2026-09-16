# 模块 02：实验室、预约草案与并发安全执行

## 目标与范围

实现实验室筛选、可用性查询、预约草案、本人预约查询、取消草案及确认后的真实执行。所有 REST 和 Agent 工具复用同一个 `ReservationService`，任何写入必须经模块 05 的确认服务。

## 数据模型

- `lab(id,name,capacity,equipment_description,open_time,close_time,status,updated_at)`；初始化 LAB-A301、LAB-B402、LAB-C205。
- `reservation(id,user_id,lab_id,start_time,end_time,participant_count,status,created_at,cancelled_at)`；状态至少 `CONFIRMED`、`CANCELLED`。
- `reservation_slot(id,lab_id,slot_start_time,reservation_id)`；唯一键 `uk_lab_slot(lab_id,slot_start_time)`。

`Reservation` 聚合根拥有 1 至 3 个 `ReservationSlot`。Mapper 至少提供按条件查询、锁定用户行/等价串行化查询、插入预约、批量插入时隙、取消并删除时隙。禁止仅“先查空闲再插预约”。

实现遵从 [后端开发规范](../backend-conventions.md)：`Lab`、`Reservation`、`ReservationSlot` 仅作 Entity；Controller 分别接收 `LabQueryDTO`、`ReservationDraftCreateDTO`，返回 `LabVO`、`ReservationVO`、`ActionDraftVO` 的 `Result<T>`。简单查询使用 `BaseMapper`，`FOR UPDATE`、可用性查询、批量写删时隙写在 `resources/mapper` XML；事务只放在 `ReservationServiceImpl`。

表字段、`uk_lab_slot(lab_id,slot_start_time)`、用户锁定 SQL 和 UTC `DATETIME(3)` 存储严格遵从 [数据库设计](../database-design.md)；Controller 和 Agent 不得以查询结果替代确认事务中的最终校验。

## Service 接口

- `LabService.search(LabQuery)`：名称、设备关键词、最低容量筛选。
- `LabService.availability(labId, date, start, end)`：按有效时隙判断。
- `ReservationService.prepareCreate(CreateReservationCommand, CurrentUser)`：只校验和返回 payload，不写业务表。
- `ReservationService.confirmCreate(ReservationPayload, CurrentUser)`：事务内复核并创建。
- `ReservationService.prepareCancel(reservationId, CurrentUser)` 与 `confirmCancel(...)`：只能处理本人预约。
- `ReservationService.findMine(CurrentUser)`：仅按当前用户过滤。

## 创建预约的事务算法

1. 校验 `Asia/Shanghai`、开始时间整点、时长为 1/2/3 小时、未来且不超过 7×24 小时、不跨天、工作日、开放时间、人数范围及实验室非维护。
2. 对 B402/C205 校验当前预约人 `trainingStatus=PASSED`；不以参与者文字描述替代此检查。
3. 锁定当前用户或采用等价可靠机制，统计尚未结束的 `CONFIRMED` 预约；达到 2 条拒绝。
4. 插入 `reservation`，按 `[start,end)` 生成每个整点的 `reservation_slot`。唯一键冲突映射为 `RESERVATION_CONFLICT` 并使全事务回滚。
5. 在同一事务中更新 `action_execution` 成功状态。只有事务成功后才删除草案缓存或失效可用性缓存。

取消时重新查询预约归属、状态及距离开始是否至少 30 分钟；事务内更新为 `CANCELLED` 并删除其时隙。被取消的预约不计入“最多两条”。

## API 与错误码

实现文档要求的 `GET /api/labs`、`GET /api/labs/{id}/availability`、`GET /api/reservations/me`、草案创建/取消接口。返回草案时必须展示实验室、起止时间、人数、限制、`actionId`、5 分钟到期时间。

标准业务码：`LAB_NOT_FOUND`、`LAB_MAINTENANCE`、`TRAINING_REQUIRED`、`INVALID_RESERVATION_TIME`、`RESERVATION_LIMIT_REACHED`、`RESERVATION_CONFLICT`、`RESERVATION_NOT_OWNED`、`CANCELLATION_TOO_LATE`。

## 验收测试

- 边界：周末、非整点、0 人、超容量、跨天、4 小时、7 天外、开放时间外均失败。
- B402：student01 可准备草案，student02 被拒绝。
- 同一实验室同一时段 20 个不同用户并发确认，恰好一个成功；数据库无孤立时隙。
- 同一用户并发确认 3 个不同草案，最终有效预约最多两条。
- `[14:00,16:00)` 和 `[16:00,18:00)` 可同时成功；他人预约不可查询/取消。
