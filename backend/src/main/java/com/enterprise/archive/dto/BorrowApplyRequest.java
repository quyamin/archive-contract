package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowApplyRequest {
    @NotNull(message = "请选择合同")
    private Long contractId;
    @NotBlank(message = "请填写借阅用途")
    private String purpose;
    @NotNull(message = "请选择借阅开始时间")
    private LocalDateTime startTime;
    @NotNull(message = "请选择借阅结束时间")
    private LocalDateTime endTime;
}
