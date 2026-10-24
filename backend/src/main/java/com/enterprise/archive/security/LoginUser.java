package com.enterprise.archive.security;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private Set<String> roleCodes = new HashSet<>();
    private Set<String> permissions = new HashSet<>();

    public boolean isAdmin() {
        return roleCodes.contains("ADMIN");
    }

    public boolean has(String perm) {
        return isAdmin() || permissions.contains(perm);
    }
}
