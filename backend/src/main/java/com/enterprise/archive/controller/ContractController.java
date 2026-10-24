package com.enterprise.archive.controller;

import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.ContractQuery;
import com.enterprise.archive.dto.ContractSaveRequest;
import com.enterprise.archive.service.ContractService;
import com.enterprise.archive.service.ExcelService;
import com.enterprise.archive.vo.ContractVO;
import com.enterprise.archive.vo.ImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ExcelService excelService;

    @GetMapping
    @RequirePerm("contract:list")
    public Result<PageResult<ContractVO>> page(ContractQuery query) {
        return Result.ok(contractService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePerm("contract:view")
    public Result<ContractVO> detail(@PathVariable Long id) {
        return Result.ok(contractService.detail(id));
    }

    @OpLog(module = "合同档案", action = "新增合同")
    @PostMapping
    @RequirePerm("contract:add")
    public Result<Long> add(@Valid @RequestBody ContractSaveRequest request) {
        request.setId(null);
        return Result.ok(contractService.save(request));
    }

    @OpLog(module = "合同档案", action = "修改合同")
    @PutMapping("/{id}")
    @RequirePerm("contract:edit")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody ContractSaveRequest request) {
        request.setId(id);
        return Result.ok(contractService.save(request));
    }

    @OpLog(module = "合同档案", action = "提交归档")
    @PostMapping("/{id}/submit")
    @RequirePerm("contract:submit")
    public Result<Void> submit(@PathVariable Long id) {
        contractService.submit(id);
        return Result.ok();
    }

    @OpLog(module = "合同档案", action = "删除草稿")
    @DeleteMapping("/{id}")
    @RequirePerm("contract:delete")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.deleteDraft(id);
        return Result.ok();
    }

    @OpLog(module = "合同档案", action = "导入合同")
    @PostMapping("/import")
    @RequirePerm("contract:import")
    public Result<ImportResultVO> importContracts(@RequestParam("file") MultipartFile file) {
        return Result.ok(excelService.importContracts(file));
    }

    @GetMapping("/import-template")
    @RequirePerm("contract:import")
    public void importTemplate(HttpServletResponse response) {
        excelService.downloadImportTemplate(response);
    }

    @OpLog(module = "合同档案", action = "导出合同台账")
    @GetMapping("/export")
    @RequirePerm("contract:export")
    public void export(ContractQuery query, HttpServletResponse response) {
        excelService.exportContracts(query, response, "合同档案台账.xlsx");
    }
}
