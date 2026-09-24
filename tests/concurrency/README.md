# 预约确认并发验收

1. 向本地测试数据库导入 [seed-concurrent-users.sql](seed-concurrent-users.sql)，其中的 20 个账号均为已培训学生，密码均是 `student01`；不要导入生产数据库。
2. 启动应用，确保 `LAB-B402` 为 `ACTIVE` 且目标时段没有已有预约。
3. 在 PowerShell 7 中运行：

   ```powershell
   pwsh -File tests/concurrency/run-reservation-confirm.ps1 -BaseUrl http://localhost:8080
   ```

   脚本会选择下一个工作日上海时间 14:00—16:00，先串行创建 20 份草案，随后用线程屏障同时提交确认。HTTP 结果必须是 1 个 `200/200` 和 19 个 `409/40901 (RESERVATION_CONFLICT)`。

4. 将脚本打印的 UTC 开始时间写入 [verify-reservation-consistency.sql](verify-reservation-consistency.sql) 的 `@slot_start_utc` 后执行。三个查询分别必须得到：`confirmed_reservation_count = 1`、`slot_count = distinct_slot_count = 2` 且 `reservation_reference_count = 1`、`orphan_slot_count = 0`。

同一用户的并发上限由 `ActionConfirmationConcurrencyIntegrationTest#shouldKeepOneStudentsConcurrentConfirmedReservationsWithinTwoReservationLimit` 覆盖：三个不同时间草案并发确认，应只有两条预约成功，另一条返回 `RESERVATION_LIMIT_REACHED`。
