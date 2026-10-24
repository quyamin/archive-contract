# 关键业务接口清单

- Base URL：独立 jar 为 `http://localhost:8080/api`；WAR 部署在 Tomcat `/archive` 下时为 `http://server:8080/archive/api`
- 认证方式：`Authorization: Bearer <JWT>`
- 请求/响应：`application/json`；上传为 `multipart/form-data`；导出为文件流
- 分页响应：`{ records, total, current, size, pages }`
- 权限列中的 `—` 表示仅需登录，无需额外权限

## 一、认证

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 公开 | 账号密码登录，返回 token、用户、角色、权限 |
| POST | `/auth/register` | 公开 | 普通用户自助注册，成功后直接返回登录态 |
| GET | `/auth/me` | — | 当前用户资料、菜单权限、按钮权限 |
| POST | `/auth/logout` | — | 前端清除登录态；服务端记录退出日志 |

登录请求：

```json
{ "username": "admin", "password": "admin123" }
```

注册请求：

```json
{
  "username": "zhangsan",
  "password": "abc123",
  "realName": "张三",
  "phone": "13900001234",
  "email": "zhangsan@example.com"
}
```

注册规则：

- 账号 4-30 位，以字母开头，仅允许字母、数字、下划线；密码 6-32 位；姓名 2-50 位；手机号、邮箱选填。
- 注册成功后默认分配 `VIEWER`（普通查阅人员）角色，状态为启用，并直接返回与登录相同的 `token`、`user`。
- 角色、部门、状态由服务端固定，请求中传入 `roleIds`、`deptId`、`status` 会被忽略，不能用于提权。
- 注册成功写入操作日志（模块“登录认证”，操作“用户注册”）。

## 二、合同档案

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/contracts` | `contract:list` | 合同分页查询，自动应用数据范围 |
| GET | `/contracts/{id}` | `contract:view` | 详情、附件、付款节点、审核/变更历史 |
| POST | `/contracts` | `contract:add` | 新增合同（草稿） |
| PUT | `/contracts/{id}` | `contract:edit` | 编辑草稿/已驳回合同 |
| POST | `/contracts/{id}/submit` | `contract:submit` | 提交归档审核（校验必填项与附件） |
| DELETE | `/contracts/{id}` | `contract:delete` | 删除草稿/已驳回合同（逻辑删除） |
| POST | `/contracts/import` | `contract:import` | Excel 批量导入，`file` 字段 |
| GET | `/contracts/import-template` | `contract:import` | 下载导入模板 |
| GET | `/contracts/export` | `contract:export` | 导出合同台账 Excel |

`GET /contracts` 常用参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `pageNum` / `pageSize` | int | 页码/每页条数 |
| `keyword` | string | 合同编号/名称/甲乙方/档案号 |
| `attachmentKeyword` | string | 附件原始文件名或附件全文 |
| `status` | string | `DRAFT/PENDING/REJECTED/ARCHIVED/EXPIRED` |
| `category` | string | `draft/reviewing/rejected/archived/expired` |
| `contractType` / `deptId` / `ownerId` | string/long | 类型、部门、负责人 |
| `signDateStart` / `signDateEnd` | `yyyy-MM-dd` | 签订日期范围 |
| `amountMin` / `amountMax` | decimal | 金额范围 |
| `expiringSoon` | boolean | 仅看临近到期 |
| `contractNo` / `contractName` / `partyA` / `partyB` / `archiveNo` | string | 精确条件模糊匹配 |

新增/编辑合同请求：

```json
{
  "contractNo": "HT-2026-0001",
  "contractName": "2026年度办公用品采购合同",
  "partyA": "星瀚科技有限公司",
  "partyB": "华信办公用品有限公司",
  "signDate": "2026-01-10",
  "effectiveDate": "2026-01-15",
  "expireDate": "2027-01-14",
  "amount": 258000.50,
  "contractType": "PURCHASE",
  "deptId": 4,
  "ownerId": 3,
  "securityLevel": "INTERNAL",
  "remark": "年度框架协议",
  "paymentNodes": [
    { "nodeName": "首付款", "planPayDate": "2026-02-01", "amount": 129000.00 }
  ]
}
```

导入结果：

```json
{
  "successCount": 1,
  "failCount": 0,
  "messages": [],
  "contractIds": [14]
}
```

## 三、附件

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/attachments/contract/{contractId}` | `contract:view` | 附件列表 |
| POST | `/attachments/upload` | `attachment:upload` | 批量上传；表单字段 `contractId`、`files`、`fileType` |
| DELETE | `/attachments/{id}` | `attachment:delete` | 逻辑删除附件，仅草稿/已驳回合同允许 |
| GET | `/attachments/{id}/preview` | `attachment:preview` | 在线预览（PDF/图片/Word） |
| GET | `/attachments/{id}/download` | `contract:download` | 下载；归档档案需 `archive:download:any` 或有效借阅 |

上传限制：单文件默认 50 MB，允许 `pdf,doc,docx,xls,xlsx,jpg,jpeg,png,gif`，可在系统参数中调整。

## 四、归档审核

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/archive/reviews` | `archive:review` | 待审核合同列表 |
| POST | `/archive/reviews/{id}` | `archive:review` | 审核合同；`{ "action": "APPROVE", "opinion": "同意归档" }` |
| GET | `/archive/changes` | `change:apply` | 变更申请列表；无 `change:review` 只能看本人 |
| POST | `/archive/changes` | `change:apply` | 提交变更申请，保存变更前后快照 |
| POST | `/archive/changes/{id}/review` | `change:review` | 变更审批；`action=APPROVE/REJECT` |

变更申请示例：

```json
{
  "contractId": 12,
  "changeType": "AMOUNT",
  "changeReason": "补充协议调整合同金额",
  "changeFields": { "amount": 99000.00 }
}
```

审核通过后自动生成 `DA + yyyyMMdd + 4 位流水` 档案编号，写入 `contract_info.archive_no` 与审核记录。

## 五、查阅借阅

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/borrows/my` | `borrow:apply` | 我的借阅记录 |
| GET | `/borrows/approval` | `borrow:approve` | 借阅审批列表 |
| POST | `/borrows` | `borrow:apply` | 提交借阅申请 |
| POST | `/borrows/{id}/review` | `borrow:approve` | 审批借阅；`action=APPROVE/REJECT` |
| POST | `/borrows/{id}/return` | `borrow:apply` | 归还档案 |

借阅申请：

```json
{
  "contractId": 12,
  "purpose": "合同审计需要查阅原件",
  "startTime": "2026-10-25T09:00:00",
  "endTime": "2026-10-27T18:00:00"
}
```

借阅状态：`PENDING / APPROVED / REJECTED / RETURNED / EXPIRED`。仅 `APPROVED` 且当前时间处于借阅区间内可下载归档附件。

## 六、风险预警

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/warnings` | `warning:view` | 当前用户预警列表，可按类型/已读/关键字筛选 |
| GET | `/warnings/unread-count` | `warning:view` | 未读数量 |
| POST | `/warnings/{id}/read` | `warning:view` | 标记单条已读 |
| POST | `/warnings/read-all` | `warning:view` | 全部已读 |
| POST | `/warnings/scan` | `warning:manage` | 手动触发扫描，返回各类预警数量 |

预警接收人：具备 `archive:review` 的用户、合同负责人、合同创建人。唯一键 `(biz_key, receiver_id)` 防止重复推送。

## 七、统计分析

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/stats/dashboard` | `stats:view` | 看板指标卡与图表数据 |
| GET | `/stats/overview` | `stats:view` | 按日期、部门、类型统计 |
| GET | `/stats/export` | `stats:export` | 导出统计报表及明细 Excel |

`/stats/overview` 参数：`startDate`、`endDate`（`yyyy-MM-dd`）、`deptId`、`contractType`。

## 八、档案台账

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/ledger/contracts` | `ledger:view` | 全量/分类台账，支持合同查询全部条件 |
| GET | `/ledger/export` | `ledger:export` | 导出台账 Excel |

分类通过 `category` 传入：`draft` 待归档、`reviewing` 审核中、`rejected` 已驳回、`archived` 已归档、`expired` 已到期；不传为全部。

## 九、系统管理

### 用户

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/users` | `system:user:list` | 用户分页 |
| POST | `/users` | `system:user:add` | 新增用户 |
| PUT | `/users` | `system:user:edit` | 编辑用户与角色 |
| PUT | `/users/{id}/password` | `system:user:edit` | 重置密码 |
| PUT | `/users/{id}/status?status=1` | `system:user:edit` | 启用/禁用 |
| DELETE | `/users/{id}` | `system:user:delete` | 逻辑删除 |

### 角色与权限

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/roles` | `system:role:list` | 角色列表 |
| GET | `/roles/permissions` | `system:role:list` | 菜单/按钮权限树 |
| POST | `/roles` | `system:role:edit` | 新增/编辑角色并绑定权限 |
| DELETE | `/roles/{id}` | `system:role:delete` | 删除角色 |

### 部门、字典、参数

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/depts/tree` | `system:dept:list` | 部门树 |
| POST | `/depts` | `system:dept:edit` | 新增/编辑部门（校验环形层级） |
| DELETE | `/depts/{id}` | `system:dept:delete` | 删除部门 |
| GET | `/dicts` | `system:dict:list` | 字典及字典项 |
| POST | `/dicts` | `system:dict:edit` | 新增/编辑字典 |
| DELETE | `/dicts/{id}` | `system:dict:delete` | 删除字典 |
| POST | `/dicts/items` | `system:dict:edit` | 新增/编辑字典项 |
| DELETE | `/dicts/items/{id}` | `system:dict:delete` | 删除字典项 |
| GET | `/configs` | `system:config:list` | 系统参数列表 |
| PUT | `/configs` | `system:config:edit` | 更新系统参数 |

### 操作日志与下拉选项

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/logs` | `system:log:list` | 日志分页；支持用户、模块、动作、状态、时间范围 |
| GET | `/options/users` | — | 负责人下拉 |
| GET | `/options/depts` | — | 部门下拉 |
| GET | `/options/dict/{code}` | — | 字典项下拉 |
| GET | `/options/roles` | — | 角色下拉 |

## 十、错误码约定

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 200 | 200 | 成功 |
| 400 | 400 | 参数校验失败、业务规则不满足、日期/JSON 格式错误 |
| 401 | 401 | 未登录、Token 失效 |
| 403 | 403 | 接口权限不足、数据越权、未借阅下载归档附件 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器异常，响应不返回堆栈 |

业务失败统一返回：

```json
{ "code": 400, "message": "请至少上传一份合同附件后再提交归档", "data": null }
```
