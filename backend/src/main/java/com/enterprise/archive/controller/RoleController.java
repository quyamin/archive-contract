package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.RoleSaveRequest;
import com.enterprise.archive.service.RoleService;
import com.enterprise.archive.vo.PermissionVO;
import com.enterprise.archive.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequirePerm("system:role:list")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    @GetMapping("/permissions")
    @RequirePerm("system:role:list")
    public Result<List<PermissionVO>> permissionTree() {
        return Result.ok(roleService.permissionTree());
    }

    @OpLog(module = "角色权限", action = "保存角色")
    @PostMapping
    @RequirePerm("system:role:edit")
    public Result<Void> save(@Valid @RequestBody RoleSaveRequest request) {
        roleService.save(request);
        return Result.ok();
    }

    @OpLog(module = "角色权限", action = "删除角色")
    @DeleteMapping("/{id}")
    @RequirePerm("system:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
