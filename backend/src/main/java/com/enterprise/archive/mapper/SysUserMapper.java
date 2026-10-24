package com.enterprise.archive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.archive.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT DISTINCT u.id FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role_permission rp ON rp.role_id = ur.role_id
            JOIN sys_permission p ON p.id = rp.permission_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE u.status = 1 AND u.deleted = 0 AND p.perm_code = #{permCode}
            """)
    List<Long> selectUserIdsByPerm(@Param("permCode") String permCode);
}
