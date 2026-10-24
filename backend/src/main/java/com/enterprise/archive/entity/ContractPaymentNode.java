package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("contract_payment_node")
public class ContractPaymentNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String nodeName;
    private LocalDate planPayDate;
    private BigDecimal amount;
    private Integer paidStatus;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
