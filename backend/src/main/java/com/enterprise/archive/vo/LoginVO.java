package com.enterprise.archive.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private long expiresIn;
    private UserProfileVO user;
}
