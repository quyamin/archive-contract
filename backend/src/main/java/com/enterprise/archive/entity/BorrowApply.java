package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("borrow_apply")
public class BorrowApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String borrowNo;
    private Long contractId;
    private Long applicantId;
    private String purpose;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Long approverId;
    private String approveOpinion;
    private LocalDateTime approveTime;
    private LocalDateTime returnTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
