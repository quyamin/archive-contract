package com.enterprise.archive.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContractVO {
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
    private String contractTypeName;
    private Long deptId;
    private String deptName;
    private Long ownerId;
    private String ownerName;
    private String securityLevel;
    private String securityLevelName;
    private String status;
    private String statusName;
    private String archiveNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archiveTime;
    private String archiveByName;
    private String remark;
    private Integer version;
    private Integer attachmentCount;
    private Long expireDays;
    private Boolean expiringSoon;
    private Boolean canDownload;
    private Boolean canEdit;
    private Long createBy;
    private String createByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<AttachmentVO> attachments;
    private List<PaymentNodeDTO> paymentNodes;
    private List<ReviewVO> reviews;
    private List<ChangeVO> changes;

    @Data
    public static class PaymentNodeDTO {
        private Long id;
        private String nodeName;
        private LocalDate planPayDate;
        private BigDecimal amount;
        private Integer paidStatus;
        private String remark;
        private Boolean expiringSoon;
    }
}
