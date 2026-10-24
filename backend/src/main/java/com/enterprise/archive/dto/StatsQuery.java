package com.enterprise.archive.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatsQuery {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long deptId;
    private String contractType;
}
