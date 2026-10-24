# 前端页面目录结构

- 技术栈：Vue 3（Composition API）+ Vite 5 + Vue Router 4 + Pinia + Element-Plus + ECharts
- 开发地址：`http://localhost:5173`
- 生产产物：`frontend/dist`
- 接口代理：开发环境 `/api` -> `http://127.0.0.1:8080`

## 一、目录结构

```text
frontend/
├─ index.html
├─ package.json
├─ vite.config.js
├─ public/
└─ src/
   ├─ main.js                    应用入口、Element-Plus、v-perm 指令
   ├─ App.vue
   ├─ api/
   │  ├─ request.js              Axios 实例、Token、统一错误提示
   │  ├─ auth.js                 登录、当前用户、退出
   │  ├─ contract.js             合同 CRUD、导入导出
   │  ├─ attachment.js           上传、预览、下载
   │  ├─ archive.js              归档审核、档案变更
   │  ├─ borrow.js               借阅申请、审批、归还
   │  ├─ warning.js              预警消息
   │  ├─ stats.js                看板、统计、导出
   │  ├─ ledger.js               档案台账、导出
   │  └─ system.js               用户/角色/部门/字典/参数/日志/选项
   ├─ components/
   │  ├─ StatusTag.vue           合同/审核/借阅状态标签
   │  ├─ ChartBox.vue            ECharts 容器与自适应
   │  ├─ FilePreview.vue         PDF、图片、Word 在线预览
   │  └─ FileUploader.vue        多附件上传、进度、删除
   ├─ layout/
   │  └─ AppLayout.vue           侧边菜单、顶栏、预警角标、用户菜单
   ├─ router/
   │  └─ index.js                路由表、登录守卫、权限守卫
   ├─ stores/
   │  ├─ index.js                Pinia 实例
   │  └─ user.js                 Token、用户信息、权限判断
   ├─ styles/
   │  └─ index.css               全局布局、表格、打印样式
   ├─ utils/
   │  ├─ download.js             Blob 下载、文件名解析
   │  └─ format.js               金额、日期、文件大小格式化
   └─ views/
      ├─ login/index.vue
      ├─ dashboard/index.vue
      ├─ contract/
      │  ├─ List.vue
      │  ├─ Form.vue
      │  └─ Detail.vue
      ├─ archive/
      │  ├─ Review.vue
      │  └─ Changes.vue
      ├─ borrow/
      │  ├─ MyBorrow.vue
      │  └─ Approval.vue
      ├─ warning/index.vue
      ├─ stats/index.vue
      ├─ ledger/index.vue
      └─ system/
         ├─ Users.vue
         ├─ Roles.vue
         ├─ Depts.vue
         ├─ Dicts.vue
         ├─ Config.vue
         └─ Logs.vue
```

## 二、路由与权限

| 路径 | 页面 | 权限编码 | 菜单 |
| --- | --- | --- | --- |
| `/login` | 登录 | 公开 | 否 |
| `/dashboard` | 数据看板 | `stats:view` | 是 |
| `/contracts` | 合同台账 | `contract:list` | 是 |
| `/contracts/new` | 合同录入 | `contract:add` | 隐藏 |
| `/contracts/:id/edit` | 编辑合同 | `contract:edit` | 隐藏 |
| `/contracts/:id` | 合同详情 | `contract:view` | 隐藏 |
| `/archive/review` | 归档审核 | `archive:review` | 是 |
| `/archive/changes` | 档案变更 | `change:apply` | 是 |
| `/borrow/my` | 我的借阅 | `borrow:apply` | 是 |
| `/borrow/approval` | 借阅审批 | `borrow:approve` | 是 |
| `/warnings` | 风险预警 | `warning:view` | 是 |
| `/stats` | 统计分析 | `stats:view` | 是 |
| `/ledger` | 档案台账 | `ledger:view` | 是 |
| `/system/users` | 用户管理 | `system:user:list` | 是 |
| `/system/roles` | 角色权限 | `system:role:list` | 是 |
| `/system/depts` | 组织机构 | `system:dept:list` | 是 |
| `/system/dicts` | 字典配置 | `system:dict:list` | 是 |
| `/system/config` | 系统参数 | `system:config:list` | 是 |
| `/system/logs` | 操作日志 | `system:log:list` | 是 |

### 登录与权限守卫

`router/index.js` 的 `beforeEach` 完成：

1. 未登录访问业务页 -> 跳转 `/login?redirect=...`。
2. 已登录访问 `/login` -> 按权限跳转首个可用页面。
3. 访问无权限路由 -> 提示并回到用户可用首页。
4. 页面标题根据 `meta.title` 动态设置。

用户可用首页优先级：看板 -> 合同台账 -> 档案台账 -> 风险预警 -> 我的借阅。

### 按钮权限

`main.js` 注册全局 `v-perm` 指令：

```vue
<el-button v-perm="'contract:add'" type="primary">录入合同</el-button>
<el-button v-perm="['contract:edit', 'contract:add']">编辑</el-button>
```

无权限元素在挂载时直接移除，不渲染为隐藏占位。复杂场景使用 `userStore.hasPerm('xxx')` 控制列、按钮或逻辑分支。

## 三、页面职责

| 页面 | 核心功能 |
| --- | --- |
| `login/index.vue` | 登录、演示账号填充、普通用户注册弹窗（注册后自动登录） |
| `contract/List.vue` | 合同台账、状态页签、组合筛选、附件全文检索、导入/导出、批量导入结果 |
| `contract/Form.vue` | 新增/编辑、付款节点、草稿保存、保存并提交、附件上传 |
| `contract/Detail.vue` | 基本信息、附件预览下载、审核/变更历史、申请变更/借阅入口 |
| `archive/Review.vue` | 待审核列表、详情抽屉、通过/驳回、审核意见 |
| `archive/Changes.vue` | 变更申请列表、变更前后对比、审批 |
| `borrow/MyBorrow.vue` | 我的借阅、状态筛选、归还 |
| `borrow/Approval.vue` | 借阅审批、审批意见、通过/驳回 |
| `warning/index.vue` | 预警列表、类型/已读筛选、跳转合同、手动扫描 |
| `stats/index.vue` | 指标卡、饼图/柱状图、日期/部门/类型筛选、报表导出 |
| `ledger/index.vue` | 分类台账、金额汇总、导出、打印 |
| `system/Users.vue` | 用户检索、新增/编辑、角色分配、重置密码、启停 |
| `system/Roles.vue` | 角色维护、权限树勾选 |
| `system/Depts.vue` | 部门树、上级部门、负责人 |
| `system/Dicts.vue` | 字典与字典项维护 |
| `system/Config.vue` | 系统参数分组编辑 |
| `system/Logs.vue` | 日志多条件查询、请求参数/错误详情抽屉 |

## 四、请求与下载

`api/request.js` 统一处理：

- 请求头注入 `Authorization: Bearer <token>`。
- 解包 `{ code, message, data }`，成功直接返回 `data`；Blob 响应保持完整。
- 401 清理登录态并跳转登录页；403/400 显示后端消息。
- 文件下载使用 `responseType: 'blob'`，由 `utils/download.js` 解析 `Content-Disposition` 文件名。

## 五、构建与适配

```bash
npm install
npm run dev       # 开发
npm run build     # 生产构建，输出 dist/
npm run preview   # 预览生产包
```

项目根目录提供两个构建脚本：

- `build-all.ps1`：前端以 `VITE_BASE=/` 构建，输出可执行 jar，访问 `http://localhost:8080/`。
- `build-war.ps1 -ContextPath archive`：前端以 `VITE_BASE=/archive/` 构建，输出 `archive.war`，部署到 Tomcat 后访问 `http://server:8080/archive/`。

打包后的前端位于后端 jar/war 的 `BOOT-INF/classes/static`，由 Spring Boot 直接托管。

- `router/index.js` 使用 `createWebHistory(import.meta.env.BASE_URL)`，Axios 的 `baseURL` 为 `${BASE_URL}api`，因此同一套源码可通过 `VITE_BASE` 适配根路径或 `/archive` 子路径。
- 部署在子路径时，`dist/index.html` 的资源引用会构建为 `/archive/assets/**`；构建上下文必须与 war 文件名一致。
- 路由使用 `createWebHistory`，后端 `SpaController` 负责把 `/contracts/{id}` 等前端路由回退到 `index.html`。若单独使用 Nginx 托管前端，必须配置 `try_files $uri $uri/ /index.html`。
- 表格列宽按 1600px 桌面宽度优化：合同台账、档案台账的编号、日期、状态与操作列无需横向滚动即可完整显示。
- 档案台账打印样式通过 `.no-print` 隐藏筛选区、分页和操作列。
- 所有页面适配 Element-Plus 中文语言包，交互以办公人员常用方式为主。
