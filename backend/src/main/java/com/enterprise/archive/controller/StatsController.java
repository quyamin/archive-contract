package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.StatsQuery;
import com.enterprise.archive.service.ExcelService;
import com.enterprise.archive.service.StatsService;
import com.enterprise.archive.vo.DashboardVO;
import com.enterprise.archive.vo.StatsVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final ExcelService excelService;

    @GetMapping("/dashboard")
    @RequirePerm("stats:view")
    public Result<DashboardVO> dashboard() {
        return Result.ok(statsService.dashboard());
    }

    @GetMapping("/overview")
    @RequirePerm("stats:view")
    public Result<StatsVO> overview(StatsQuery query) {
        return Result.ok(statsService.overview(query));
    }

    @OpLog(module = "统计分析", action = "导出统计报表")
    @GetMapping("/export")
    @RequirePerm("stats:export")
    public void export(StatsQuery query, HttpServletResponse response) {
        excelService.exportStats(query, response);
    }
}
