package com.enterprise.archive.vo;

import com.enterprise.archive.entity.SysDictItem;
import lombok.Data;

import java.util.List;

@Data
public class DictVO {
    private Long id;
    private String dictCode;
    private String dictName;
    private String description;
    private Integer status;
    private List<SysDictItem> items;
}
