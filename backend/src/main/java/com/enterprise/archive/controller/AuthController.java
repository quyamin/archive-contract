package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.dto.LoginRequest;
import com.enterprise.archive.dto.RegisterRequest;
import com.enterprise.archive.security.LoginUser;
import com.enterprise.archive.service.AuthService;
import com.enterprise.archive.vo.LoginVO;
import com.enterprise.archive.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return Result.ok(authService.login(request, servletRequest));
    }

    /**
     * 普通用户自助注册。注册成功后直接返回登录态，默认角色为普通查阅人员。
     * 该接口在 WebConfig 中放行，不需要登录，也不接受角色、部门、状态等敏感参数。
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return Result.ok(authService.register(request, servletRequest));
    }

    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        LoginUser loginUser = UserContext.get();
        return Result.ok(authService.toProfile(loginUser));
    }

    @OpLog(module = "登录认证", action = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
