# 后端项目结构与设计

- 根包：`com.enterprise.archive`
- 技术栈：Spring Boot 3.3.5、Java 17、MyBatis-Plus 3.5.7、MySQL 8、JWT、Apache POI、PDFBox
- 启动类：`com.enterprise.archive.ArchiveApplication`
- 服务根路径：`/`（前端静态资源）
- 接口前缀：`/api/**`（由 `WebConfig.configurePathMatch` 为所有 `@RestController` 统一添加）

## 一、包结构

```text
backend/
├─ pom.xml
└─ src/main/
   ├─ java/com/enterprise/archive/
   │  ├─ ArchiveApplication.java           启动类
   │  ├─ ServletInitializer.java           外部 Tomcat 部署入口（WAR）
   │  ├─ aspect/
   │  │  └─ OperationLogAspect.java         @OpLog 审计切面
   │  ├─ common/
   │  │  ├─ BizConstants.java               状态、动作、错误码常量
   │  │  ├─ BizException.java               业务异常
   │  │  ├─ GlobalExceptionHandler.java     统一异常响应
   │  │  ├─ PageResult.java                 分页响应
   │  │  ├─ Result.java                     统一响应体
   │  │  ├─ UserContext.java                ThreadLocal 登录用户
   │  │  └─ annotation/
   │  │     ├─ OpLog.java                   操作日志注解
   │  │     └─ RequirePerm.java             接口权限注解
   │  ├─ config/
   │  │  ├─ JacksonConfig.java              日期时间序列化/反序列化
   │  │  ├─ MybatisPlusConfig.java          分页插件、乐观锁、自动填充
   │  │  ├─ SecurityBeanConfig.java         BCrypt 密码编码器
   │  │  └─ WebConfig.java                  拦截器、CORS
   │  ├─ controller/                        15 个 REST 控制器
   │  ├─ dto/                               请求参数对象
   │  ├─ entity/                            17 个数据库实体
   │  ├─ job/
   │  │  └─ WarningScheduler.java           每日预警扫描任务
   │  ├─ mapper/                            MyBatis-Plus Mapper
   │  ├─ security/
   │  │  ├─ AuthInterceptor.java            JWT 认证 + 接口权限校验
   │  │  ├─ JwtUtil.java                    Token 签发与解析
   │  │  └─ LoginUser.java                  登录用户与权限集合
   │  ├─ service/                           业务服务
   │  └─ vo/                                响应视图对象
   └─ resources/
      ├─ application.yml
      └─ mapper/                           自定义 SQL XML（如有）
```

## 二、分层与请求链路

```text
HTTP /api/**
  → AuthInterceptor（JWT + UserContext + @RequirePerm）
  → Controller（参数校验、权限注解、@OpLog）
  → Service（业务规则、事务、数据权限）
  → Mapper（MyBatis-Plus / XML）
  → MySQL
  → Result / PageResult 统一 JSON
  → GlobalExceptionHandler 统一异常
```

### 控制器职责

| 控制器 | 前缀 | 职责 |
| --- | --- | --- |
| `AuthController` | `/auth` | 登录、当前用户、退出 |
| `ContractController` | `/contracts` | 合同分页/详情/保存/编辑/提交/删除/导入/导出 |
| `AttachmentController` | `/attachments` | 附件上传、列表、预览、下载、删除 |
| `ArchiveController` | `/archive` | 归档审核列表/审核、变更申请/审批 |
| `BorrowController` | `/borrows` | 我的借阅、借阅审批、申请、审批、归还 |
| `WarningController` | `/warnings` | 预警列表、未读数、已读、扫描 |
| `StatsController` | `/stats` | 看板、统计、报表导出 |
| `LedgerController` | `/ledger` | 全量/分类台账、台账导出 |
| `UserController` | `/users` | 用户 CRUD、密码重置、状态 |
| `RoleController` | `/roles` | 角色、权限树、保存、删除 |
| `DeptController` | `/depts` | 部门树、保存、删除 |
| `DictController` | `/dicts` | 字典与字典项维护 |
| `ConfigController` | `/configs` | 系统参数查询与更新 |
| `LogController` | `/logs` | 操作日志分页查询（只读） |
| `OptionController` | `/options` | 用户/部门/字典/角色下拉选项 |
| `SpaController` | 无 | 前端 History 路由回退到 `index.html`，不参与 `/api` 前缀 |

### 前后端一体化托管

- 前端构建产物放在 `backend/src/main/resources/static/`，也可由 Maven 在打包时从 `frontend/dist` 自动复制。
- 访问 `http://host:8080/` 返回 `static/index.html`，`/assets/**` 返回 JS/CSS。
- `SpaController` 为 `/login`、`/dashboard`、`/contracts/{id}`、`/ledger`、`/system/*` 等前端路由返回 `forward:/index.html`，保证刷新不 404。
- `WebConfig.configurePathMatch` 只给 `@RestController` 添加 `/api` 前缀，因此静态资源、SPA 页面和 REST 接口互不冲突。
- 登录拦截器只匹配 `/api/**`，不会拦截前端页面与静态资源。
- 执行 `mvn clean package -Pwar` 时改为 WAR 打包，内嵌 Tomcat 变为 `provided`；`ServletInitializer` 让 Tomcat 能启动 Spring 容器。war 部署在 `/archive` 下时，页面为 `/archive/`，接口为 `/archive/api/**`。
- WAR 构建需配合 `build-war.ps1` 指定前端 `VITE_BASE`，不能只改 war 文件名。

### 服务职责

| 服务 | 关键逻辑 |
| --- | --- |
| `AuthService` | 登录校验、加载用户权限、构建 `LoginUser` |
| `ContractService` | 合同 CRUD、数据范围、提交、审核、变更、归档号生成、详情聚合 |
| `AttachmentService` | 文件落盘、扩展名/大小校验、文本抽取、SHA-256、预览下载鉴权 |
| `BorrowService` | 借阅申请、审批、归还、过期处理、借阅台账 |
| `WarningService` | 到期/付款扫描、站内消息去重、已读处理 |
| `StatsService` | 看板指标、维度统计、Excel 报表导出 |
| `ExcelService` | 合同模板导入、合同/台账/统计导出 |
| `UserService` / `RoleService` / `DeptService` / `DictService` / `ConfigService` | 系统基础数据维护与校验 |
| `OperationLogService` | 追加审计日志，不提供删除 |

## 三、认证与权限

1. 登录成功后签发 JWT，前端在 `Authorization: Bearer <token>` 中携带。
2. `AuthInterceptor` 仅对 `/api/**` 生效，放行 `/api/auth/login`、`/api/auth/register` 和 `/error`；静态资源与前端页面不受拦截。
3. 解析 Token 后加载用户、角色、权限集合，写入 `UserContext`。
4. 若 Controller 方法或类上有 `@RequirePerm("xxx")`，则校验权限；无权限返回 HTTP 403。
5. 请求结束后 `UserContext.clear()`，避免线程复用导致串号。
6. 自助注册接口仅接收账号、密码、姓名、手机号、邮箱，服务端固定分配 `VIEWER` 角色与启用状态，避免通过注册接口提权。

权限编码分为菜单权限与按钮权限，例如：

- `contract:list`、`contract:add`、`contract:edit`、`contract:delete`、`contract:submit`
- `contract:view:all` 全量数据查看范围
- `archive:review`、`archive:download:any`
- `change:apply`、`change:review`
- `borrow:apply`、`borrow:approve`
- `warning:view`、`warning:manage`
- `stats:view`、`stats:export`
- `ledger:view`、`ledger:export`、`ledger:print`
- `system:user:*`、`system:role:*`、`system:dept:*`、`system:dict:*`、`system:config:*`、`system:log:list`

## 四、数据权限

`ContractService.applyDataScope` 按权限分层：

| 用户类型 | 可见范围 |
| --- | --- |
| 具备 `contract:view:all` | 全部合同 |
| 具备 `contract:add` 但无全量权限 | 本人创建、本人负责、本部门合同 |
| 普通查阅人员 | 仅 `ARCHIVED` / `EXPIRED` 已归档合同 |

变更记录额外限制：无 `change:review` 权限的用户只能查看本人发起的变更申请，避免越权读取档案变更内容。

归档附件下载：

- 非归档合同：创建人/负责人或具备 `contract:view:all` 才能下载。
- 归档合同：具备 `archive:download:any`，或存在本人审批通过且在借阅时间窗内的借阅记录。
- 预览也需要 `attachment:preview`，并遵守合同可见范围。

## 五、事务与数据一致性

- 合同保存（主表 + 付款节点）、提交、审核、变更申请、变更审批、借阅审批、附件上传均使用 `@Transactional`。
- `contract_info.version` 为乐观锁字段，防止并发覆盖。
- 归档编号生成方法加 `synchronized`，格式 `DA + yyyyMMdd + 4 位流水`，并通过 `uk_archive_no` 唯一索引兜底。
- 预警写入使用 `(biz_key, receiver_id)` 唯一索引 + 先查后插，保证重复扫描不重复推送。
- 变更申请与审批保存变更前后 JSON 快照，审批通过后写回合同主档并保留历史。

## 六、统一响应与异常

```json
// 成功
{ "code": 200, "message": "操作成功", "data": { } }

// 分页
{ "code": 200, "message": "操作成功",
  "data": { "records": [], "total": 0, "current": 1, "size": 10, "pages": 0 } }

// 失败
{ "code": 400, "message": "合同基础信息不完整，请补充后再提交归档", "data": null }
```

`GlobalExceptionHandler` 处理业务异常、参数校验异常、JSON/日期格式异常、未登录/无权限及未知异常。业务失败不会返回堆栈信息给前端。

对于不存在的静态资源或前端路径（如扫描器探测的 `/hybridaction/**`、浏览器请求的 `/favicon.ico`），Spring 静态资源处理器会抛出 `NoResourceFoundException`。系统统一返回 `404 {"code":404,"message":"资源不存在"}`，只在 DEBUG 级别记录资源路径，不打印 ERROR 堆栈，避免无效探测把 `logs/archive-system.log` 刷满；真正的业务异常仍按 500 记录完整堆栈。

## 七、定时任务与附件路径

- `WarningScheduler` 每天 01:10 执行预警扫描；也可由有 `warning:manage` 权限的用户手动触发。
- 附件目录由 `app.file.storage-path`（数据库参数 `file.storage-path` 可覆盖）决定，默认相对启动目录 `./data/uploads`。
- 生产环境建议配置绝对路径，并把该目录纳入备份与磁盘监控。
