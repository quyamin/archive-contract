package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.*;
import com.enterprise.archive.service.ContractService;
import com.enterprise.archive.vo.ChangeVO;
import com.enterprise.archive.vo.ContractVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ContractService contractService;

    @GetMapping("/reviews")
    @RequirePerm("archive:review")
    public Result<PageResult<ContractVO>> reviews(ContractQuery query) {
        return Result.ok(contractService.reviewPage(query));
    }

    @OpLog(module = "归档审核", action = "审核归档申请")
    @PostMapping("/reviews/{id}")
    @RequirePerm("archive:review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        contractService.review(id, request);
        return Result.ok();
    }

    @GetMapping("/changes")
    @RequirePerm("change:apply")
    public Result<PageResult<ChangeVO>> changes(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "false") boolean onlyMine) {
        return Result.ok(contractService.changePage(pageNum, pageSize, status, onlyMine));
    }

    @OpLog(module = "档案变更", action = "申请档案变更")
    @PostMapping("/changes")
    @RequirePerm("change:apply")
    public Result<Void> applyChange(@Valid @RequestBody ChangeApplyRequest request) {
        contractService.applyChange(request);
        return Result.ok();
    }

    @OpLog(module = "档案变更", action = "审批档案变更")
    @PostMapping("/changes/{id}/review")
    @RequirePerm("change:review")
    public Result<Void> reviewChange(@PathVariable Long id, @Valid @RequestBody ChangeReviewRequest request) {
        contractService.reviewChange(id, request);
        return Result.ok();
    }
}
