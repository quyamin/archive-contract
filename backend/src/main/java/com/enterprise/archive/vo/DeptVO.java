package com.enterprise.archive.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeptVO {
    private Long id;
    private Long parentId;
    private String deptCode;
    private String deptName;
    private String leader;
    private String phone;
    private Integer sort;
    private Integer status;
    private List<DeptVO> children = new ArrayList<>();
}
