package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptSaveRequest {
    private Long id;
    private Long parentId;
    @NotBlank(message = "部门编码不能为空")
    private String deptCode;
    @NotBlank(message = "部门名称不能为空")
    private String deptName;
    private String leader;
    private String phone;
    private Integer sort;
    private Integer status;
}
