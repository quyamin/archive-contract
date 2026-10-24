package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.dto.DictItemSaveRequest;
import com.enterprise.archive.dto.DictSaveRequest;
import com.enterprise.archive.entity.SysDict;
import com.enterprise.archive.entity.SysDictItem;
import com.enterprise.archive.mapper.SysDictItemMapper;
import com.enterprise.archive.mapper.SysDictMapper;
import com.enterprise.archive.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper itemMapper;

    public List<DictVO> listAll() {
        List<SysDict> dicts = dictMapper.selectList(new LambdaQueryWrapper<SysDict>().orderByAsc(SysDict::getId));
        Map<Long, List<SysDictItem>> itemMap = itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                        .orderByAsc(SysDictItem::getSort, SysDictItem::getId))
                .stream().collect(Collectors.groupingBy(SysDictItem::getDictId));
        return dicts.stream().map(dict -> {
            DictVO vo = new DictVO();
            BeanUtils.copyProperties(dict, vo);
            vo.setItems(itemMap.getOrDefault(dict.getId(), Collections.emptyList()));
            return vo;
        }).toList();
    }

    public List<SysDictItem> items(String dictCode) {
        SysDict dict = dictMapper.selectOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictCode, dictCode).eq(SysDict::getStatus, 1).last("LIMIT 1"));
        if (dict == null) {
            return Collections.emptyList();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, dict.getId())
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSort, SysDictItem::getId));
    }

    public Map<String, String> labelMap(String dictCode) {
        return items(dictCode).stream().collect(Collectors.toMap(
                SysDictItem::getItemValue, SysDictItem::getItemLabel, (a, b) -> a));
    }

    public void saveDict(DictSaveRequest request) {
        SysDict exists = dictMapper.selectOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictCode, request.getDictCode()).last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(request.getId())) {
            throw new BizException("字典编码已存在");
        }
        SysDict dict;
        if (request.getId() == null) {
            dict = new SysDict();
        } else {
            dict = dictMapper.selectById(request.getId());
            if (dict == null) {
                throw new BizException("字典不存在");
            }
        }
        dict.setDictCode(request.getDictCode());
        dict.setDictName(request.getDictName());
        dict.setDescription(request.getDescription());
        dict.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (request.getId() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
    }

    @Transactional
    public void deleteDict(Long id) {
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));
        if (count > 0) {
            throw new BizException("请先删除字典项");
        }
        dictMapper.deleteById(id);
    }

    public void saveItem(DictItemSaveRequest request) {
        SysDictItem exists = itemMapper.selectOne(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, request.getDictId())
                .eq(SysDictItem::getItemValue, request.getItemValue()).last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(request.getId())) {
            throw new BizException("该字典项值已存在");
        }
        SysDictItem item;
        if (request.getId() == null) {
            item = new SysDictItem();
        } else {
            item = itemMapper.selectById(request.getId());
            if (item == null) {
                throw new BizException("字典项不存在");
            }
        }
        item.setDictId(request.getDictId());
        item.setItemLabel(request.getItemLabel());
        item.setItemValue(request.getItemValue());
        item.setSort(request.getSort() == null ? 0 : request.getSort());
        item.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (request.getId() == null) {
            itemMapper.insert(item);
        } else {
            itemMapper.updateById(item);
        }
    }

    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }
}
