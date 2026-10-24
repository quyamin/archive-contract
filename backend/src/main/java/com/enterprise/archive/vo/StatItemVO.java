package com.enterprise.archive.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatItemVO {
    private String name;
    private Long count;
    private BigDecimal amount;

    public StatItemVO() {
    }

    public StatItemVO(String name, Long count, BigDecimal amount) {
        this.name = name;
        this.count = count;
        this.amount = amount;
    }
}
