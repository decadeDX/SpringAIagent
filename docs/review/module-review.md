# 模块开发文档审查与技术基线

## 审查结论

8 个模块的业务边界、验收用例和依赖顺序与作业要求一致，可以作为实施依据；无需推倒重写。以下修订必须在编码前统一，否则会影响并发、幂等和权限验收。

除本审查结论外，所有实现必须以 [后端开发规范](../backend-conventions.md) 和 [数据库设计](../database-design.md) 为共同约束来源。前者决定代码目录、DTO/VO、`Result<T>`、Service 事务和 Mapper 职责；后者决定表名、字段、索引、时间存储和并发 SQL。本文不复制二者的完整内容，以免形成相互冲突的第二份规范。

| 模块 | 结论 | 必须落实的修订 |
|---|---|---|
| 01 基础认证 | 可实施 | 采用 Spring Security，密码用 BCrypt；所有业务身份只从认证上下文取得。明确使用 Spring Boot 4.1.x、JDK 17、MyBatis-Plus Boot 4 starter。 |
| 02 实验室预约 | 可实施 | `reservation_slot` 的唯一键是最终并发保障；MyBatis-Plus 的乐观锁不能替代它。用户预约上限检查须用用户行锁或等价串行化。 |
| 03 报修 | 可实施 | 状态只能相邻流转，处理说明必填；报修不自动修改实验室状态。 |
| 04 知识库 RAG | 可实施 | MySQL 仅保存文档元数据和分块正文；向量放 Redis Stack 或 PGVector。检索必须限定已发布、索引成功的文档版本。 |
| 05 Agent/确认 | 必须修订 | 确认时先查 MySQL 的 `action_execution`：已成功动作即使 Redis 草案过期也必须返回原结果。草案还需绑定 `sessionId`，确认请求显式携带它。 |
| 06 Redis | 可实施 | Redis 不可用时草案创建和首次确认必须失败；缓存只能在数据库事务提交后失效。 |
| 07 接口安全 | 必须细化 | 用本文档同目录之外的 `api.md` 和 `http-error-codes.md` 作为接口与错误码单一事实来源。 |
| 08 测试部署 | 可实施 | 追加 Mapper 自定义 SQL 的集成测试；并发测试必须直查预约与时隙表证明一致性。 |

## Spring Boot + MyBatis-Plus 基线

使用 JDK 17、Spring Boot 4.1.x、MyBatis-Plus 3.5.17、MySQL 8.4 LTS、Redis 7 + Redis Stack（如选择 Redis 向量库）。MyBatis-Plus 官方已提供 Spring Boot 4 starter；Spring Boot 4.1.1 支持 Java 17 至 26，因此项目以现有 JDK 17 作为统一基线。[MyBatis-Plus 安装说明](https://baomidou.com/en/getting-started/install/) [Spring Boot 系统要求](https://docs.spring.io/spring-boot/system-requirements.html)

使用 `mybatis-plus-spring-boot4-starter`，不要再同时引入普通 MyBatis starter。`BaseMapper<T>` 仅用于单表 CRUD；以下查询必须使用 XML 或注解 SQL，保持 SQL 可见并可压测：

- `SELECT ... FOR UPDATE` 锁定当前用户并统计有效预约；
- 批量写入、删除 `reservation_slot`；
- 按多个预约条件过滤实验室和查询可用性；
- 管理端带关联条件的文档、工单、审计查询。

实体统一使用 `@TableName`、`@TableId`、`@TableField(fill = ...)`；业务表主键可用 `ASSIGN_ID` 的 `BIGINT`，实验室编号保留业务唯一键 `VARCHAR`。实体只用于持久化，Controller 请求使用 DTO、响应使用 VO 与 `Result<T>`。只对管理员可更新且确有版本语义的 `lab`、`repair_ticket` 加 `@Version`。不要对 `reservation_slot`、`action_execution` 使用乐观锁，也不要全局启用逻辑删除：取消预约必须物理释放时隙，确认幂等记录不得被隐藏。

数据库脚本采用 Flyway 管理，`database/migration/V1__schema.sql`、`V2__seed.sql`；课程要求的 `database/schema.sql`、`seed.sql` 可由同一来源导出或在 README 明确说明。所有库表使用 `utf8mb4`、`InnoDB`、UTC 存储时间戳，应用边界转换为 `Asia/Shanghai`。

## 关键确认流程修订

确认 `actionId` 时先以 `action_id + user_id` 查询 `action_execution`：若状态为 `SUCCEEDED`，直接返回其中保存的结果，不读取 Redis。无持久化执行记录时，才读取 Redis 草案并校验用户、会话、动作类型和 5 分钟有效期；随后在单事务中插入/锁定执行记录、重新校验业务规则、写业务事实和时隙、将执行记录置成功。唯一键竞争者读取同一执行记录并返回已完成结果或明确“处理中”。

这项顺序同时满足“Redis 草案过期后首次确认失败”与“成功后响应丢失，再次确认返回原结果”。
