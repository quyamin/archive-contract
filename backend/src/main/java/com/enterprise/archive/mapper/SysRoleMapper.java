package com.enterprise.archive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.archive.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.* FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
