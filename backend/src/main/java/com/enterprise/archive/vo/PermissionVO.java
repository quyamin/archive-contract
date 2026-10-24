package com.enterprise.archive.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionVO {
    private Long id;
    private Long parentId;
    private String permName;
    private String permCode;
    private String permType;
    private String path;
    private String icon;
    private Integer sort;
    private List<PermissionVO> children = new ArrayList<>();
}
