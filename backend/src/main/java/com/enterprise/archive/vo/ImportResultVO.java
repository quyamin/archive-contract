package com.enterprise.archive.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVO {
    private int successCount;
    private int failCount;
    private List<String> messages = new ArrayList<>();
    private List<Long> contractIds = new ArrayList<>();
}
