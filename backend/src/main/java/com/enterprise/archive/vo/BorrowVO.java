package com.enterprise.archive.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowVO {
    private Long id;
    private String borrowNo;
    private Long contractId;
    private String contractNo;
    private String contractName;
    private Long applicantId;
    private String applicantName;
    private String applicantDept;
    private String purpose;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private String status;
    private Long approverId;
    private String approverName;
    private String approveOpinion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private Boolean canApprove;
    private Boolean canReturn;
}
