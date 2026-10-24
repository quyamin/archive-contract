package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.dto.LoginRequest;
import com.enterprise.archive.dto.RegisterRequest;
import com.enterprise.archive.entity.SysDept;
import com.enterprise.archive.entity.SysRole;
import com.enterprise.archive.entity.SysUser;
import com.enterprise.archive.entity.SysUserRole;
import com.enterprise.archive.mapper.SysDeptMapper;
import com.enterprise.archive.mapper.SysPermissionMapper;
import com.enterprise.archive.mapper.SysRoleMapper;
import com.enterprise.archive.mapper.SysUserMapper;
import com.enterprise.archive.mapper.SysUserRoleMapper;
import com.enterprise.archive.security.JwtUtil;
import com.enterprise.archive.security.LoginUser;
import com.enterprise.archive.vo.LoginVO;
import com.enterprise.archive.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** 自助注册用户的默认角色：普通查阅人员。 */
    private static final String DEFAULT_ROLE_CODE = "VIEWER";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OperationLogService logService;

    @Transactional
    public LoginVO login(LoginRequest request, HttpServletRequest servletRequest) {
        long start = System.currentTimeMillis();
        try {
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, request.getUsername()).last("LIMIT 1"));
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logService.record(user == null ? null : user.getId(), request.getUsername(),
                        "登录认证", "用户登录", "FAIL", "账号或密码错误",
                        System.currentTimeMillis() - start, servletRequest, "{\"username\":\"" + request.getUsername() + "\"}");
                throw new BizException("账号或密码错误");
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                logService.record(user.getId(), user.getUsername(), "登录认证", "用户登录",
                        "FAIL", "账号已禁用", System.currentTimeMillis() - start, servletRequest, "");
                throw new BizException("账号已被禁用，请联系管理员");
            }
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            LoginUser loginUser = buildLoginUser(user);
            LoginVO vo = new LoginVO();
            vo.setToken(jwtUtil.generate(user.getId(), user.getUsername()));
            vo.setExpiresIn(jwtUtil.getExpireSeconds());
            vo.setUser(toProfile(loginUser));
            logService.record(user.getId(), user.getRealName(), "登录认证", "用户登录",
                    "SUCCESS", null, System.currentTimeMillis() - start, servletRequest, "");
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            logService.record(null, request.getUsername(), "登录认证", "用户登录",
                    "FAIL", e.getMessage(), System.currentTimeMillis() - start, servletRequest, "");
            throw e;
        }
    }

    /**
     * 自助注册：只允许创建启用的普通查阅人员账号。
     * 角色、部门、状态均由服务端固定，前端参数无法越权指定。
     */
    @Transactional
    public LoginVO register(RegisterRequest request, HttpServletRequest servletRequest) {
        long start = System.currentTimeMillis();
        String username = request.getUsername().trim();
        String realName = request.getRealName().trim();

        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("LIMIT 1"));
        if (exists != null) {
            throw new BizException("登录账号已存在，请更换");
        }

        SysRole defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
                .eq(SysRole::getStatus, 1)
                .last("LIMIT 1"));
        if (defaultRole == null) {
            throw new BizException("系统未配置默认角色，请联系管理员");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(realName);
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setStatus(1);
        user.setRemark("用户自助注册");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时由唯一索引兜底。
            throw new BizException("登录账号已存在，请更换");
        }

        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(defaultRole.getId());
        userRoleMapper.insert(relation);

        LoginUser loginUser = buildLoginUser(user);
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generate(user.getId(), user.getUsername()));
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        vo.setUser(toProfile(loginUser));
        logService.record(user.getId(), user.getUsername(), "登录认证", "用户注册",
                "SUCCESS", null, System.currentTimeMillis() - start, servletRequest,
                "{\"username\":\"" + username + "\"}");
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public LoginUser loadLoginUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() != 1)) {
            throw new BizException(401, "账号不存在或已被禁用");
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setDeptId(user.getDeptId());
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            loginUser.setDeptName(dept == null ? "" : dept.getDeptName());
        }
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        Set<String> roleCodes = new HashSet<>();
        for (SysRole role : roles) {
            roleCodes.add(role.getRoleCode());
        }
        loginUser.setRoleCodes(roleCodes);
        loginUser.setPermissions(new HashSet<>(permissionMapper.selectPermCodesByUserId(user.getId())));
        return loginUser;
    }

    public UserProfileVO toProfile(LoginUser loginUser) {
        UserProfileVO profile = new UserProfileVO();
        profile.setId(loginUser.getId());
        profile.setUsername(loginUser.getUsername());
        profile.setRealName(loginUser.getRealName());
        profile.setDeptId(loginUser.getDeptId());
        profile.setDeptName(loginUser.getDeptName());
        profile.setRoleCodes(loginUser.getRoleCodes().stream().sorted().toList());
        List<SysRole> roles = roleMapper.selectByUserId(loginUser.getId());
        profile.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());
        profile.setPermissions(loginUser.getPermissions());
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user != null) {
            profile.setPhone(user.getPhone());
            profile.setEmail(user.getEmail());
        }
        return profile;
    }
}
