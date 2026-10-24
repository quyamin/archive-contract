package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.ConfigUpdateRequest;
import com.enterprise.archive.entity.SysConfig;
import com.enterprise.archive.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    @RequirePerm("system:config:list")
    public Result<List<SysConfig>> list() {
        return Result.ok(configService.list());
    }

    @OpLog(module = "系统参数", action = "修改系统参数")
    @PutMapping
    @RequirePerm("system:config:edit")
    public Result<Void> update(@Valid @RequestBody ConfigUpdateRequest request) {
        configService.update(request);
        return Result.ok();
    }
}
