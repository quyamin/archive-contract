package com.enterprise.archive.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractQuery {
    private long pageNum = 1;
    private long pageSize = 10;
    private String contractNo;
    private String contractName;
    private String partyA;
    private String partyB;
    private String archiveNo;
    private LocalDate signDateStart;
    private LocalDate signDateEnd;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private Long deptId;
    private Long ownerId;
    private String status;
    private String contractType;
    private String keyword;
    private String attachmentKeyword;
    private Boolean expiringSoon;
    private String category;
}
