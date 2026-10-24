package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("contract_info")
public class ContractInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private String contractName;
    private String partyA;
    private String partyB;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private BigDecimal amount;
    private String contractType;
    private Long deptId;
    private Long ownerId;
    private String securityLevel;
    private String status;
    private String archiveNo;
    private LocalDateTime archiveTime;
    private Long archiveBy;
    private String remark;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
