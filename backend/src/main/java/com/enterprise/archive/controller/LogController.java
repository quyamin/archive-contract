package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.LogQuery;
import com.enterprise.archive.entity.SysOperationLog;
import com.enterprise.archive.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogService logService;

    @GetMapping
    @RequirePerm("system:log:list")
    public Result<PageResult<SysOperationLog>> page(LogQuery query) {
        return Result.ok(logService.page(query));
    }
}
