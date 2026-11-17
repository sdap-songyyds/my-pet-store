package cn.edu.csu.mycoursestore.user.controller;

import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.common.FormErrors;
import cn.edu.csu.mycoursestore.security.AuthCookieFactory;
import cn.edu.csu.mycoursestore.security.CurrentUser;
import cn.edu.csu.mycoursestore.security.RequireAuth;
import cn.edu.csu.mycoursestore.user.dto.AuthResponse;
import cn.edu.csu.mycoursestore.user.dto.ChangePasswordRequest;
import cn.edu.csu.mycoursestore.user.dto.UpdateProfileRequest;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.format.DateTimeFormatter;

@Controller
public class AccountController {

    private static final DateTimeFormatter JOINED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthService authService;
    private final AuthCookieFactory cookieFactory;

    public AccountController(AuthService authService, AuthCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @RequireAuth
    @GetMapping("/account")
    public String account(@CurrentUser StoreUser user,
                          @RequestParam(required = false) String profileUpdated,
                          @RequestParam(required = false) String passwordUpdated,
                          Model model) {
        populate(model, user);
        if (profileUpdated != null) {
            model.addAttribute("success", "个人资料已更新");
        } else if (passwordUpdated != null) {
            model.addAttribute("success", "密码已更新，登录有效期已续期");
        }
        return "account/index";
    }

    @RequireAuth
    @PostMapping("/account/profile")
    public String updateProfile(@CurrentUser StoreUser user,
                                @Valid @ModelAttribute("profileForm") UpdateProfileRequest form,
                                BindingResult binding,
                                Model model) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "profile", binding);
            populate(model, user);
            return "account/index";
        }
        authService.updateProfile(user.getId(), form);
        return "redirect:/account?profileUpdated=1";
    }

    @RequireAuth
    @PostMapping("/account/password")
    public String changePassword(@CurrentUser StoreUser user,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordRequest form,
                                 BindingResult binding,
                                 HttpServletResponse response,
                                 Model model) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "password", binding);
            populate(model, user);
            return "account/index";
        }
        if (!form.newPassword().equals(form.confirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "两次输入的新密码不一致");
            FormErrors.add(model, "password", binding);
            populate(model, user);
            return "account/index";
        }
        try {
            AuthResponse auth = authService.changePassword(user.getId(), form);
            response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.create(auth.token()).toString());
            return "redirect:/account?passwordUpdated=1";
        } catch (ApiException ex) {
            binding.reject("passwordError", ex.getMessage());
            FormErrors.add(model, "password", binding);
            populate(model, user);
            return "account/index";
        }
    }

    private void populate(Model model, StoreUser user) {
        model.addAttribute("roleLabel", user.getRole() == UserRole.ADMIN ? "管理员" : "顾客");
        model.addAttribute("joinedAt", user.getCreatedAt() == null
                ? "-"
                : user.getCreatedAt().format(JOINED_AT_FORMAT));
    }
}