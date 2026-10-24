package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_warning")
public class SysWarning {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String warningType;
    private LocalDate warningDate;
    private String message;
    private String bizKey;
    private Long receiverId;
    private Integer readStatus;
    private LocalDateTime readTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
