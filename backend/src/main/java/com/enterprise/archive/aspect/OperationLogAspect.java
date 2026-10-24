package com.enterprise.archive.aspect;

import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.security.LoginUser;
import com.enterprise.archive.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService logService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            status = "FAIL";
            error = throwable.getMessage();
            throw throwable;
        } finally {
            LoginUser user = UserContext.getOrNull();
            HttpServletRequest request = currentRequest();
            String params = serializeArgs(joinPoint);
            logService.record(user == null ? null : user.getId(),
                    user == null ? null : user.getRealName(),
                    opLog.module(), opLog.action(), status, error,
                    System.currentTimeMillis() - start, request, params);
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] names = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            Object[] safeArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof MultipartFile || arg instanceof MultipartFile[]
                        || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                    safeArgs[i] = "[文件或请求对象]";
                } else {
                    safeArgs[i] = arg;
                }
            }
            String json = objectMapper.writeValueAsString(
                    Arrays.stream(names).map(name -> name).toArray());
            String values = objectMapper.writeValueAsString(safeArgs);
            String result = json + " => " + values;
            return result.replaceAll("(?i)(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }
}
