package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ChangeApplyRequest {
    @NotNull(message = "合同不能为空")
    private Long contractId;
    @NotBlank(message = "请选择变更类型")
    private String changeType;
    @NotBlank(message = "请填写变更原因")
    private String changeReason;
    private Map<String, Object> changeFields;
}
