package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictSaveRequest {
    private Long id;
    @NotBlank(message = "字典编码不能为空")
    private String dictCode;
    @NotBlank(message = "字典名称不能为空")
    private String dictName;
    private String description;
    private Integer status;
}
