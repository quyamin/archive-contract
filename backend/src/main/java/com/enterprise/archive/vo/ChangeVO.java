package com.enterprise.archive.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeVO {
    private Long id;
    private Long contractId;
    private String contractNo;
    private String contractName;
    private String changeType;
    private String changeReason;
    private String beforeContent;
    private String afterContent;
    private String status;
    private Long applicantId;
    private String applicantName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    private Long approverId;
    private String approverName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;
    private String approveOpinion;
}
