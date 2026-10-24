package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.WarningQuery;
import com.enterprise.archive.service.WarningService;
import com.enterprise.archive.vo.WarningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/warnings")
@RequiredArgsConstructor
public class WarningController {

    private final WarningService warningService;

    @GetMapping
    @RequirePerm("warning:view")
    public Result<PageResult<WarningVO>> page(WarningQuery query) {
        return Result.ok(warningService.page(query));
    }

    @GetMapping("/unread-count")
    @RequirePerm("warning:view")
    public Result<Long> unreadCount() {
        return Result.ok(warningService.unreadCount());
    }

    @PostMapping("/{id}/read")
    @RequirePerm("warning:view")
    public Result<Void> markRead(@PathVariable Long id) {
        warningService.markRead(id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    @RequirePerm("warning:view")
    public Result<Void> markAllRead() {
        warningService.markAllRead();
        return Result.ok();
    }

    @OpLog(module = "风险预警", action = "手动执行预警扫描")
    @PostMapping("/scan")
    @RequirePerm("warning:manage")
    public Result<Map<String, Object>> scan() {
        return Result.ok(warningService.scan());
    }
}
