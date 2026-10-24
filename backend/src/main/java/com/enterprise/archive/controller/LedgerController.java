package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.ContractQuery;
import com.enterprise.archive.service.ContractService;
import com.enterprise.archive.service.ExcelService;
import com.enterprise.archive.vo.ContractVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final ContractService contractService;
    private final ExcelService excelService;

    @GetMapping("/contracts")
    @RequirePerm("ledger:view")
    public Result<PageResult<ContractVO>> page(ContractQuery query) {
        return Result.ok(contractService.page(query));
    }

    @OpLog(module = "档案台账", action = "导出档案台账")
    @GetMapping("/export")
    @RequirePerm("ledger:export")
    public void export(ContractQuery query, HttpServletResponse response) {
        excelService.exportContracts(query, response, "档案资料台账.xlsx");
    }
}
