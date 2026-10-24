package com.enterprise.archive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{3,29}$",
            message = "账号需以字母开头，长度为 4-30 位，可包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度需为 2-50 个字符")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;
}
