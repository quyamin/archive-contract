package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.BorrowApplyRequest;
import com.enterprise.archive.dto.BorrowReviewRequest;
import com.enterprise.archive.service.BorrowService;
import com.enterprise.archive.vo.BorrowVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @GetMapping("/my")
    @RequirePerm("borrow:apply")
    public Result<PageResult<BorrowVO>> my(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(borrowService.page(pageNum, pageSize, status, true, keyword));
    }

    @GetMapping("/approval")
    @RequirePerm("borrow:approve")
    public Result<PageResult<BorrowVO>> approval(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(borrowService.page(pageNum, pageSize, status, false, keyword));
    }

    @OpLog(module = "档案借阅", action = "提交借阅申请")
    @PostMapping
    @RequirePerm("borrow:apply")
    public Result<Long> apply(@Valid @RequestBody BorrowApplyRequest request) {
        return Result.ok(borrowService.apply(request));
    }

    @OpLog(module = "档案借阅", action = "审批借阅申请")
    @PostMapping("/{id}/review")
    @RequirePerm("borrow:approve")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody BorrowReviewRequest request) {
        borrowService.review(id, request);
        return Result.ok();
    }

    @OpLog(module = "档案借阅", action = "归还借阅档案")
    @PostMapping("/{id}/return")
    @RequirePerm("borrow:apply")
    public Result<Void> returnBorrow(@PathVariable Long id) {
        borrowService.returnBorrow(id);
        return Result.ok();
    }
}
