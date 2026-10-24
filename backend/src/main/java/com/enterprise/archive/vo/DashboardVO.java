package com.enterprise.archive.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private Long totalContracts;
    private Long archivedCount;
    private Long pendingReviewCount;
    private Long expiringCount;
    private Long unreadWarnings;
    private Long activeBorrowCount;
    private BigDecimal totalAmount;
    private List<StatItemVO> typeStats;
    private List<StatItemVO> deptStats;
    private List<StatItemVO> monthStats;
    private List<ContractVO> recentContracts;
    private List<WarningVO> recentWarnings;
}
