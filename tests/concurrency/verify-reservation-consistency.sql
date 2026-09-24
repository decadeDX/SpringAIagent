-- 在 run-reservation-confirm.ps1 输出的测试时段替换 @slot_start_utc 后执行。
SET @lab_id = 'LAB-B402';
SET @slot_start_utc = '2026-09-22 06:00:00';
SET @slot_end_utc = DATE_ADD(@slot_start_utc, INTERVAL 2 HOUR);

SELECT COUNT(*) AS confirmed_reservation_count
FROM reservation
WHERE lab_id = @lab_id
  AND start_time = @slot_start_utc
  AND end_time = @slot_end_utc
  AND status = 'CONFIRMED';

SELECT COUNT(*) AS slot_count,
       COUNT(DISTINCT slot_start_time) AS distinct_slot_count,
       COUNT(DISTINCT reservation_id) AS reservation_reference_count
FROM reservation_slot
WHERE lab_id = @lab_id
  AND slot_start_time >= @slot_start_utc
  AND slot_start_time < @slot_end_utc;

SELECT COUNT(*) AS orphan_slot_count
FROM reservation_slot s
LEFT JOIN reservation r ON r.id = s.reservation_id
WHERE s.lab_id = @lab_id
  AND s.slot_start_time >= @slot_start_utc
  AND s.slot_start_time < @slot_end_utc
  AND r.id IS NULL;
