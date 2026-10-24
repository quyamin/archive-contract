package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.entity.SysDept;
import com.enterprise.archive.entity.SysDictItem;
import com.enterprise.archive.service.DeptService;
import com.enterprise.archive.service.DictService;
import com.enterprise.archive.service.RoleService;
import com.enterprise.archive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/options")
@RequiredArgsConstructor
public class OptionController {

    private final UserService userService;
    private final DeptService deptService;
    private final DictService dictService;
    private final RoleService roleService;

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        return Result.ok(userService.options());
    }

    @GetMapping("/depts")
    public Result<List<SysDept>> depts() {
        return Result.ok(deptService.flatList());
    }

    @GetMapping("/dict/{code}")
    public Result<List<SysDictItem>> dictItems(@PathVariable String code) {
        return Result.ok(dictService.items(code));
    }

    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> roles() {
        return Result.ok(roleService.list().stream().map(r -> Map.<String, Object>of(
                "id", r.getId(), "roleName", r.getRoleName(), "roleCode", r.getRoleCode())).toList());
    }
}
