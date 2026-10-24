package com.enterprise.archive.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentNodeDTO {
    private Long id;
    private String nodeName;
    private LocalDate planPayDate;
    private BigDecimal amount;
    private Integer paidStatus;
    private String remark;
}
