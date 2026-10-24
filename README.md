# 档案合同资料管理分析系统 V1.0

面向企业行政、法务、档案与业务管理人员的一体化合同档案管理平台，覆盖合同录入、附件归集、归档审核、档案变更、查阅借阅、到期预警、统计分析、档案台账与权限管控的完整闭环。

- 后端：Java 17 + Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + MySQL 8.0
- 前端：Vue 3 + Vite 5 + Element-Plus + ECharts + Pinia
- 数据库：`archive_contract`（utf8mb4）
- 一体化部署：前端构建产物内嵌到后端 jar/war，可直接运行或部署到 Tomcat
- 默认接口前缀：`http://localhost:8080/api`

## 一、功能模块

| 模块 | 主要能力 |
| --- | --- |
| 系统基础管理 | 用户、角色、菜单/按钮级 RBAC、部门树、数据字典、系统参数、不可删除操作日志、普通用户自助注册 |
| 档案合同资料采集 | 合同信息表单、草稿保存、PDF/Word/图片多附件上传、Excel 模板批量导入、批量附件上传 |
| 归档审核 | 提交归档、审核通过/驳回及意见、自动生成唯一档案编号、正式归档、归档后只读 |
| 档案变更 | 归档档案变更申请、变更前后快照、变更审批、审批通过后写回主档并保留完整历史 |
| 查阅借阅 | 多条件检索、附件全文检索、在线预览、下载权限控制、借阅申请/审批/归还/到期自动失效、借阅台账 |
| 风险预警 | 合同到期、付款节点提醒，提前天数可配置，站内消息、定时扫描、跳转合同详情 |
| 统计分析 | 看板指标卡、合同类型/部门/年份/金额区间/合作方分布、饼图/柱状图、日期范围筛选、报表及明细导出 |
| 档案台账 | 全量台账及待归档/审核中/已驳回/已归档/已到期分类台账、分页筛选、导出、打印 |

## 二、目录结构

```text
dangan/
├─ backend/                     Spring Boot 后端
│  ├─ src/main/java/com/enterprise/archive/
│  ├─ src/main/resources/       application.yml、Mapper XML
│  └─ pom.xml
├─ frontend/                    Vue 3 前端
│  ├─ src/api/                  接口封装
│  ├─ src/components/           公共组件
│  ├─ src/layout/               主框架布局
│  ├─ src/router/               路由与权限守卫
│  ├─ src/stores/               Pinia 用户状态
│  ├─ src/views/                业务页面
│  └─ vite.config.js
├─ sql/
│  ├─ 01_schema.sql             完整建表 SQL（含数据保护触发器）
│  └─ 02_init_data.sql          初始化数据（角色、权限、字典、演示合同）
├─ docs/                        设计、接口、部署文档
├─ build-all.ps1                前端构建 + 后端 jar 打包
├─ build-war.ps1                前端构建 + 后端 war 打包（外部 Tomcat）
└─ README.md
```

## 三、快速启动

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+ / npm 9+

### 2. 初始化数据库

```bash
mysql -uroot -p < sql/01_schema.sql
mysql -uroot -p < sql/02_init_data.sql
```

> `01_schema.sql` 会重建全部业务表，仅用于首次安装或演示环境重置；生产升级请勿直接执行。

### 3. 一体化打包（推荐）

```powershell
cd E:\dangan
.\build-all.ps1
java -jar backend\target\archive-contract-system-1.0.0.jar
```

`build-all.ps1` 会依次执行：

1. `npm run build` 构建前端到 `frontend/dist`。
2. 同步资源到 `backend/src/main/resources/static`。
3. `mvn clean package` 将前端一并打入后端 jar。

启动后直接访问 <http://localhost:8080/>，接口仍为 `http://localhost:8080/api/**`。前端路由使用 History 模式，刷新 `/contracts`、`/ledger` 等页面会由后端回退到 `index.html`。

> 也可以手动执行 `npm run build` 后运行 `mvn -f backend/pom.xml clean package -DskipTests`。Maven 在 `prepare-package` 阶段会自动把已存在的 `frontend/dist` 复制进 jar。

### 4. 部署到外部 Tomcat（WAR）

```powershell
cd E:\dangan
.\build-war.ps1 -ContextPath archive
```

产物：`backend\target\archive.war`。复制到 Tomcat 10.1.x 的 `webapps` 目录并启动，访问：

- 页面：`http://server:8080/archive/`
- 接口：`http://server:8080/archive/api/**`

说明：

- 必须使用 **Tomcat 10.1.x**（Jakarta EE 10），Tomcat 9/8.5 无法运行 Spring Boot 3。
- `-ContextPath ROOT` 生成 `ROOT.war`，部署在根路径 `/`。
- 上下文路径同时影响前端资源路径，修改后必须重新执行脚本，不能只改 war 文件名。
- 外部 Tomcat 下端口和上下文由 Tomcat 决定，`SERVER_PORT` 配置不生效。
- 环境变量（数据库、`JWT_SECRET`、`FILE_STORAGE_PATH`）在 Tomcat 的 `setenv.sh` / `setenv.bat` 中配置。

### 5. 前后端分离开发

```bash
# 终端 1：启动后端
java -jar backend/target/archive-contract-system-1.0.0.jar

# 终端 2：启动前端开发服务器
cd frontend
npm install
npm run dev
```

开发服务器访问 <http://localhost:5173>，并将 `/api` 代理到 `http://127.0.0.1:8080`。

生产构建产物也可单独部署：

```bash
cd frontend
npm run build          # 产物在 frontend/dist
npm run preview        # 本地预览生产包
```

## 四、演示账号

| 账号 | 密码 | 角色 | 主要权限 |
| --- | --- | --- | --- |
| `admin` | `admin123` | 管理员 | 全部菜单、按钮与数据 |
| `archivist` | `archive123` | 档案管理员 | 归档审核、变更审批、借阅审批、预警、统计、台账 |
| `entry` | `entry123` | 业务录入员 | 合同录入、编辑、提交、附件上传、批量导入、本人/部门数据 |
| `viewer` | `viewer123` | 普通查阅人员 | 已归档合同查阅、借阅申请、台账查看 |

密码使用 BCrypt 存储。首次部署后请立即修改演示账号密码与 `JWT_SECRET`。

登录页提供“立即注册”入口，普通用户可自助注册。注册后默认分配 `VIEWER`（普通查阅人员）角色，可查阅已归档合同、发起借阅申请、查看风险预警与档案台账；角色、部门、状态由服务端固定，注册请求无法指定，避免提权。

## 五、关键配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_HOST` / `DB_PORT` | `127.0.0.1` / `3306` | MySQL 地址 |
| `DB_NAME` | `archive_contract` | 数据库名 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | 数据库账号 |
| `JWT_SECRET` | 内置演示密钥 | JWT 签名密钥，生产必须替换为 ≥32 字节随机值 |
| `JWT_EXPIRE_HOURS` | `8` | 登录有效期（小时） |
| `FILE_STORAGE_PATH` | `./data/uploads` | 附件物理存储目录 |
| `CORS_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | 允许的前端来源 |

合同到期/付款提醒提前天数优先读取数据库 `sys_config.warning_advance_days`，默认 30 天；定时扫描表达式为 `0 10 1 * * ?`（每天 01:10）。

## 六、权限与安全约束

| 约束 | 落地方式 |
| --- | --- |
| 已归档档案禁止物理删除 | `contract_info` 使用 `@TableLogic` 逻辑删除；MySQL 触发器禁止物理删除已归档数据，也禁止将其 `deleted` 置 1 |
| 操作日志不可删除 | 日志仅追加，无删除接口；`trg_log_forbid_delete` 从数据库层拒绝 `DELETE` |
| 审核/变更历史不可删除 | 对应表无删除入口，触发器拒绝物理删除 |
| 附件安全存储 | UUID 重命名、按日期分目录、路径归一化防穿越、SHA-256 校验、文件元数据独立入库、逻辑删除 |
| 接口权限校验 | `AuthInterceptor` 校验 JWT，结合 `@RequirePerm` 校验菜单/按钮权限，无权限返回 403 |
| 按钮动态显隐 | 前端 `v-perm` 指令与 `userStore.hasPerm()` 双重控制，路由守卫拦截无权限页面 |
| 数据隔离 | 非全量数据权限用户仅能访问本人创建/负责或本部门合同；归档档案下载需审批通过的借阅记录 |
| 自助注册 | 注册接口公开但仅创建 `VIEWER` 账号，忽略角色/部门/状态参数；注册行为写入不可删除操作日志 |
| 密码与敏感参数 | BCrypt 存储；操作日志中的 `password` 自动脱敏 |

## 七、文档索引

1. [数据库设计说明](docs/01-database-design.md)
2. [后端项目结构与设计](docs/02-backend-structure.md)
3. [关键业务接口清单](docs/03-api-list.md)
4. [前端页面目录结构](docs/04-frontend-structure.md)
5. [关键业务代码示例](docs/05-key-code.md)
6. [部署说明](docs/06-deployment.md)

完整建表脚本位于 [`sql/01_schema.sql`](sql/01_schema.sql)，初始化数据位于 [`sql/02_init_data.sql`](sql/02_init_data.sql)。

## 八、验证结果

截至 2026-10-24，已完成以下验证：

- 后端编译打包、启动、MySQL 初始化均通过。
- 合同新增 → 上传附件 → 提交归档 → 审核通过 → 生成档案号 → 变更审批 → 借阅审批 → 下载 → 归还全流程通过。
- 归档合同直接编辑被拒绝（HTTP 400），必须走变更审批。
- Excel 模板导入返回 `successCount=1, failCount=0`；合同、台账、统计、导入模板四类 Excel 导出正常。
- 前端 16 个页面路由巡检无控制台错误、无接口 4xx/5xx。
- 一体化 jar 验证通过：`/` 返回页面、`/contracts/12` 刷新正常、静态资源可加载、未登录访问 `/api/**` 返回 401。
- WAR 在 `/archive` 上下文验证通过：登录跳转、`/archive/contracts/12` 刷新、`/archive/api/**` 接口与静态资源均正常。
- 普通用户自助注册验证通过：注册后自动登录、默认 `VIEWER` 角色、仅 12 项权限，无法访问用户管理接口（403），重复账号与非法参数返回 400。
- `admin / archivist / entry / viewer` 四种角色的菜单、按钮、接口权限与越权跳转验证通过。
- 1600px 桌面宽度下合同台账、档案台账的档案号、日期、状态、操作列均完整可见。
