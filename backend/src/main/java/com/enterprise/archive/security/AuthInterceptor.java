package com.enterprise.archive.security;

import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            throw new BizException(401, "请先登录后再操作");
        }
        try {
            Long userId = jwtUtil.parseUserId(token);
            LoginUser loginUser = authService.loadLoginUser(userId);
            UserContext.set(loginUser);
        } catch (BizException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            RequirePerm requirePerm = handlerMethod.getMethodAnnotation(RequirePerm.class);
            if (requirePerm == null) {
                requirePerm = handlerMethod.getBeanType().getAnnotation(RequirePerm.class);
            }
            if (requirePerm != null && !UserContext.hasPerm(requirePerm.value())) {
                throw new BizException(403, "您没有执行此操作的权限");
            }
        }
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
