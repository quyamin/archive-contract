package com.enterprise.archive.common;

import com.enterprise.archive.security.LoginUser;

public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        return user;
    }

    public static LoginUser getOrNull() {
        return HOLDER.get();
    }

    public static Long userId() {
        return get().getId();
    }

    public static String username() {
        return get().getUsername();
    }

    public static boolean hasPerm(String perm) {
        LoginUser user = HOLDER.get();
        return user != null && (user.isAdmin() || user.getPermissions().contains(perm));
    }

    public static void clear() {
        HOLDER.remove();
    }
}
