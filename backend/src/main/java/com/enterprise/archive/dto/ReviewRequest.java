package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank(message = "审核动作不能为空")
    private String action;
    private String opinion;
}
