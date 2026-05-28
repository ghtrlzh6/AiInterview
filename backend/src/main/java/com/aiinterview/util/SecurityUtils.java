package com.aiinterview.util;

import com.aiinterview.common.BusinessException;
import com.aiinterview.security.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static SecurityUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
            return user;
        }
        throw BusinessException.unauthorized("未登录");
    }

    public static Long currentUserId() {
        return currentUser().getUserId();
    }
}
