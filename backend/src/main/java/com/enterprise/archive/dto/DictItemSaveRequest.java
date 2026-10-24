package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictItemSaveRequest {
    private Long id;
    @NotNull(message = "字典不能为空")
    private Long dictId;
    @NotBlank(message = "字典项名称不能为空")
    private String itemLabel;
    @NotBlank(message = "字典项值不能为空")
    private String itemValue;
    private Integer sort;
    private Integer status;
}
