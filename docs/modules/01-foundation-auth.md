# 模块 01：基础设施、认证与统一错误处理

## 目标与完成标准

提供可信的当前用户身份，使后续任何 Service 都从认证上下文取得 `userId` 和 `role`。完成后，`student01`、`student02`、`admin01` 能登录；未认证得到 401，学生访问管理端得到 403，密码与令牌不出现在日志或响应中。

## 数据与包结构

新增 `sys_user` 表：`id`、`username`（唯一）、`password_hash`、`role`、`training_status`、`created_at`、`updated_at`。表名、唯一键和时间字段以 [数据库设计](../database-design.md) 为准；预置账号密码以 BCrypt 摘要写入 `database/seed.sql`，禁止沿用模板中明文 `PASSWORD` 字段。

建议包：`config`（`SecurityConfig`）、`security`（`JwtAuthenticationFilter`、`CurrentUser`）、`user`（`SysUser`、`SysUserMapper`、`AuthService`、`AuthServiceImpl`）、`common`（`Result`、`RequestIdFilter`）、`exception`（`BusinessException`、`GlobalExceptionHandler`）、`dto`（`LoginDTO`）、`vo`（`LoginVO`）。接口只接收 `LoginDTO`，返回短期访问令牌和基本资料，禁止返回 `passwordHash`。

实际命名遵从 [后端开发规范](../backend-conventions.md)：`SysUser` 是持久化 Entity，`LoginDTO` 接收登录请求，`LoginVO` 返回令牌与用户摘要；`AuthController -> AuthService -> SysUserMapper`，Controller 返回 `Result<LoginVO>`，不直接访问 Mapper。

## 实现步骤

1. 选定 Spring Security 的无状态 Bearer Token 实现；登录仅允许匿名，`/api/admin/**` 需要 `ADMIN`，其他业务接口需登录。
2. 认证过滤器验证令牌后建立 `Authentication`；`CurrentUser.requireId()` 是业务层取得身份的唯一入口。不得从聊天文本、query 或 request body 读取操作者 ID。
3. 在 Controller 使用 Bean Validation 校验 DTO；跨字段、时间、归属等规则留在 Service。
4. 每个请求生成或透传 `X-Request-Id`，异常响应一律为 `{code,message,requestId}`。将异常映射为 400/401/403/404/409/429/503。
5. 配置环境变量读取 MySQL、Redis、模型密钥。提交 `.env.example`，不提交真实密钥；删除现有 `application.yml` 中的硬编码模型 key。

## 关键规则

- 管理员身份只取决于已验证的令牌 Claims；“我是管理员”只是普通聊天内容。
- 用户实体、Agent 轨迹、日志均应对用户名以外的敏感信息最小化暴露；密码、Bearer Token、模型 API Key 永不记录。
- `@PreAuthorize` 是 Controller 的第一层门禁；Service 仍须执行资源归属检查，防止内部调用绕过 Controller。

## 验收测试

- 正确/错误密码登录；密码摘要不等于明文。
- 无 Token 访问 `/api/reservations/me` 为 401；学生 PATCH `/api/admin/labs/LAB-A301` 为 403。
- student01 用 body 中伪造的 `userId=admin01` 时，仍只能看到自己的资源。
- 异常响应均带 requestId，应用日志不包含密码和完整 Token。
