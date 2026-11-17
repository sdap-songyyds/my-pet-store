package cn.edu.csu.mycoursestore.user.service;

import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.security.JwtTokenProvider;
import cn.edu.csu.mycoursestore.user.dto.AuthResponse;
import cn.edu.csu.mycoursestore.user.dto.LoginRequest;
import cn.edu.csu.mycoursestore.user.dto.RegisterRequest;
import cn.edu.csu.mycoursestore.user.dto.UserProfile;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final StoreUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(StoreUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userMapper.exists(new LambdaQueryWrapper<StoreUser>().eq(StoreUser::getUsername, username))) {
            throw usernameTaken();
        }

        StoreUser user = new StoreUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ENABLED);
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw usernameTaken();
        }
        return authResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        StoreUser user = userMapper.selectOne(
                new LambdaQueryWrapper<StoreUser>().eq(StoreUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        if (user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "账号已被禁用");
        }
        return authResponse(user);
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse authResponse(StoreUser user) {
        return new AuthResponse(tokenProvider.issue(user), tokenProvider.expiresInSeconds(), UserProfile.from(user));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ApiException usernameTaken() {
        return new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "用户名已被占用");
    }
}