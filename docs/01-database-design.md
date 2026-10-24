# 数据库设计说明

- 数据库：`archive_contract`
- 字符集：`utf8mb4` / `utf8mb4_general_ci`
- 引擎：InnoDB
- 建表脚本：[`sql/01_schema.sql`](../sql/01_schema.sql)
- 初始化脚本：[`sql/02_init_data.sql`](../sql/02_init_data.sql)

## 一、执行顺序

```bash
mysql -uroot -p < sql/01_schema.sql
mysql -uroot -p < sql/02_init_data.sql
```

`01_schema.sql` 会自动创建数据库，并按依赖顺序删除、重建 17 张业务表，最后创建 6 个数据保护触发器。`02_init_data.sql` 负责写入部门、用户、角色、权限、字典、系统参数、演示合同及示例附件记录。

> 生产环境升级时不要重复执行 `01_schema.sql`，应先备份再执行增量脚本。

## 二、表清单

| 分类 | 表名 | 说明 | 关键字段/约束 |
| --- | --- | --- | --- |
| 组织 | `sys_dept` | 部门树 | `parent_id` 自关联；`uk_dept_code` |
| 用户 | `sys_user` | 用户账号 | `uk_user_username`；BCrypt 密码；`deleted` |
| 角色 | `sys_role` | 角色定义 | `role_code` 唯一；逻辑删除 |
| 关联 | `sys_user_role` | 用户-角色 | `(user_id, role_id)` 唯一 |
| 权限 | `sys_permission` | 菜单与按钮权限 | `perm_code` 唯一；`perm_type=MENU/BUTTON` |
| 关联 | `sys_role_permission` | 角色-权限 | `(role_id, permission_id)` 唯一 |
| 字典 | `sys_dict` | 字典定义 | `dict_code` 唯一 |
| 字典 | `sys_dict_item` | 字典项 | `(dict_id, item_value)` 唯一 |
| 参数 | `sys_config` | 系统参数 | `config_key` 唯一 |
| 合同 | `contract_info` | 合同主档 | `uk_contract_no`、`uk_archive_no`；`version` 乐观锁；`deleted` |
| 合同 | `contract_payment_node` | 付款节点 | `contract_id`、`plan_pay_date`、`paid_status` |
| 附件 | `contract_attachment` | 附件元数据 | 存储文件名、原文件名、路径、SHA-256、全文、逻辑删除 |
| 归档 | `archive_review_record` | 提交/审核历史 | `action=SUBMIT/APPROVE/REJECT`，不可删除 |
| 变更 | `archive_change_record` | 档案变更历史 | 变更前后 JSON 快照，不可删除 |
| 借阅 | `borrow_apply` | 借阅申请与台账 | 起止时间、状态、审批/归还信息 |
| 日志 | `sys_operation_log` | 操作审计 | 仅追加，数据库层禁止删除 |
| 预警 | `sys_warning` | 站内预警消息 | `(biz_key, receiver_id)` 唯一，幂等去重 |

## 三、核心关系

```text
sys_dept 1 ── N sys_user 1 ── N contract_info
                         └── N borrow_apply
sys_user N ── N sys_role (sys_user_role)
sys_role N ── N sys_permission (sys_role_permission)

contract_info 1 ── N contract_attachment
contract_info 1 ── N contract_payment_node
contract_info 1 ── N archive_review_record
contract_info 1 ── N archive_change_record
contract_info 1 ── N borrow_apply
contract_info 1 ── N sys_warning
```

## 四、状态字典

### 合同状态 `contract_info.status`

| 值 | 含义 | 允许操作 |
| --- | --- | --- |
| `DRAFT` | 草稿 | 编辑、删除、上传附件、提交归档 |
| `PENDING` | 审核中 | 审核通过/驳回，不可编辑 |
| `REJECTED` | 已驳回 | 编辑、补充附件、重新提交、删除 |
| `ARCHIVED` | 已归档 | 只读；变更需申请审批；借阅下载 |
| `EXPIRED` | 已到期 | 只读；由定时任务将超期已归档合同自动置为到期 |

### 变更状态 `archive_change_record.status`

`PENDING` 待审批、`APPROVED` 已通过、`REJECTED` 已驳回。同一合同同一时间只允许一条待审批变更。

### 借阅状态 `borrow_apply.status`

`PENDING` 待审批、`APPROVED` 已通过、`REJECTED` 已驳回、`RETURNED` 已归还、`EXPIRED` 已过期。只有审批通过且在借阅时间窗内的用户才能下载对应归档附件。

### 审核动作 `archive_review_record.action`

`SUBMIT` 提交归档、`APPROVE` 审核通过、`REJECT` 审核驳回。

## 五、关键索引

| 表 | 索引 | 用途 |
| --- | --- | --- |
| `contract_info` | `uk_archive_no` | 档案编号全局唯一 |
| `contract_info` | `idx_contract_status` / `idx_contract_expire` | 台账分类、到期扫描 |
| `contract_info` | `idx_contract_dept` / `idx_contract_owner` / `idx_contract_created` | 数据隔离与部门统计 |
| `contract_attachment` | `idx_attach_contract` | 详情页附件加载 |
| `borrow_apply` | `idx_borrow_status` / `idx_borrow_applicant` | 审批列表、我的借阅 |
| `sys_warning` | `uk_warning_biz_receiver` | 预警幂等去重 |
| `sys_warning` | `idx_warning_receiver (receiver_id, read_status)` | 未读角标与消息列表 |
| `sys_operation_log` | `idx_log_time` / `idx_log_user` / `idx_log_module` | 日志检索 |

## 六、数据保护触发器

脚本末尾创建以下触发器，作为应用层校验之外的最后防线：

| 触发器 | 作用 |
| --- | --- |
| `trg_log_forbid_delete` | 禁止删除 `sys_operation_log` |
| `trg_review_forbid_delete` | 禁止删除归档审核记录 |
| `trg_change_forbid_delete` | 禁止删除档案变更记录 |
| `trg_contract_forbid_delete` | 禁止物理删除已归档/已到期或已有档案号的合同 |
| `trg_contract_forbid_archive_delete` | 禁止把已归档合同置为逻辑删除 |
| `trg_attachment_forbid_delete` | 禁止物理删除附件，只允许逻辑删除 |

触发器只拦截 `DELETE` 和非法 `UPDATE`，不影响正常的业务更新。数据库账号不应拥有 `DROP TRIGGER` 权限。

## 七、附件存储设计

- 数据库只保存附件元数据和相对/绝对路径，文件本身存放在 `app.file.storage-path`（默认 `./data/uploads`）。
- 文件按 `yyyy/MM/dd/UUID.ext` 保存，原始文件名单独入库。
- 上传时做扩展名白名单、大小限制、文件名净化、路径穿越校验。
- 上传后计算 SHA-256，可校验文件完整性；PDF/Word 抽取文本存入 `content_text` 供附件全文检索。
- 删除附件走 MyBatis-Plus 逻辑删除，物理文件保留，避免误删和审计断链。
- 备份时必须同时备份数据库与附件目录，两者缺一不可。

## 八、初始化数据说明

- 4 个部门层级示例、4 个演示用户。
- 4 个角色：`ADMIN`、`ARCHIVIST`、`ENTRY`、`VIEWER`。
- 菜单/按钮权限约 40 项，覆盖合同、归档、变更、借阅、预警、统计、台账、系统管理。
- 字典：合同类型、合同状态、密级、附件类型等。
- 系统参数：附件大小、允许扩展名、预警提前天数等。
- 演示合同、审核记录与借阅记录，用于快速体验统计看板、台账与权限隔离。演示合同未预置附件文件，体验归档审核前请先上传附件。

演示账号与密码见项目根目录 [`README.md`](../README.md)。
