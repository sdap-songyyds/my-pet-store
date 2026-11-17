package cn.edu.csu.mycoursestore.security;

import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties properties;
    private final StoreUserMapper userMapper;

    public AuthInterceptor(JwtTokenProvider tokenProvider, JwtProperties properties, StoreUserMapper userMapper) {
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        boolean authenticated = authenticate(request);
        if (!authenticated && requiresAuth(handler)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        }
        return true;
    }

    private boolean authenticate(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return false;
        }
        Optional<Long> userId = tokenProvider.parseUserId(token);
        if (userId.isEmpty()) {
            return false;
        }
        StoreUser user = userMapper.selectById(userId.get());
        if (user == null || user.getStatus() != UserStatus.ENABLED) {
            return false;
        }
        request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (properties.cookieName().equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean requiresAuth(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(RequireAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);
    }
}