package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.UserSaveRequest;
import com.enterprise.archive.entity.SysDept;
import com.enterprise.archive.entity.SysRole;
import com.enterprise.archive.entity.SysUser;
import com.enterprise.archive.entity.SysUserRole;
import com.enterprise.archive.mapper.SysDeptMapper;
import com.enterprise.archive.mapper.SysRoleMapper;
import com.enterprise.archive.mapper.SysUserMapper;
import com.enterprise.archive.mapper.SysUserRoleMapper;
import com.enterprise.archive.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(long pageNum, long pageSize, String keyword, Long deptId, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getPhone, keyword))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        Map<Long, List<SysUserRole>> roleMap = userRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId));
        Map<Long, String> roleNames = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));
        List<UserVO> records = page.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setDeptId(user.getDeptId());
            vo.setDeptName(deptNames.getOrDefault(user.getDeptId(), ""));
            vo.setStatus(user.getStatus());
            vo.setRemark(user.getRemark());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());
            List<SysUserRole> relations = roleMap.getOrDefault(user.getId(), Collections.emptyList());
            vo.setRoleIds(relations.stream().map(SysUserRole::getRoleId).toList());
            vo.setRoleNames(relations.stream().map(r -> roleNames.getOrDefault(r.getRoleId(), "")).toList());
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public void save(UserSaveRequest request) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()).last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(request.getId())) {
            throw new BizException("登录账号已存在");
        }
        SysUser user;
        if (request.getId() == null) {
            if (!StringUtils.hasText(request.getPassword()) || request.getPassword().length() < 6) {
                throw new BizException("初始密码长度不能少于6位");
            }
            user = new SysUser();
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        } else {
            user = userMapper.selectById(request.getId());
            if (user == null) {
                throw new BizException("用户不存在");
            }
            if (StringUtils.hasText(request.getPassword())) {
                if (request.getPassword().length() < 6) {
                    throw new BizException("密码长度不能少于6位");
                }
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            if (user.getId() == 1L && request.getStatus() != null && request.getStatus() == 0) {
                throw new BizException("系统超级管理员账号不允许禁用");
            }
        }
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDeptId(request.getDeptId());
        user.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        }
        bindRoles(user.getId(), request.getRoleIds());
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    public void resetPassword(Long id, String password) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BizException("新密码长度不能少于6位");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    public void changeStatus(Long id, Integer status) {
        if (id == 1L && status == 0) {
            throw new BizException("系统超级管理员账号不允许禁用");
        }
        if (id.equals(UserContext.userId()) && status == 0) {
            throw new BizException("不能禁用当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        if (id == 1L) {
            throw new BizException("系统超级管理员账号不允许删除");
        }
        if (id.equals(UserContext.userId())) {
            throw new BizException("不能删除当前登录账号");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
    }

    public List<Map<String, Object>> options() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1).orderByAsc(SysUser::getRealName))
                .stream().map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getRealName());
                    map.put("deptId", u.getDeptId());
                    return map;
                }).toList();
    }

    public List<SysUser> listAllSimple() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1).orderByAsc(SysUser::getRealName));
    }
}
