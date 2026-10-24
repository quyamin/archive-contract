package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.DeptSaveRequest;
import com.enterprise.archive.service.DeptService;
import com.enterprise.archive.vo.DeptVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    @RequirePerm("system:dept:list")
    public Result<List<DeptVO>> tree() {
        return Result.ok(deptService.tree());
    }

    @OpLog(module = "组织机构", action = "保存部门")
    @PostMapping
    @RequirePerm("system:dept:edit")
    public Result<Void> save(@Valid @RequestBody DeptSaveRequest request) {
        deptService.save(request);
        return Result.ok();
    }

    @OpLog(module = "组织机构", action = "删除部门")
    @DeleteMapping("/{id}")
    @RequirePerm("system:dept:delete")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
