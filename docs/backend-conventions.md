# Spring Boot 后端项目通用开发规范

项目采用常见的分层架构，保持职责清晰、命名统一，避免过度设计。

## 1. 推荐目录结构

```text
src/main/java/<base-package>/
├── Application.java
│
├── controller/        # HTTP 接口层
├── service/           # 业务逻辑层
│   └── impl/          # Service 实现
├── mapper/            # 数据访问层
├── entity/            # 数据库实体
├── dto/               # 请求参数对象
├── vo/                # 响应对象
├── config/            # 配置类
├── common/            # 公共类、统一响应等
├── exception/         # 异常及全局异常处理
├── enums/             # 枚举
└── util/              # 通用工具类

src/main/resources/
├── application.yml
├── mapper/            # MyBatis XML
└── db/                # SQL 等数据库文件
```

## 2. 分层职责

### Controller

负责 HTTP 接口。

主要职责：

* 接收请求参数
* 使用 DTO 接收请求数据
* 参数校验
* 调用 Service
* 返回统一响应结果

Controller 应保持简洁，不编写复杂业务逻辑，不直接操作 Mapper。

### DTO

DTO（Data Transfer Object）用于接收请求数据。

原则：

* Controller 原则上使用 DTO 接收前端请求
* 不直接使用 Entity 接收请求
* 根据业务场景拆分 DTO
* DTO 中可以使用参数校验注解

命名统一：

```text
创建       XxxCreateDTO
修改       XxxUpdateDTO
查询       XxxQueryDTO
登录       LoginDTO
注册       RegisterDTO
```

不要为了复用而创建一个巨大的 `XxxDTO` 承担所有请求场景。

### Service

负责核心业务逻辑。

主要职责：

* 业务规则校验
* 数据处理
* 调用 Mapper
* 调用 Redis 等基础设施
* 事务控制
* Entity、DTO、VO 之间的数据转换或组织

复杂业务必须放在 Service，而不是 Controller 或 Mapper。

### Entity

Entity 表示数据库实体，通常与数据库表对应。

原则：

* Entity 主要用于持久化
* 字段与数据库结构保持合理映射
* 不直接作为 Controller 的请求对象
* 原则上不直接作为接口响应返回给前端

命名统一：

```text
Xxx
```

通常不需要：

```text
XxxEntity
```

### Mapper

负责数据库访问。

主要职责：

* CRUD
* SQL 查询
* 数据持久化

原则：

* 简单 CRUD 优先使用 MyBatis-Plus 提供的方法
* 复杂查询再使用自定义 Mapper 方法或 XML
* Mapper 中不编写业务逻辑

命名统一：

```text
XxxMapper
```

### VO

VO（View Object）表示接口返回给前端的数据。

原则：

* 只包含前端需要的数据
* 不暴露密码、内部状态等敏感或无关字段
* 可以组合多个 Entity 的数据
* Entity 和 VO 不要求字段完全一致

命名统一：

```text
XxxVO
```

## 3. 标准数据流

请求：

```text
前端
 ↓
DTO
 ↓
Controller
 ↓
Service
 ↓
Entity
 ↓
Mapper
 ↓
Database
```

响应：

```text
Database
 ↓
Mapper
 ↓
Entity
 ↓
Service
 ↓
VO
 ↓
Controller
 ↓
统一响应对象
 ↓
前端
```

核心原则：

```text
DTO       → 请求数据
Entity    → 持久化数据
VO        → 响应数据
```

## 4. Service 命名

Service 接口：

```text
XxxService
```

Service 实现：

```text
XxxServiceImpl
```

一般结构：

```text
service/
├── XxxService.java
└── impl/
    └── XxxServiceImpl.java
```

## 5. Controller 命名

统一：

```text
XxxController
```

Controller 只负责：

```text
接收请求
→ 参数校验
→ 调用 Service
→ 返回结果
```

避免：

```text
Controller
→ 直接操作 Mapper
→ 编写 SQL
→ 编写大量业务判断
```

## 6. 统一响应

所有 Controller API 使用统一响应结构，例如：

```text
code
message
data
```

泛型形式：

```text
Result<T>
```

常见形式：

```text
Result<XxxVO>
Result<List<XxxVO>>
Result<PageResult<XxxVO>>
Result<Void>
```

避免不同接口使用完全不同的响应格式。

## 7. 异常处理

统一使用全局异常处理机制。

推荐：

```text
exception/
├── BusinessException
└── GlobalExceptionHandler
```

业务异常由 Service 抛出，由全局异常处理器转换为统一 API 响应。

避免在每个 Controller 中大量编写：

```text
try
catch
return error
```

## 8. 参数校验

请求参数优先通过 Bean Validation 进行校验，例如：

```text
@NotNull
@NotBlank
@Size
@Min
@Max
@Pattern
```

Controller 使用：

```text
@Valid
```

业务层仍然负责业务规则校验。

即：

```text
DTO 参数校验
    ↓
检查格式、空值、长度等

Service 业务校验
    ↓
检查业务规则
```

## 9. 枚举

具有固定状态集合的数据优先定义枚举，不在业务代码中大量使用魔法字符串或魔法数字。

命名：

```text
XxxStatus
XxxType
XxxRole
```

枚举统一放在：

```text
enums/
```

## 10. Config 与 Util

配置类放：

```text
config/
```

例如框架、中间件、Web 等配置。

通用工具放：

```text
util/
```

工具类应该保持通用性。

不要把具体业务逻辑放进 Util。

## 11. Redis 使用原则

Redis 作为缓存、临时状态、分布式控制等基础设施使用。

通常由 Service 根据业务需要访问 Redis。

不要因为引入 Redis 而改变基本分层：

```text
Controller
    ↓
Service
   ↙ ↘
MySQL Redis
```

## 12. 事务规范

涉及多个相关数据库写操作时，在 Service 层进行事务控制。

例如：

```text
@Transactional
Service Method
    ↓
写操作 A
    ↓
写操作 B
    ↓
写操作 C
```

任何一步失败，应根据业务要求整体回滚。

事务不要放在 Controller 层。

## 13. 命名统一

```text
Entity              Xxx

DTO
创建                XxxCreateDTO
修改                XxxUpdateDTO
查询                XxxQueryDTO
登录                LoginDTO
注册                RegisterDTO

VO                  XxxVO

Controller          XxxController

Service             XxxService

Service 实现        XxxServiceImpl

Mapper              XxxMapper

状态枚举            XxxStatus

类型枚举            XxxType

业务异常            BusinessException
```

DTO、VO 等缩写统一使用大写：

```text
正确：
UserDTO
UserVO

避免：
UserDto
UserVo
```

## 14. 开发约束

开发过程中遵守以下原则：

1. Controller 不直接操作 Mapper。
2. Controller 不编写复杂业务逻辑。
3. 请求参数优先使用 DTO，不直接使用 Entity。
4. Entity 主要用于数据库持久化。
5. API 响应优先使用 VO，不直接暴露 Entity。
6. Service 负责核心业务逻辑。
7. Mapper 只负责数据访问，不负责业务判断。
8. 简单 CRUD 优先使用 MyBatis-Plus，复杂 SQL 再自定义。
9. API 使用统一响应结构。
10. 使用全局异常处理机制。
11. 使用统一参数校验机制。
12. 事务放在 Service 层。
13. 固定状态优先使用枚举，避免魔法值。
14. Redis 等基础设施由 Service 根据业务需要调用。
15. 所有模块保持统一命名和目录结构。
16. 不为了“架构完整”随意增加 BO、DO、PO、Assembler 等额外层次。
17. 只有当业务复杂度确实需要时，再引入额外抽象层。

## 15. 默认开发原则

在没有特殊要求时，后续新增功能统一按照：

```text
Controller
    ↓
DTO
    ↓
Service
    ↓
Entity
    ↓
Mapper
    ↓
Database
```

响应统一按照：

```text
Database
    ↓
Mapper
    ↓
Entity
    ↓
Service
    ↓
VO
    ↓
Controller
    ↓
Result<VO>
```

以简单、清晰、职责单一、容易维护为优先原则，避免过度设计。
