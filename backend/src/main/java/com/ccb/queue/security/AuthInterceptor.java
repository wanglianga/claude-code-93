package com.ccb.queue.security;

import com.ccb.queue.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 除登录、叫号大屏、静态资源外，其余 /api/** 均需有效令牌 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";
    private final TokenStore tokenStore;

    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("X-Auth-Token");
        var user = tokenStore.resolve(token);
        if (user.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或令牌失效\"}");
            return false;
        }
        request.setAttribute(CURRENT_USER, user.get());
        return true;
    }

    public static User current(HttpServletRequest request) {
        return (User) request.getAttribute(CURRENT_USER);
    }
}
