package cn.edu.csu.mycoursestore.user.controller;

import cn.edu.csu.mycoursestore.security.AuthCookieFactory;
import cn.edu.csu.mycoursestore.security.CurrentUser;
import cn.edu.csu.mycoursestore.security.RequireAuth;
import cn.edu.csu.mycoursestore.user.dto.AuthResponse;
import cn.edu.csu.mycoursestore.user.dto.LoginRequest;
import cn.edu.csu.mycoursestore.user.dto.RegisterRequest;
import cn.edu.csu.mycoursestore.user.dto.UserProfile;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieFactory cookieFactory;

    public AuthController(AuthService authService, AuthCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(auth.token()).toString())
                .body(auth);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(auth.token()).toString())
                .body(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    @RequireAuth
    @GetMapping("/me")
    public UserProfile me(@CurrentUser StoreUser user) {
        return UserProfile.from(user);
    }
}