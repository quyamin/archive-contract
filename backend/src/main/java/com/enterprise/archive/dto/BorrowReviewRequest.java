package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BorrowReviewRequest {
    @NotBlank(message = "审批动作不能为空")
    private String action;
    private String opinion;
}
