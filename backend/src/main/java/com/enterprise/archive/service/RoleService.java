package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.dto.RoleSaveRequest;
import com.enterprise.archive.entity.*;
import com.enterprise.archive.mapper.*;
import com.enterprise.archive.vo.PermissionVO;
import com.enterprise.archive.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;

    public List<RoleVO> list() {
        Map<Long, List<Long>> permMap = rolePermissionMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysRolePermission::getRoleId,
                        Collectors.mapping(SysRolePermission::getPermissionId, Collectors.toList())));
        Map<Long, Long> userCount = userRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleId, Collectors.counting()));
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream().map(role -> {
                    RoleVO vo = new RoleVO();
                    vo.setId(role.getId());
                    vo.setRoleCode(role.getRoleCode());
                    vo.setRoleName(role.getRoleName());
                    vo.setDescription(role.getDescription());
                    vo.setStatus(role.getStatus());
                    vo.setPermissionIds(permMap.getOrDefault(role.getId(), Collections.emptyList()));
                    vo.setUserCount(userCount.getOrDefault(role.getId(), 0L).intValue());
                    return vo;
                }).toList();
    }

    @Transactional
    public void save(RoleSaveRequest request) {
        SysRole exists = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, request.getRoleCode()).last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(request.getId())) {
            throw new BizException("角色编码已存在");
        }
        SysRole role;
        if (request.getId() == null) {
            role = new SysRole();
        } else {
            role = roleMapper.selectById(request.getId());
            if (role == null) {
                throw new BizException("角色不存在");
            }
            if ("ADMIN".equals(role.getRoleCode()) && !"ADMIN".equals(request.getRoleCode())) {
                throw new BizException("内置管理员角色编码不允许修改");
            }
        }
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (request.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        if (request.getPermissionIds() != null) {
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, role.getId()));
            for (Long permissionId : new HashSet<>(request.getPermissionIds())) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(role.getId());
                relation.setPermissionId(permissionId);
                rolePermissionMapper.insert(relation);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        if ("ADMIN".equals(role.getRoleCode())) {
            throw new BizException("内置管理员角色不允许删除");
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (count > 0) {
            throw new BizException("该角色下仍有用户，无法删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    public List<PermissionVO> permissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort, SysPermission::getId));
        Map<Long, PermissionVO> map = new LinkedHashMap<>();
        for (SysPermission permission : all) {
            PermissionVO vo = new PermissionVO();
            vo.setId(permission.getId());
            vo.setParentId(permission.getParentId());
            vo.setPermName(permission.getPermName());
            vo.setPermCode(permission.getPermCode());
            vo.setPermType(permission.getPermType());
            vo.setPath(permission.getPath());
            vo.setIcon(permission.getIcon());
            vo.setSort(permission.getSort());
            map.put(vo.getId(), vo);
        }
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO vo : map.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0 || !map.containsKey(vo.getParentId())) {
                roots.add(vo);
            } else {
                map.get(vo.getParentId()).getChildren().add(vo);
            }
        }
        return roots;
    }

    public List<SysPermission> allPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSort, SysPermission::getId));
    }
}
