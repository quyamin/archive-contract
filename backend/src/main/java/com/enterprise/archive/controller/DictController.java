package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.dto.DictItemSaveRequest;
import com.enterprise.archive.dto.DictSaveRequest;
import com.enterprise.archive.service.DictService;
import com.enterprise.archive.vo.DictVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping
    @RequirePerm("system:dict:list")
    public Result<List<DictVO>> list() {
        return Result.ok(dictService.listAll());
    }

    @OpLog(module = "字典配置", action = "保存字典")
    @PostMapping
    @RequirePerm("system:dict:edit")
    public Result<Void> saveDict(@Valid @RequestBody DictSaveRequest request) {
        dictService.saveDict(request);
        return Result.ok();
    }

    @OpLog(module = "字典配置", action = "删除字典")
    @DeleteMapping("/{id}")
    @RequirePerm("system:dict:delete")
    public Result<Void> deleteDict(@PathVariable Long id) {
        dictService.deleteDict(id);
        return Result.ok();
    }

    @OpLog(module = "字典配置", action = "保存字典项")
    @PostMapping("/items")
    @RequirePerm("system:dict:edit")
    public Result<Void> saveItem(@Valid @RequestBody DictItemSaveRequest request) {
        dictService.saveItem(request);
        return Result.ok();
    }

    @OpLog(module = "字典配置", action = "删除字典项")
    @DeleteMapping("/items/{id}")
    @RequirePerm("system:dict:delete")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.ok();
    }
}
