package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("archive_change_record")
public class ArchiveChangeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String changeType;
    private String changeReason;
    private String beforeContent;
    private String afterContent;
    private String status;
    private Long applicantId;
    private LocalDateTime applyTime;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveOpinion;
}
