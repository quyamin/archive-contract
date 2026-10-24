package com.enterprise.archive.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ContractSaveRequest {
    private Long id;
    @NotBlank(message = "合同编号不能为空")
    private String contractNo;
    @NotBlank(message = "合同名称不能为空")
    private String contractName;
    @NotBlank(message = "甲方不能为空")
    private String partyA;
    @NotBlank(message = "乙方不能为空")
    private String partyB;
    @NotNull(message = "请选择签订日期")
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    @NotNull(message = "合同金额不能为空")
    @DecimalMin(value = "0", message = "合同金额不能小于0")
    private BigDecimal amount;
    @NotBlank(message = "请选择合同类型")
    private String contractType;
    @NotNull(message = "请选择归属部门")
    private Long deptId;
    @NotNull(message = "请选择负责人")
    private Long ownerId;
    @NotBlank(message = "请选择密级")
    private String securityLevel;
    private String remark;
    private List<PaymentNodeDTO> paymentNodes;
}
