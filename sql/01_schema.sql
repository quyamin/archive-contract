-- =====================================================================
-- 企业档案合同资料管理分析系统 V1.0
-- MySQL 8.0 建表脚本
-- 数据库：archive_contract
-- =====================================================================
CREATE DATABASE IF NOT EXISTS archive_contract
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE archive_contract;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_warning;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS borrow_apply;
DROP TABLE IF EXISTS archive_change_record;
DROP TABLE IF EXISTS archive_review_record;
DROP TABLE IF EXISTS contract_attachment;
DROP TABLE IF EXISTS contract_payment_node;
DROP TABLE IF EXISTS contract_info;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 组织机构
-- ---------------------------------------------------------------------
CREATE TABLE sys_dept (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  parent_id   BIGINT       NULL COMMENT '上级部门ID，顶级部门为空',
  dept_code   VARCHAR(50)  NOT NULL COMMENT '部门编码',
  dept_name   VARCHAR(100) NOT NULL COMMENT '部门名称',
  leader      VARCHAR(50)  NULL COMMENT '部门负责人',
  phone       VARCHAR(30)  NULL COMMENT '联系电话',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dept_code (dept_code),
  KEY idx_dept_parent (parent_id),
  CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES sys_dept (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构表';

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username        VARCHAR(50)  NOT NULL COMMENT '登录账号',
  password        VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  real_name       VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  phone           VARCHAR(30)  NULL COMMENT '手机号',
  email           VARCHAR(100) NULL COMMENT '邮箱',
  dept_id         BIGINT       NULL COMMENT '所属部门ID',
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  last_login_time DATETIME     NULL COMMENT '最后登录时间',
  remark          VARCHAR(255) NULL COMMENT '备注',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_dept (dept_id),
  CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES sys_dept (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 角色
-- ---------------------------------------------------------------------
CREATE TABLE sys_role (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
  role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
  description VARCHAR(255) NULL COMMENT '角色描述',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_user_role (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_ur_role (role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ---------------------------------------------------------------------
-- 权限（菜单 + 按钮）
-- ---------------------------------------------------------------------
CREATE TABLE sys_permission (
  id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  parent_id BIGINT       NULL COMMENT '父权限ID',
  perm_name VARCHAR(50)  NOT NULL COMMENT '权限名称',
  perm_code VARCHAR(80)  NULL COMMENT '权限编码（菜单/按钮）',
  perm_type VARCHAR(10)  NOT NULL DEFAULT 'BUTTON' COMMENT '类型：MENU/BUTTON',
  path      VARCHAR(120) NULL COMMENT '前端路由',
  icon      VARCHAR(50)  NULL COMMENT '图标',
  sort      INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (perm_code),
  KEY idx_perm_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE sys_role_permission (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  role_id       BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (role_id, permission_id),
  KEY idx_rp_perm (permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ---------------------------------------------------------------------
-- 数据字典
-- ---------------------------------------------------------------------
CREATE TABLE sys_dict (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  dict_code   VARCHAR(50)  NOT NULL COMMENT '字典编码',
  dict_name   VARCHAR(100) NOT NULL COMMENT '字典名称',
  description VARCHAR(255) NULL COMMENT '字典说明',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

CREATE TABLE sys_dict_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典项ID',
  dict_id     BIGINT       NOT NULL COMMENT '字典ID',
  item_label  VARCHAR(100) NOT NULL COMMENT '显示名称',
  item_value  VARCHAR(50)  NOT NULL COMMENT '字典值',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_item_value (dict_id, item_value),
  CONSTRAINT fk_dict_item_dict FOREIGN KEY (dict_id) REFERENCES sys_dict (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项表';

-- ---------------------------------------------------------------------
-- 系统参数
-- ---------------------------------------------------------------------
CREATE TABLE sys_config (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '参数ID',
  config_key   VARCHAR(100) NOT NULL COMMENT '参数键',
  config_value VARCHAR(500) NOT NULL COMMENT '参数值',
  config_name  VARCHAR(100) NOT NULL COMMENT '参数名称',
  config_group VARCHAR(50)  NOT NULL DEFAULT '通用' COMMENT '参数分组',
  remark       VARCHAR(255) NULL COMMENT '说明',
  update_by    BIGINT       NULL COMMENT '最后修改人',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- ---------------------------------------------------------------------
-- 合同主信息
-- ---------------------------------------------------------------------
CREATE TABLE contract_info (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  contract_no    VARCHAR(64)   NOT NULL COMMENT '合同编号',
  contract_name  VARCHAR(255)  NOT NULL COMMENT '合同名称',
  party_a        VARCHAR(255)  NOT NULL COMMENT '甲方',
  party_b        VARCHAR(255)  NOT NULL COMMENT '乙方（合作单位）',
  sign_date      DATE          NOT NULL COMMENT '签订日期',
  effective_date DATE          NULL COMMENT '生效日期',
  expire_date    DATE          NULL COMMENT '到期日期',
  amount         DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '合同金额（元）',
  contract_type  VARCHAR(50)   NOT NULL COMMENT '合同类型（字典contract_type）',
  dept_id        BIGINT        NOT NULL COMMENT '归属部门',
  owner_id       BIGINT        NOT NULL COMMENT '合同负责人',
  security_level VARCHAR(30)   NOT NULL COMMENT '档案密级（字典security_level）',
  status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING/REJECTED/ARCHIVED/EXPIRED',
  archive_no     VARCHAR(64)   NULL COMMENT '正式档案归档编号',
  archive_time   DATETIME      NULL COMMENT '归档时间',
  archive_by     BIGINT        NULL COMMENT '归档操作人',
  remark         VARCHAR(1000) NULL COMMENT '备注',
  version        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  create_by      BIGINT        NOT NULL COMMENT '创建人',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by      BIGINT        NULL COMMENT '修改人',
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_no (contract_no),
  UNIQUE KEY uk_archive_no (archive_no),
  KEY idx_contract_name (contract_name),
  KEY idx_contract_dept (dept_id),
  KEY idx_contract_owner (owner_id),
  KEY idx_contract_status (status),
  KEY idx_contract_sign (sign_date),
  KEY idx_contract_expire (expire_date),
  KEY idx_contract_type (contract_type),
  KEY idx_contract_created (create_by),
  CONSTRAINT fk_contract_dept FOREIGN KEY (dept_id) REFERENCES sys_dept (id) ON DELETE RESTRICT,
  CONSTRAINT fk_contract_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_contract_archive_by FOREIGN KEY (archive_by) REFERENCES sys_user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_contract_create_by FOREIGN KEY (create_by) REFERENCES sys_user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_contract_update_by FOREIGN KEY (update_by) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同主信息表';

-- ---------------------------------------------------------------------
-- 合同付款节点
-- ---------------------------------------------------------------------
CREATE TABLE contract_payment_node (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '付款节点ID',
  contract_id   BIGINT        NOT NULL COMMENT '合同ID',
  node_name     VARCHAR(100)  NOT NULL COMMENT '节点名称',
  plan_pay_date DATE          NOT NULL COMMENT '计划付款日期',
  amount        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '计划付款金额',
  paid_status   TINYINT       NOT NULL DEFAULT 0 COMMENT '付款状态：0未付 1已付',
  remark        VARCHAR(255)  NULL COMMENT '备注',
  deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_pay_contract (contract_id),
  KEY idx_pay_date (plan_pay_date),
  CONSTRAINT fk_pay_contract FOREIGN KEY (contract_id) REFERENCES contract_info (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同付款节点表';

-- ---------------------------------------------------------------------
-- 合同附件
-- ---------------------------------------------------------------------
CREATE TABLE contract_attachment (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  contract_id   BIGINT       NOT NULL COMMENT '合同ID',
  file_name     VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID）',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_path     VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  file_ext      VARCHAR(20)  NOT NULL COMMENT '扩展名',
  file_size     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  mime_type     VARCHAR(150) NULL COMMENT 'MIME类型',
  file_type     VARCHAR(30)  NOT NULL DEFAULT 'OTHER' COMMENT '附件类型：ORIGINAL/SUPPLEMENT/APPROVAL/ACCEPTANCE/OTHER',
  checksum      VARCHAR(64)  NULL COMMENT 'SHA-256校验值',
  content_text  LONGTEXT     NULL COMMENT '抽取的文本内容，用于附件全文检索',
  uploader_id   BIGINT       NOT NULL COMMENT '上传人',
  upload_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_attach_contract (contract_id),
  KEY idx_attach_uploader (uploader_id),
  CONSTRAINT fk_attach_contract FOREIGN KEY (contract_id) REFERENCES contract_info (id) ON DELETE RESTRICT,
  CONSTRAINT fk_attach_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同附件表';

-- ---------------------------------------------------------------------
-- 归档审核记录
-- ---------------------------------------------------------------------
CREATE TABLE archive_review_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  contract_id BIGINT       NOT NULL COMMENT '合同ID',
  action      VARCHAR(20)  NOT NULL COMMENT '动作：SUBMIT/APPROVE/REJECT',
  opinion     VARCHAR(1000) NULL COMMENT '审核意见',
  reviewer_id BIGINT       NOT NULL COMMENT '操作人（提交人或审核人）',
  review_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  archive_no  VARCHAR(64)  NULL COMMENT '审核通过时生成的归档编号',
  PRIMARY KEY (id),
  KEY idx_review_contract (contract_id),
  KEY idx_review_user (reviewer_id),
  CONSTRAINT fk_review_contract FOREIGN KEY (contract_id) REFERENCES contract_info (id) ON DELETE RESTRICT,
  CONSTRAINT fk_review_user FOREIGN KEY (reviewer_id) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档审核记录表';

-- ---------------------------------------------------------------------
-- 档案变更记录
-- ---------------------------------------------------------------------
CREATE TABLE archive_change_record (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '变更ID',
  contract_id     BIGINT       NOT NULL COMMENT '合同ID',
  change_type     VARCHAR(30)  NOT NULL COMMENT '变更类型',
  change_reason   VARCHAR(1000) NOT NULL COMMENT '变更原因',
  before_content  LONGTEXT     NULL COMMENT '变更前内容JSON快照',
  after_content   LONGTEXT     NULL COMMENT '变更后内容JSON快照',
  status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  applicant_id    BIGINT       NOT NULL COMMENT '申请人',
  apply_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  approver_id     BIGINT       NULL COMMENT '审批人',
  approve_time    DATETIME     NULL COMMENT '审批时间',
  approve_opinion VARCHAR(1000) NULL COMMENT '审批意见',
  PRIMARY KEY (id),
  KEY idx_change_contract (contract_id),
  KEY idx_change_status (status),
  CONSTRAINT fk_change_contract FOREIGN KEY (contract_id) REFERENCES contract_info (id) ON DELETE RESTRICT,
  CONSTRAINT fk_change_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_change_approver FOREIGN KEY (approver_id) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档案变更记录表';

-- ---------------------------------------------------------------------
-- 借阅申请
-- ---------------------------------------------------------------------
CREATE TABLE borrow_apply (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '借阅ID',
  borrow_no       VARCHAR(64)  NOT NULL COMMENT '借阅单号',
  contract_id     BIGINT       NOT NULL COMMENT '合同ID',
  applicant_id    BIGINT       NOT NULL COMMENT '申请人',
  purpose         VARCHAR(500) NOT NULL COMMENT '借阅用途',
  start_time      DATETIME     NOT NULL COMMENT '借阅开始时间',
  end_time        DATETIME     NOT NULL COMMENT '借阅结束时间',
  status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/RETURNED/EXPIRED',
  approver_id     BIGINT       NULL COMMENT '审批人',
  approve_opinion VARCHAR(1000) NULL COMMENT '审批意见',
  approve_time    DATETIME     NULL COMMENT '审批时间',
  return_time     DATETIME     NULL COMMENT '归还时间',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_borrow_no (borrow_no),
  KEY idx_borrow_contract (contract_id),
  KEY idx_borrow_applicant (applicant_id),
  KEY idx_borrow_status (status),
  CONSTRAINT fk_borrow_contract FOREIGN KEY (contract_id) REFERENCES contract_info (id) ON DELETE RESTRICT,
  CONSTRAINT fk_borrow_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
  CONSTRAINT fk_borrow_approver FOREIGN KEY (approver_id) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅申请表';

-- ---------------------------------------------------------------------
-- 系统操作日志（不可删除）
-- ---------------------------------------------------------------------
CREATE TABLE sys_operation_log (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  user_id        BIGINT        NULL COMMENT '操作用户ID',
  username       VARCHAR(50)   NULL COMMENT '操作用户',
  module         VARCHAR(50)   NOT NULL COMMENT '业务模块',
  operation      VARCHAR(100)  NOT NULL COMMENT '操作动作',
  request_method VARCHAR(10)   NULL COMMENT 'HTTP方法',
  request_uri    VARCHAR(255)  NULL COMMENT '请求地址',
  request_params VARCHAR(4000) NULL COMMENT '请求参数（密码已脱敏）',
  ip             VARCHAR(64)   NULL COMMENT '客户端IP',
  status         VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT '结果：SUCCESS/FAIL',
  error_msg      VARCHAR(1000) NULL COMMENT '错误信息',
  duration_ms    BIGINT        NULL COMMENT '耗时毫秒',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_log_user (user_id),
  KEY idx_log_time (create_time),
  KEY idx_log_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ---------------------------------------------------------------------
-- 预警信息（站内消息）
-- ---------------------------------------------------------------------
CREATE TABLE sys_warning (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  contract_id  BIGINT       NOT NULL COMMENT '合同ID',
  warning_type VARCHAR(20)  NOT NULL COMMENT '类型：EXPIRY到期/PAYMENT付款',
  warning_date DATE         NOT NULL COMMENT '触发业务日期',
  message      VARCHAR(1000) NOT NULL COMMENT '预警内容',
  biz_key      VARCHAR(120) NOT NULL COMMENT '业务唯一键（防重复推送）',
  receiver_id  BIGINT       NOT NULL COMMENT '接收人',
  read_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '已读状态：0未读 1已读',
  read_time    DATETIME     NULL COMMENT '阅读时间',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_warning_biz_receiver (biz_key, receiver_id),
  KEY idx_warning_receiver (receiver_id, read_status),
  KEY idx_warning_contract (contract_id),
  CONSTRAINT fk_warning_contract FOREIGN KEY (contract_id) REFERENCES contract_info (id) ON DELETE RESTRICT,
  CONSTRAINT fk_warning_receiver FOREIGN KEY (receiver_id) REFERENCES sys_user (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警信息表';

-- ---------------------------------------------------------------------
-- 数据保护触发器
-- 1. 操作日志禁止删除
-- 2. 归档审核、变更记录禁止删除
-- 3. 已归档合同禁止物理删除、禁止逻辑删除
-- 4. 附件仅允许逻辑删除，禁止物理删除
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_log_forbid_delete;
DROP TRIGGER IF EXISTS trg_review_forbid_delete;
DROP TRIGGER IF EXISTS trg_change_forbid_delete;
DROP TRIGGER IF EXISTS trg_contract_forbid_delete;
DROP TRIGGER IF EXISTS trg_contract_forbid_archive_delete;
DROP TRIGGER IF EXISTS trg_attachment_forbid_delete;

DELIMITER $$
CREATE TRIGGER trg_log_forbid_delete BEFORE DELETE ON sys_operation_log
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '操作日志禁止删除';
END$$

CREATE TRIGGER trg_review_forbid_delete BEFORE DELETE ON archive_review_record
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '归档审核记录禁止删除';
END$$

CREATE TRIGGER trg_change_forbid_delete BEFORE DELETE ON archive_change_record
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '档案变更记录禁止删除';
END$$

CREATE TRIGGER trg_contract_forbid_delete BEFORE DELETE ON contract_info
FOR EACH ROW
BEGIN
  IF OLD.archive_no IS NOT NULL OR OLD.status IN ('ARCHIVED', 'EXPIRED') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已归档合同档案禁止物理删除';
  END IF;
END$$

CREATE TRIGGER trg_contract_forbid_archive_delete BEFORE UPDATE ON contract_info
FOR EACH ROW
BEGIN
  IF OLD.archive_no IS NOT NULL AND NEW.deleted = 1 AND OLD.deleted = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已归档合同档案禁止删除，如需处置请走档案变更审批流程';
  END IF;
END$$

CREATE TRIGGER trg_attachment_forbid_delete BEFORE DELETE ON contract_attachment
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '附件禁止物理删除，请使用逻辑删除';
END$$
DELIMITER ;
