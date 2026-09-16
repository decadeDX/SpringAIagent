# 数据库设计建议（MySQL + MyBatis-Plus）

## 选型

关系业务采用 MySQL 8.4 LTS + InnoDB；InnoDB 提供 ACID 事务、行级锁和外键支持，适合预约确认的原子提交。[InnoDB 官方说明](https://dev.mysql.com/doc/refman/8.4/en/innodb-introduction.html) 向量不建议存 MySQL：使用 Redis Stack（RedisJSON/Search 向量索引）或 PGVector，MySQL 只保存可追溯元数据与原文。普通 Redis 键值实例不能替代向量库。

所有表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`；业务事实使用 `BIGINT` 主键，外部可见编号可由 `RES-`/`TKT-` 前缀加 ID 生成。时刻以 UTC `DATETIME(3)` 存储，API 仅接收或返回带 `+08:00` 的 ISO-8601 值。

## 表与关键字段

| 表 | 主键与核心字段 | 约束/索引 |
|---|---|---|
| `sys_user` | `id`、`username`、`password_hash`、`role`、`training_status` | `uk_user_username(username)` |
| `lab` | `id VARCHAR(32)`、名称、容量、设备说明、开放/关闭时间、状态、`version` | 名称普通索引；`version` 供管理员更新乐观锁 |
| `reservation` | `id`、`user_id`、`lab_id`、开始/结束时刻、人数、状态 | `idx_reservation_user_status_end(user_id,status,end_time)`、`idx_reservation_lab_start(lab_id,start_time)` |
| `reservation_slot` | `id`、`lab_id`、`slot_start_time`、`reservation_id` | **`uk_lab_slot(lab_id,slot_start_time)`**、`idx_slot_reservation(reservation_id)` |
| `repair_ticket` | `id`、`user_id`、`lab_id`、设备、描述、安全风险、状态、处理说明、处理人、`version` | `idx_ticket_user_created(user_id,created_at)`、`idx_ticket_status_created(status,created_at)` |
| `knowledge_document` | `id`、逻辑文档号、版本、标题、生效时间、适用实验室、发布/索引状态、源文件、失败原因 | `uk_document_version(logical_document_code,version)`、`idx_document_retrieval(status,index_status)` |
| `knowledge_chunk` | `id`、`document_id`、序号、正文、摘要、向量记录 ID、内容摘要 hash | `uk_chunk_sequence(document_id,sequence_no)`、`idx_chunk_document(document_id)` |
| `action_execution` | `action_id CHAR(36)`、用户、会话、动作类型、状态、payload、业务编号、结果摘要 | 主键/唯一 `action_id`，`idx_action_user(user_id)` |
| `agent_trace` | `id`、requestId、sessionId、userId、工具名、脱敏参数、结果摘要、耗时、错误码 | `idx_trace_request(request_id)`、`idx_trace_user_session(user_id,session_id)` |

`knowledge_document` 的同一逻辑文档最多只能有一个 `PUBLISHED` 版本；这不能由普通联合唯一键直接表达，应在发布事务中锁定该逻辑文档的版本记录，先停用旧版本、再发布新版本，并同步更新向量库检索过滤条件。

## 预约事务与 SQL 要点

1. `SELECT id FROM sys_user WHERE id = ? FOR UPDATE`，以该行串行化同一用户的有效预约上限检查。
2. 查询 `CONFIRMED AND end_time > UTC_TIMESTAMP(3)` 的数量；数量达到 2 时回滚。
3. 插入 `reservation`；逐小时批量插入 `reservation_slot`。
4. 任一时隙触发 `uk_lab_slot` 重复键，捕获 `DuplicateKeyException` 映射为 `RESERVATION_CONFLICT`，整个事务回滚。
5. 同一事务更新 `action_execution` 为成功。取消时更新预约状态并物理删除对应时隙。

复合唯一键能强制每个实验室每个整点最多一个占用，避免“先查再插”并发漏洞；MySQL 的 `UNIQUE` 索引会拒绝重复键。[MySQL UNIQUE 索引说明](https://dev.mysql.com/doc/refman/8.4/en/create-index.html)

## MyBatis-Plus 映射约定

- 后端目录、DTO/VO 命名、统一 `Result<T>`、Controller/Service/Mapper 职责以 [后端开发规范](backend-conventions.md) 为准；本文件只定义持久化约束。
- `SysUserMapper extends BaseMapper<SysUser>` 等单表 Mapper；复杂 SQL 放 `resources/mapper/*.xml`，方法仍声明在 Mapper 接口。
- 服务层用 `@Transactional(rollbackFor = Exception.class)`；绝不从实体 Controller 或 Agent 工具直接调用 Mapper 写核心表。
- 将 `created_at`、`updated_at` 交由 `MetaObjectHandler` 填充；JSON payload 使用 `JSON` 字段并保留 DTO schema 版本。
- 不使用 `@TableLogic` 处理预约时隙、动作执行和知识版本。需要保留历史的预约、工单、文档以状态字段表示；时隙按取消动作物理删除。
