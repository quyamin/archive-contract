-- =====================================================================
-- 企业档案合同资料管理分析系统 V1.0
-- 初始化基础数据（请在 01_schema.sql 之后执行）
-- 演示账号密码见项目 README.md
-- =====================================================================
USE archive_contract;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 组织机构
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_dept (id, parent_id, dept_code, dept_name, leader, phone, sort, status) VALUES
(1, NULL, 'D001', '总经办', '张总', '010-88001001', 1, 1),
(2, 1, 'D002', '法务部', '李律师', '010-88001002', 2, 1),
(3, 1, 'D003', '行政部', '王经理', '010-88001003', 3, 1),
(4, 1, 'D004', '采购部', '赵经理', '010-88001004', 4, 1),
(5, 1, 'D005', '销售部', '陈经理', '010-88001005', 5, 1),
(6, 1, 'D006', '财务部', '周会计', '010-88001006', 6, 1),
(7, 3, 'D007', '档案室', '孙老师', '010-88001007', 7, 1);

-- ---------------------------------------------------------------------
-- 用户（BCrypt 加密，演示密码见 README）
-- admin123 / archive123 / entry123 / viewer123
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_user (id, username, password, real_name, phone, email, dept_id, status, remark) VALUES
(1, 'admin', '$2b$10$uxfiPPqWj0qlS4epSwr3YOcSU38yv1WQkehY44ZPdxcQMAtlZUXMK', '系统管理员', '13800000001', 'admin@example.com', 1, 1, '系统超级管理员'),
(2, 'archivist', '$2b$10$wf7Hr/ZhmZZVxJaQevIU8.1nVcpGQQ8IYPbYWL2AA1Zn.PyqV.Sja', '孙档案', '13800000002', 'archive@example.com', 7, 1, '档案管理员'),
(3, 'entry', '$2b$10$fU6rpzkTwpTymojDQoMmku61HYCg6YLTgXcTDV.F6ZwXqipcX67V6', '赵业务', '13800000003', 'entry@example.com', 4, 1, '业务录入员'),
(4, 'viewer', '$2b$10$fK5roB4tZj0IPyrHnJY3/uUrrUMns1CsRjmE0J9YqB4N.PMeiPoCy', '李查阅', '13800000004', 'viewer@example.com', 2, 1, '普通查阅人员');

-- ---------------------------------------------------------------------
-- 角色
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_role (id, role_code, role_name, description, status) VALUES
(1, 'ADMIN', '管理员', '系统超级管理员，拥有全部菜单、按钮与数据权限', 1),
(2, 'ARCHIVIST', '档案管理员', '负责归档审核、档案变更审批、借阅审批、预警与统计分析', 1),
(3, 'ENTRY', '业务录入员', '负责合同录入、附件上传、提交归档及本人部门数据查看', 1),
(4, 'VIEWER', '普通查阅人员', '可查阅已归档合同、发起借阅申请，借阅审批后方可下载原件', 1);

INSERT IGNORE INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1), (2, 2, 2), (3, 3, 3), (4, 4, 4);

-- ---------------------------------------------------------------------
-- 权限（菜单 + 按钮级）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, icon, sort) VALUES
(1, NULL, '系统管理', 'menu:system', 'MENU', NULL, 'Setting', 90),
(2, NULL, '合同档案', 'menu:contract', 'MENU', NULL, 'Document', 10),
(3, NULL, '归档管理', 'menu:archive', 'MENU', NULL, 'Files', 20),
(4, NULL, '借阅管理', 'menu:borrow', 'MENU', NULL, 'Reading', 30),
(5, NULL, '风险预警', 'menu:warning', 'MENU', NULL, 'Warning', 40),
(6, NULL, '数据分析', 'menu:stats', 'MENU', NULL, 'DataAnalysis', 50),
(7, NULL, '档案台账', 'menu:ledger', 'MENU', NULL, 'Collection', 60),

(11, 1, '用户管理', 'system:user:list', 'MENU', '/system/users', 'User', 1),
(12, 11, '新增用户', 'system:user:add', 'BUTTON', NULL, NULL, 2),
(13, 11, '编辑用户', 'system:user:edit', 'BUTTON', NULL, NULL, 3),
(14, 11, '删除用户', 'system:user:delete', 'BUTTON', NULL, NULL, 4),
(21, 1, '角色权限', 'system:role:list', 'MENU', '/system/roles', 'Avatar', 5),
(22, 21, '编辑角色', 'system:role:edit', 'BUTTON', NULL, NULL, 6),
(23, 21, '删除角色', 'system:role:delete', 'BUTTON', NULL, NULL, 7),
(31, 1, '组织机构', 'system:dept:list', 'MENU', '/system/depts', 'OfficeBuilding', 8),
(32, 31, '编辑部门', 'system:dept:edit', 'BUTTON', NULL, NULL, 9),
(33, 31, '删除部门', 'system:dept:delete', 'BUTTON', NULL, NULL, 10),
(41, 1, '字典配置', 'system:dict:list', 'MENU', '/system/dicts', 'Notebook', 11),
(42, 41, '编辑字典', 'system:dict:edit', 'BUTTON', NULL, NULL, 12),
(43, 41, '删除字典', 'system:dict:delete', 'BUTTON', NULL, NULL, 13),
(51, 1, '系统参数', 'system:config:list', 'MENU', '/system/config', 'Tools', 14),
(52, 51, '编辑参数', 'system:config:edit', 'BUTTON', NULL, NULL, 15),
(61, 1, '操作日志', 'system:log:list', 'MENU', '/system/logs', 'Tickets', 16),

(101, 2, '合同台账', 'contract:list', 'MENU', '/contracts', 'Document', 1),
(102, 101, '录入合同', 'contract:add', 'BUTTON', NULL, NULL, 2),
(103, 101, '编辑合同', 'contract:edit', 'BUTTON', NULL, NULL, 3),
(104, 101, '删除草稿', 'contract:delete', 'BUTTON', NULL, NULL, 4),
(105, 101, '查看详情', 'contract:view', 'BUTTON', NULL, NULL, 5),
(106, 101, '提交归档', 'contract:submit', 'BUTTON', NULL, NULL, 6),
(107, 101, '批量导入', 'contract:import', 'BUTTON', NULL, NULL, 7),
(108, 101, '导出台账', 'contract:export', 'BUTTON', NULL, NULL, 8),
(109, 101, '下载附件', 'contract:download', 'BUTTON', NULL, NULL, 9),
(110, 101, '查看全部数据', 'contract:view:all', 'BUTTON', NULL, NULL, 10),
(111, 101, '上传附件', 'attachment:upload', 'BUTTON', NULL, NULL, 11),
(112, 101, '预览附件', 'attachment:preview', 'BUTTON', NULL, NULL, 12),
(113, 101, '删除附件', 'attachment:delete', 'BUTTON', NULL, NULL, 13),

(201, 3, '归档审核', 'archive:review', 'MENU', '/archive/review', 'Checked', 1),
(202, 3, '档案变更', 'change:apply', 'MENU', '/archive/changes', 'EditPen', 2),
(203, 202, '变更审批', 'change:review', 'BUTTON', NULL, NULL, 3),
(204, 201, '下载任意档案', 'archive:download:any', 'BUTTON', NULL, NULL, 4),

(301, 4, '我的借阅', 'borrow:apply', 'MENU', '/borrow/my', 'Reading', 1),
(302, 4, '借阅审批', 'borrow:approve', 'MENU', '/borrow/approval', 'Stamp', 2),

(401, 5, '预警列表', 'warning:view', 'MENU', '/warnings', 'Warning', 1),
(402, 401, '手动预警扫描', 'warning:manage', 'BUTTON', NULL, NULL, 2),

(501, 6, '统计分析与看板', 'stats:view', 'MENU', '/dashboard', 'DataAnalysis', 1),
(502, 501, '导出报表', 'stats:export', 'BUTTON', NULL, NULL, 2),

(601, 7, '档案总台账', 'ledger:view', 'MENU', '/ledger', 'Collection', 1),
(602, 601, '导出台账', 'ledger:export', 'BUTTON', NULL, NULL, 2),
(603, 601, '打印台账', 'ledger:print', 'BUTTON', NULL, NULL, 3);

-- 管理员：全部权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 档案管理员
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),
(2,101),(2,105),(2,108),(2,109),(2,110),(2,112),
(2,201),(2,202),(2,203),(2,204),
(2,301),(2,302),
(2,401),(2,402),
(2,501),(2,502),
(2,601),(2,602),(2,603);

-- 业务录入员
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(3,2),(3,3),(3,4),(3,5),(3,7),
(3,101),(3,102),(3,103),(3,104),(3,105),(3,106),(3,107),(3,108),(3,109),
(3,111),(3,112),(3,113),
(3,202),
(3,301),
(3,401),
(3,601),(3,602),(3,603);

-- 普通查阅人员
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(4,2),(4,4),(4,5),(4,7),
(4,101),(4,105),(4,109),(4,112),
(4,301),
(4,401),
(4,601),(4,603);

-- ---------------------------------------------------------------------
-- 数据字典
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_dict (id, dict_code, dict_name, description, status) VALUES
(1, 'contract_type', '合同类型', '合同业务类型字典', 1),
(2, 'contract_status', '合同状态', '合同全生命周期状态', 1),
(3, 'security_level', '档案密级', '档案资料保密等级', 1),
(4, 'archive_status', '归档状态', '档案归档状态字典', 1),
(5, 'file_type', '附件类型', '合同附件分类', 1),
(6, 'borrow_status', '借阅状态', '档案借阅审批状态', 1),
(7, 'change_status', '变更状态', '档案变更审批状态', 1),
(8, 'change_type', '变更类型', '档案变更业务分类', 1);

INSERT IGNORE INTO sys_dict_item (id, dict_id, item_label, item_value, sort, status) VALUES
(1, 1, '采购合同', 'PURCHASE', 1, 1),
(2, 1, '销售合同', 'SALES', 2, 1),
(3, 1, '服务合同', 'SERVICE', 3, 1),
(4, 1, '租赁合同', 'LEASE', 4, 1),
(5, 1, '劳务合同', 'LABOR', 5, 1),
(6, 1, '其他合同', 'OTHER', 6, 1),

(11, 2, '待归档', 'DRAFT', 1, 1),
(12, 2, '审核中', 'PENDING', 2, 1),
(13, 2, '已驳回', 'REJECTED', 3, 1),
(14, 2, '已归档', 'ARCHIVED', 4, 1),
(15, 2, '已到期', 'EXPIRED', 5, 1),

(21, 3, '公开', 'PUBLIC', 1, 1),
(22, 3, '内部', 'INTERNAL', 2, 1),
(23, 3, '秘密', 'SECRET', 3, 1),
(24, 3, '机密', 'CONFIDENTIAL', 4, 1),

(31, 4, '待归档', 'PENDING', 1, 1),
(32, 4, '审核中', 'REVIEWING', 2, 1),
(33, 4, '已驳回', 'REJECTED', 3, 1),
(34, 4, '已归档', 'ARCHIVED', 4, 1),
(35, 4, '已到期', 'EXPIRED', 5, 1),

(41, 5, '合同原件', 'ORIGINAL', 1, 1),
(42, 5, '补充协议', 'SUPPLEMENT', 2, 1),
(43, 5, '审批单', 'APPROVAL', 3, 1),
(44, 5, '验收材料', 'ACCEPTANCE', 4, 1),
(45, 5, '其他附件', 'OTHER', 5, 1),

(51, 6, '待审批', 'PENDING', 1, 1),
(52, 6, '已通过', 'APPROVED', 2, 1),
(53, 6, '已驳回', 'REJECTED', 3, 1),
(54, 6, '已归还', 'RETURNED', 4, 1),
(55, 6, '已到期', 'EXPIRED', 5, 1),

(61, 7, '待审批', 'PENDING', 1, 1),
(62, 7, '已通过', 'APPROVED', 2, 1),
(63, 7, '已驳回', 'REJECTED', 3, 1),

(71, 8, '基本信息变更', 'INFO', 1, 1),
(72, 8, '金额变更', 'AMOUNT', 2, 1),
(73, 8, '期限变更', 'DATE', 3, 1),
(74, 8, '其他变更', 'OTHER', 4, 1);

-- ---------------------------------------------------------------------
-- 系统参数
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_config (id, config_key, config_value, config_name, config_group, remark) VALUES
(1, 'system.name', '企业档案合同资料管理分析系统', '系统名称', '基础配置', '显示在系统标题栏'),
(2, 'file.storage-path', './data/uploads', '文件存储路径', '文件存储', '支持相对路径或绝对路径，修改后新文件保存至新路径'),
(3, 'file.max-size-mb', '50', '附件大小限制(MB)', '文件存储', '单个附件最大体积'),
(4, 'file.allowed-ext', 'pdf,doc,docx,xls,xlsx,jpg,jpeg,png,gif', '允许的附件格式', '文件存储', '英文逗号分隔'),
(5, 'warning.advance-days', '30', '预警提前天数', '风险预警', '合同到期与付款节点提前提醒天数'),
(6, 'borrow.max-days', '90', '单次借阅最长天数', '借阅管理', '档案借阅最长期限');

-- ---------------------------------------------------------------------
-- 演示合同数据（日期相对当前日期，便于直接查看预警效果）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO contract_info
(id, contract_no, contract_name, party_a, party_b, sign_date, effective_date, expire_date, amount,
 contract_type, dept_id, owner_id, security_level, status, archive_no, archive_time, archive_by,
 remark, version, deleted, create_by, create_time) VALUES
(1, 'HT-2026-0001', '2026年度办公用品采购合同', '星瀚科技有限公司', '华信办公用品有限公司',
 DATE_SUB(CURDATE(), INTERVAL 300 DAY), DATE_SUB(CURDATE(), INTERVAL 290 DAY), DATE_ADD(CURDATE(), INTERVAL 18 DAY),
 350000.00, 'PURCHASE', 4, 3, 'INTERNAL', 'ARCHIVED', 'DA202601010001', DATE_SUB(NOW(), INTERVAL 280 DAY), 2,
 '年度办公用品集中采购，含办公耗材与打印设备', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 310 DAY)),
(2, 'HT-2026-0002', '云服务平台年度服务合同', '星瀚科技有限公司', '云启网络科技有限公司',
 DATE_SUB(CURDATE(), INTERVAL 180 DAY), DATE_SUB(CURDATE(), INTERVAL 170 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY),
 880000.00, 'SERVICE', 1, 1, 'INTERNAL', 'ARCHIVED', 'DA202604150001', DATE_SUB(NOW(), INTERVAL 160 DAY), 2,
 '企业云资源与运维服务年度采购', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 190 DAY)),
(3, 'HT-2025-0088', '软件系统维保服务合同', '星瀚科技有限公司', '恒信软件股份有限公司',
 DATE_SUB(CURDATE(), INTERVAL 420 DAY), DATE_SUB(CURDATE(), INTERVAL 410 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY),
 128000.00, 'SERVICE', 1, 1, 'INTERNAL', 'EXPIRED', 'DA202509010001', DATE_SUB(NOW(), INTERVAL 400 DAY), 2,
 '核心业务系统年度维保，已到期待续签', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 430 DAY)),
(4, 'HT-2026-0004', '智能设备销售框架合同', '星瀚科技有限公司', '华东智能设备有限公司',
 DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 110 DAY), DATE_ADD(CURDATE(), INTERVAL 240 DAY),
 2600000.00, 'SALES', 5, 3, 'INTERNAL', 'ARCHIVED', 'DA202606180001', DATE_SUB(NOW(), INTERVAL 100 DAY), 2,
 '智能终端设备年度销售框架协议', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 130 DAY)),
(5, 'HT-2026-0005', '常年法律顾问服务合同', '星瀚科技有限公司', '北京明理律师事务所',
 DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 335 DAY),
 120000.00, 'SERVICE', 2, 4, 'SECRET', 'PENDING', NULL, NULL, NULL,
 '年度法律顾问服务，待档案管理员审核', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 'HT-2026-0006', '研发测试设备租赁合同', '星瀚科技有限公司', '中科测试设备租赁有限公司',
 DATE_SUB(CURDATE(), INTERVAL 5 DAY), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 360 DAY),
 460000.00, 'LEASE', 4, 3, 'INTERNAL', 'DRAFT', NULL, NULL, NULL,
 '研发实验室测试设备租赁，草稿待完善', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 'HT-2026-0007', '品牌市场推广服务合同', '星瀚科技有限公司', '创想文化传媒有限公司',
 DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 50 DAY), DATE_ADD(CURDATE(), INTERVAL 120 DAY),
 480000.00, 'SERVICE', 5, 3, 'INTERNAL', 'REJECTED', NULL, NULL, NULL,
 '年度品牌推广服务，资料待补充', 0, 0, 3, DATE_SUB(NOW(), INTERVAL 65 DAY)),
(8, 'HT-2025-0066', '2025年度劳务派遣服务合同', '星瀚科技有限公司', '人和人力资源服务有限公司',
 DATE_SUB(CURDATE(), INTERVAL 500 DAY), DATE_SUB(CURDATE(), INTERVAL 490 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY),
 750000.00, 'LABOR', 3, 2, 'INTERNAL', 'ARCHIVED', 'DA202508120001', DATE_SUB(NOW(), INTERVAL 480 DAY), 2,
 '行政辅助岗位劳务派遣服务', 0, 0, 2, DATE_SUB(NOW(), INTERVAL 510 DAY)),
(9, 'HT-2026-0009', '园区安保服务合同', '星瀚科技有限公司', '鼎卫保安服务有限公司',
 DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_SUB(CURDATE(), INTERVAL 140 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
 260000.00, 'SERVICE', 3, 2, 'SECRET', 'ARCHIVED', 'DA202601200001', DATE_SUB(NOW(), INTERVAL 130 DAY), 2,
 '总部园区安保值守及巡逻服务', 0, 0, 2, DATE_SUB(NOW(), INTERVAL 160 DAY)),
(10, 'HT-2024-0101', '2024年度财务审计服务合同', '星瀚科技有限公司', '立诚会计师事务所',
 DATE_SUB(CURDATE(), INTERVAL 700 DAY), DATE_SUB(CURDATE(), INTERVAL 690 DAY), DATE_SUB(CURDATE(), INTERVAL 330 DAY),
 95000.00, 'SERVICE', 6, 2, 'INTERNAL', 'EXPIRED', 'DA202411180001', DATE_SUB(NOW(), INTERVAL 680 DAY), 2,
 '年度财务报表审计服务，已到期', 0, 0, 2, DATE_SUB(NOW(), INTERVAL 710 DAY));

-- 付款节点
INSERT IGNORE INTO contract_payment_node
(id, contract_id, node_name, plan_pay_date, amount, paid_status, remark, deleted) VALUES
(1, 1, '预付款（30%）', DATE_SUB(CURDATE(), INTERVAL 260 DAY), 105000.00, 1, '已支付', 0),
(2, 1, '到货款（60%）', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 210000.00, 0, '货物验收后支付', 0),
(3, 1, '质保尾款（10%）', DATE_ADD(CURDATE(), INTERVAL 18 DAY), 35000.00, 0, '质保期结束支付', 0),
(4, 2, '年度服务费', DATE_ADD(CURDATE(), INTERVAL 12 DAY), 880000.00, 0, '年度一次性支付', 0),
(5, 9, '半年度服务费', DATE_ADD(CURDATE(), INTERVAL 5 DAY), 130000.00, 0, '半年服务费结算', 0);

-- 审核记录
INSERT IGNORE INTO archive_review_record
(id, contract_id, action, opinion, reviewer_id, review_time, archive_no) VALUES
(1, 1, 'SUBMIT', '资料已齐备，提交归档审核', 3, DATE_SUB(NOW(), INTERVAL 285 DAY), NULL),
(2, 1, 'APPROVE', '合同信息与附件核验无误，同意归档', 2, DATE_SUB(NOW(), INTERVAL 280 DAY), 'DA202601010001'),
(3, 2, 'SUBMIT', '云服务合同提交归档', 3, DATE_SUB(NOW(), INTERVAL 165 DAY), NULL),
(4, 2, 'APPROVE', '审核通过，正式归档', 2, DATE_SUB(NOW(), INTERVAL 160 DAY), 'DA202604150001'),
(5, 3, 'SUBMIT', '维保合同提交归档', 3, DATE_SUB(NOW(), INTERVAL 405 DAY), NULL),
(6, 3, 'APPROVE', '审核通过', 2, DATE_SUB(NOW(), INTERVAL 400 DAY), 'DA202509010001'),
(7, 4, 'SUBMIT', '销售框架合同提交归档', 3, DATE_SUB(NOW(), INTERVAL 105 DAY), NULL),
(8, 4, 'APPROVE', '审核通过，正式归档', 2, DATE_SUB(NOW(), INTERVAL 100 DAY), 'DA202606180001'),
(9, 5, 'SUBMIT', '法律顾问合同提交归档', 3, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(10, 7, 'SUBMIT', '市场推广合同提交归档', 3, DATE_SUB(NOW(), INTERVAL 40 DAY), NULL),
(11, 7, 'REJECT', '缺少合同审批单与验收材料，请补充后重新提交', 2, DATE_SUB(NOW(), INTERVAL 38 DAY), NULL),
(12, 8, 'SUBMIT', '劳务派遣合同提交归档', 2, DATE_SUB(NOW(), INTERVAL 485 DAY), NULL),
(13, 8, 'APPROVE', '审核通过，正式归档', 2, DATE_SUB(NOW(), INTERVAL 480 DAY), 'DA202508120001'),
(14, 9, 'SUBMIT', '安保服务合同提交归档', 2, DATE_SUB(NOW(), INTERVAL 135 DAY), NULL),
(15, 9, 'APPROVE', '审核通过，正式归档', 2, DATE_SUB(NOW(), INTERVAL 130 DAY), 'DA202601200001'),
(16, 10, 'SUBMIT', '财务审计合同提交归档', 2, DATE_SUB(NOW(), INTERVAL 685 DAY), NULL),
(17, 10, 'APPROVE', '审核通过，正式归档', 2, DATE_SUB(NOW(), INTERVAL 680 DAY), 'DA202411180001');

-- 借阅演示数据
INSERT IGNORE INTO borrow_apply
(id, borrow_no, contract_id, applicant_id, purpose, start_time, end_time, status,
 approver_id, approve_opinion, approve_time, return_time, deleted, create_time) VALUES
(1, 'JY202600000001', 2, 4, '法务合规抽查云服务合同条款', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY),
 'APPROVED', 2, '同意借阅，请按期归还', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'JY202600000002', 1, 3, '年度采购复盘，核对付款节点', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY),
 'PENDING', NULL, NULL, NULL, NULL, 0, NOW());

-- 注意：演示合同未预置附件文件。首次演示请使用业务录入员账号上传 PDF/Word 后再提交归档；
-- 普通查阅人员 viewer 对合同 2 已有生效中的借阅记录，上传附件后可直接验证下载权限控制。
