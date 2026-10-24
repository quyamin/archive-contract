package com.enterprise.archive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfigUpdateRequest {
    @NotBlank(message = "配置项不能为空")
    private String configKey;
    @NotBlank(message = "配置值不能为空")
    private String configValue;
}
