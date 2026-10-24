package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.UserSaveRequest;
import com.enterprise.archive.service.UserService;
import com.enterprise.archive.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequirePerm("system:user:list")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(pageNum, pageSize, keyword, deptId, status));
    }

    @OpLog(module = "用户管理", action = "新增用户")
    @PostMapping
    @RequirePerm("system:user:add")
    public Result<Void> add(@Valid @RequestBody UserSaveRequest request) {
        request.setId(null);
        userService.save(request);
        return Result.ok();
    }

    @OpLog(module = "用户管理", action = "修改用户")
    @PutMapping
    @RequirePerm("system:user:edit")
    public Result<Void> update(@Valid @RequestBody UserSaveRequest request) {
        userService.save(request);
        return Result.ok();
    }

    @OpLog(module = "用户管理", action = "重置密码")
    @PutMapping("/{id}/password")
    @RequirePerm("system:user:edit")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }

    @OpLog(module = "用户管理", action = "变更账号状态")
    @PutMapping("/{id}/status")
    @RequirePerm("system:user:edit")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return Result.ok();
    }

    @OpLog(module = "用户管理", action = "删除用户")
    @DeleteMapping("/{id}")
    @RequirePerm("system:user:delete")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
