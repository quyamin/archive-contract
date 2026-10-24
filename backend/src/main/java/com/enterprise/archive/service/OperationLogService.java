package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.dto.LogQuery;
import com.enterprise.archive.entity.SysOperationLog;
import com.enterprise.archive.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogMapper logMapper;

    public void record(Long userId, String username, String module, String operation,
                       String status, String errorMsg, long duration,
                       HttpServletRequest request, String params) {
        try {
            SysOperationLog log = new SysOperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setModule(module);
            log.setOperation(operation);
            log.setStatus(status);
            log.setErrorMsg(errorMsg == null ? null : truncate(errorMsg, 1000));
            log.setDurationMs(duration);
            log.setRequestParams(truncate(params, 4000));
            if (request != null) {
                log.setRequestMethod(request.getMethod());
                log.setRequestUri(request.getRequestURI());
                log.setIp(com.enterprise.archive.util.WebUtils.getIp(request));
            }
            logMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不能影响主业务
        }
    }

    public PageResult<SysOperationLog> page(LogQuery query) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .like(StringUtils.hasText(query.getUsername()), SysOperationLog::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getModule()), SysOperationLog::getModule, query.getModule())
                .like(StringUtils.hasText(query.getOperation()), SysOperationLog::getOperation, query.getOperation())
                .eq(StringUtils.hasText(query.getStatus()), SysOperationLog::getStatus, query.getStatus())
                .ge(query.getStartTime() != null, SysOperationLog::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, SysOperationLog::getCreateTime, query.getEndTime())
                .orderByDesc(SysOperationLog::getId);
        return PageResult.of(logMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper));
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
