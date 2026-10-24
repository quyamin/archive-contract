package com.enterprise.archive.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private String deptName;
    private List<String> roleCodes;
    private List<String> roleNames;
    private Set<String> permissions;
}
