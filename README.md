# 校园实验室智能服务平台

基于 Spring Boot、Spring AI、MySQL 与 Redis Stack 的实验室服务平台。学生可查询实验室、通过“草案—确认”流程预约或报修；管理员可维护实验室、知识文档版本及脱敏 Agent trace。RAG 仅检索已发布且索引成功的文档版本，Agent 仅能调用受控业务工具，不能直接写数据库或绕过确认。

## 目录说明

本项目采用 Maven 单仓库布局，根目录 `src/` 即题目结构中的 `backend/`：

```text
src/                    Spring Boot 后端（controller / service / mapper / config）
frontend/               Vue 3 + TypeScript + Vite 前端
database/               schema.sql 与 seed.sql
deploy/                 Docker Compose 与 .env.example
knowledge/uploads/      运行期上传文件目录（不提交用户上传内容）
tests/                  RAG 题集、接口/Agent 证据和并发脚本
docs/                   架构、接口、报告及模块设计材料
```

后端的主要代码入口是 `src/main/java/io/github/decadedx/springaiagent/SpringAIagentApplication.java`；接口契约见 [docs/api.md](docs/api.md) 和 [docs/openapi.yaml](docs/openapi.yaml)。架构、数据模型和关键流程见 [docs/design.md](docs/design.md)，验收操作顺序见 [docs/demo.md](docs/demo.md)。

## 运行环境

- JDK 17
- Maven Wrapper（无需单独安装 Maven）
- Node.js 20+、npm
- Docker Desktop / Docker Compose
- MySQL 8.4、Redis Stack（由 `deploy/docker-compose.yml` 启动）
- Spring Boot 4.1.1、Spring AI 2.0.1、MyBatis-Plus 3.5.17
- 聊天模型：OpenAI 兼容接口；Embedding：OpenAI 兼容接口。实际供应商、模型和维度由环境变量决定。

## 配置模型与基础依赖

1. 复制模板，**不要提交**真实密钥：

   ```powershell
   Copy-Item deploy/.env.example deploy/.env
   ```

2. 在 `deploy/.env` 填写数据库、Redis、JWT 和模型配置。至少需要：

   ```text
   OPENAI_BASE_URL
   OPENAI_API_KEY
   OPENAI_CHAT_MODEL
   DASHSCOPE_EMBEDDING_BASE_URL
   DASHSCOPE_API_KEY
   DASHSCOPE_EMBEDDING_MODEL
   DASHSCOPE_EMBEDDING_DIMENSIONS
   ```

   `OPENAI_*` 用于聊天与工具调用；`DASHSCOPE_EMBEDDING_*` 用于文档向量化。设 `KNOWLEDGE_RAG_ENABLED=true` 才会启用真实 RAG、向量检索和模型适配器。

3. 启动 MySQL 和 Redis Stack：

   ```powershell
   docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
   ```

   默认端口为 MySQL `127.0.0.1:3307`、Redis `127.0.0.1:6379`、Redis Insight `127.0.0.1:8001`。

4. 启动后端。Flyway 会自动执行 `src/main/resources/db/migration/` 中的表结构、账号和实验室基础数据迁移：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. 启动前端：

   ```powershell
   Set-Location frontend
   npm ci
   npm run dev
   ```

前端默认开发地址为 `http://localhost:5173`，后端默认地址为 `http://localhost:8080`。

## 数据库、账号与知识库

### 初始化数据库

常规启动不需要手工执行 SQL：后端 Flyway 会初始化空库。`database/schema.sql` 和 `database/seed.sql` 用于需要手动建库的场景；演示账号及实验室基础数据与 Flyway 迁移保持一致。

| 账号 | 密码 | 角色 | 培训状态 |
| --- | --- | --- | --- |
| `student01` | `student01` | 学生 | 已通过 |
| `student02` | `student02` | 学生 | 未通过 |
| `admin01` | `admin01` | 管理员 | 不适用 |

### 导入和发布知识文档

以管理员登录后，在前端管理页面上传 UTF-8 Markdown/TXT；也可调用 `POST /api/admin/knowledge/documents` 的 multipart 接口，字段为 `file`、`logicalDocumentCode`、`title`、`version`、`effectiveAt`、`applicableLabIds`。

上传返回 `202 Accepted`，索引在事务提交后异步执行。使用 `GET /api/admin/knowledge/documents` 查看：

- `indexStatus=SUCCEEDED` 且 `chunkCount > 0`：索引构建完成；
- `indexStatus=FAILED`：查看 `indexFailureReason`；
- 只有索引成功的版本才能调用 `POST /api/admin/knowledge/documents/{id}/publish` 发布；发布新版本会停用同一逻辑文档的旧版本。

原始六篇课程知识文档目前位于本机 `docs/knowledge-base/`。该目录被 `.gitignore` 排除，提交仓库或压缩包前必须将原始资料及旧版本测试样例放入可交付目录并确认已包含；不要把真实 API Key、Redis 数据或用户上传文件一并提交。

## 一次预约示例

1. 以 `student01` 登录前端。
2. 在“创建预约”页面选择 `LAB-B402`，填写未来 7 天内工作日的整点时段、1–3 小时时长和不超过容量的人数；或在聊天页面提出完整需求。
3. 系统只生成有效期 5 分钟的预约草案，草案不占用时隙。
4. 核对草案卡片中的实验室、时间、人数和提示后，点击确认；`POST /api/actions/{actionId}/confirm` 会重新校验培训、时段、预约上限和冲突。
5. 首次确认成功后返回真实预约编号；再次确认相同 `actionId` 返回幂等结果，不会创建第二条预约。

## 测试

业务与并发的 Testcontainers 回归：

```powershell
.\mvnw.cmd test "-Dtest=ReservationControllerIntegrationTest,RepairTicketControllerIntegrationTest,RepairTicketServiceIntegrationTest,ActionConfirmationConcurrencyIntegrationTest,ActionConfirmationServiceImplTest,AgentOrchestrationServiceImplTest,AgentTraceServiceImplTest"
```

HTTP 并发脚本（仅对专用测试库运行）：

```powershell
pwsh -File tests/concurrency/run-reservation-confirm.ps1 -BaseUrl http://localhost:8080
```

运行前向**测试库**导入 `tests/concurrency/seed-concurrent-users.sql`；脚本期望 1 个成功和 19 个 `40901 RESERVATION_CONFLICT`。随后按 `tests/concurrency/verify-reservation-consistency.sql` 对实际 UTC 时段查询预约、时隙和孤立时隙。

真实 Agent、HTTP 并发与 Testcontainers 的已执行证据见：

- [业务/Agent 结果](tests/api-tests/results/)
- [并发结果](tests/concurrency/results/)
- [实验报告](docs/report.md)

RAG 的固定 30 题题集位于 [tests/rag-evaluation/questions.json](tests/rag-evaluation/questions.json)。真实模型评测需要先导入并发布知识文档，再显式设置 `RUN_RAG_E2E=true`；

