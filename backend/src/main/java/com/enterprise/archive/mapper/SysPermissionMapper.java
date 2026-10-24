package com.enterprise.archive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.archive.entity.SysPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("""
            SELECT DISTINCT p.perm_code FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_role r ON r.id = rp.role_id AND r.status = 1 AND r.deleted = 0
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND p.perm_code IS NOT NULL AND p.perm_code <> ''
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT p.* FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.sort, p.id
            """)
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);
}
