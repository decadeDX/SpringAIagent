# 模块 06：Redis 缓存、会话、草案、限流与降级

## 目标

Redis 只承担可丢失或短期状态，MySQL 是预约、工单和确认幂等的最终事实。完成后具备规定的 4 类 Redis 用途，并对 Redis 不可用作出明确、不会绕过安全边界的响应。

Redis 仅由对应 Service 调用，不能被 Controller 或 Mapper 直接访问；持久化事实、`action_execution` 幂等记录和 `reservation_slot` 并发约束以 [数据库设计](../database-design.md) 为准。代码分层、事务位置和命名遵从 [后端开发规范](../backend-conventions.md)。

## Key 设计

| Key | 值与 TTL | 写入/失效规则 |
|---|---|---|
| `lab:detail:{labId}` | 实验室详情，10 分钟 | 管理员更新实验室后立即删除 |
| `chat:session:{userId}:{sessionId}` | 有界消息与槽位，30 分钟滑动续期 | 每轮追加并裁剪；按用户隔离 |
| `agent:action:{actionId}` | 用户、会话、类型、payload、expiresAt，5 分钟 | 创建草案写入；确认成功/参数变化删除 |
| `rate:chat:{userId}:{window}` | 原子计数，窗口 TTL | 首次 `INCR` 时同时设置过期；超过 20/min 返回 429 |

额外的 RAG 缓存只能储存无个人数据的问答，键必须包含问题归一化值和 `knowledge:published-version`；文档发布或停用时递增该版本或逐项失效。

## 实现要点

1. 使用 `StringRedisTemplate` 或明确 JSON 序列化的 `RedisTemplate`，Key 统一字符串，payload 加 schema version，避免模板目前宽泛的 `Object,Object` 造成反序列化兼容问题。
2. 限流应使用 Lua 或 `SET key 1 EX window NX` + `INCR` 的可靠组合，防止 `INCR` 与设置 TTL 之间崩溃留下永久键。
3. 缓存更新遵循“数据库事务成功后失效”，不是事务内提前删缓存。查询缓存未命中回源 MySQL。
4. Redis 故障：普通实验室查询可绕过缓存回源；聊天限流按文档化的保守策略拒绝或受限降级；草案生成和首次确认必须明确报 `REDIS_UNAVAILABLE`，不能允许直接创建业务事实。
5. Redis 向量检索仅在 Redis Stack/Search 模块可用时启用；否则选择 PGVector 等独立向量库。启动健康检查要验证索引能力而非只验证 PING。

## 验收测试

- 管理员将实验室设维护后，下次详情查询不读取旧缓存；预约立即拒绝。
- 两名用户的相同 sessionId 读取不到彼此上下文/草案。
- 连续第 21 次聊天请求在一分钟窗口内为 429，窗口后可恢复。
- 关闭 Redis：缓存查询仍可回源；草案和确认返回明示错误，且数据库不新增预约或工单。
