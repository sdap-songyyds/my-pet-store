package cn.edu.csu.mycoursestore.security;

import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        boolean authenticated = authenticate(request);
        boolean adminRequired = requiresAdmin(handler);
        if (!authenticated && (adminRequired || requiresAuth(handler))) {
            if (isJsonHandler(handler)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
            }
            redirectToLogin(request, response);
            return false;
        }
        if (adminRequired) {
            StoreUser user = (StoreUser) request.getAttribute(CURRENT_USER_ATTRIBUTE);
            if (user == null || user.getRole() != UserRole.ADMIN) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权访问后台");
            }
        }
        return true;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        boolean replayable = "GET".equals(method) || "HEAD".equals(method);
        String target = replayable ? request.getRequestURI() : "/";
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && target.startsWith(contextPath)) {
            target = target.substring(contextPath.length());
        }
        if (replayable && request.getQueryString() != null) {
            target += "?" + request.getQueryString();
        }
        response.sendRedirect(contextPath + "/login?redirect="
                + UriUtils.encodeQueryParam(target, StandardCharsets.UTF_8));
    }

    private boolean isJsonHandler(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), ResponseBody.class);
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

    private boolean requiresAdmin(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
    }
}