package com.enterprise.archive.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StatsVO {
    private Long totalCount;
    private BigDecimal totalAmount;
    private List<StatItemVO> typeStats;
    private List<StatItemVO> deptStats;
    private List<StatItemVO> yearStats;
    private List<StatItemVO> amountRangeStats;
    private List<StatItemVO> partnerStats;
    private List<StatItemVO> monthStats;
}
