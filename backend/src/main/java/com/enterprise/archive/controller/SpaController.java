package com.enterprise.archive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端使用 History 路由，直接访问或刷新业务路径时需要回退到 index.html。
 * 该控制器不参与 /api 前缀，因此不会被 REST 接口或登录拦截器影响。
 */
@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/login",
            "/dashboard",
            "/contracts",
            "/contracts/new",
            "/contracts/{id}",
            "/contracts/{id}/edit",
            "/archive/review",
            "/archive/changes",
            "/borrow/my",
            "/borrow/approval",
            "/warnings",
            "/stats",
            "/ledger",
            "/system/users",
            "/system/roles",
            "/system/depts",
            "/system/dicts",
            "/system/config",
            "/system/logs"
    })
    public String index() {
        return "forward:/index.html";
    }
}
