package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.dto.DeptSaveRequest;
import com.enterprise.archive.entity.ContractInfo;
import com.enterprise.archive.entity.SysDept;
import com.enterprise.archive.entity.SysUser;
import com.enterprise.archive.mapper.ContractInfoMapper;
import com.enterprise.archive.mapper.SysDeptMapper;
import com.enterprise.archive.mapper.SysUserMapper;
import com.enterprise.archive.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final ContractInfoMapper contractMapper;

    public List<DeptVO> tree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort, SysDept::getId));
        Map<Long, DeptVO> map = new LinkedHashMap<>();
        for (SysDept dept : all) {
            DeptVO vo = new DeptVO();
            BeanUtils.copyProperties(dept, vo);
            map.put(vo.getId(), vo);
        }
        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO vo : map.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0 || !map.containsKey(vo.getParentId())) {
                roots.add(vo);
            } else {
                map.get(vo.getParentId()).getChildren().add(vo);
            }
        }
        return roots;
    }

    public List<SysDept> flatList() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1).orderByAsc(SysDept::getSort, SysDept::getId));
    }

    public void save(DeptSaveRequest request) {
        SysDept exists = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptCode, request.getDeptCode()).last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(request.getId())) {
            throw new BizException("部门编码已存在");
        }
        SysDept dept;
        if (request.getId() == null) {
            dept = new SysDept();
        } else {
            dept = deptMapper.selectById(request.getId());
            if (dept == null) {
                throw new BizException("部门不存在");
            }
        }
        validateParent(request.getId(), request.getParentId());
        dept.setParentId(request.getParentId() == null || request.getParentId() == 0
                ? null : request.getParentId());
        dept.setDeptCode(request.getDeptCode());
        dept.setDeptName(request.getDeptName());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setSort(request.getSort() == null ? 0 : request.getSort());
        dept.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (request.getId() == null) {
            deptMapper.insert(dept);
        } else {
            deptMapper.updateById(dept);
        }
    }

    /** 校验上级部门存在，且不会形成环形层级。 */
    private void validateParent(Long id, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(id)) {
            throw new BizException("上级部门不能选择自己");
        }
        Long cursor = parentId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null && cursor != 0 && visited.add(cursor)) {
            if (cursor.equals(id)) {
                throw new BizException("上级部门不能选择当前部门的下级部门");
            }
            SysDept parent = deptMapper.selectById(cursor);
            if (parent == null) {
                throw new BizException("上级部门不存在");
            }
            cursor = parent.getParentId();
        }
    }

    public void delete(Long id) {
        Long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BizException("该部门下存在子部门，无法删除");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BizException("该部门下存在用户，无法删除");
        }
        Long contracts = contractMapper.selectCount(new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getDeptId, id));
        if (contracts > 0) {
            throw new BizException("该部门已关联合同档案，无法删除");
        }
        deptMapper.deleteById(id);
    }

    public Map<Long, String> nameMap() {
        Map<Long, String> map = new HashMap<>();
        deptMapper.selectList(null).forEach(d -> map.put(d.getId(), d.getDeptName()));
        return map;
    }
}
