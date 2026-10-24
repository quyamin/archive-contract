package com.enterprise.archive.dto;

import lombok.Data;

@Data
public class WarningQuery {
    private long pageNum = 1;
    private long pageSize = 10;
    private String warningType;
    private Integer readStatus;
    private String keyword;
}
