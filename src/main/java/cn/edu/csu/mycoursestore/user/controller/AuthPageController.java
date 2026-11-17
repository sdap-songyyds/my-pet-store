package cn.edu.csu.mycoursestore.user.controller;

import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.common.FormErrors;
import cn.edu.csu.mycoursestore.security.AuthCookieFactory;
import cn.edu.csu.mycoursestore.security.CurrentUser;
import cn.edu.csu.mycoursestore.user.dto.AuthResponse;
import cn.edu.csu.mycoursestore.user.dto.LoginRequest;
import cn.edu.csu.mycoursestore.user.dto.RegisterRequest;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
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

@Controller
public class AuthPageController {

    private final AuthService authService;
    private final AuthCookieFactory cookieFactory;

    public AuthPageController(AuthService authService, AuthCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/")
    public String home(@CurrentUser StoreUser user) {
        return user == null ? "redirect:/login" : "redirect:/account";
    }

    @GetMapping("/login")
    public String loginPage(@CurrentUser StoreUser user,
                            @RequestParam(required = false) String redirect,
                            Model model) {
        if (user != null) {
            return "redirect:/account";
        }
        model.addAttribute("redirect", safeRedirect(redirect));
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginRequest form,
                        BindingResult binding,
                        @RequestParam(required = false) String redirect,
                        HttpServletResponse response,
                        Model model) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "login", binding);
            model.addAttribute("redirect", safeRedirect(redirect));
            return "auth/login";
        }
        try {
            AuthResponse auth = authService.login(form);
            response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.create(auth.token()).toString());
            return "redirect:" + safeRedirect(redirect);
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("redirect", safeRedirect(redirect));
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(@CurrentUser StoreUser user) {
        if (user != null) {
            return "redirect:/account";
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form,
                           BindingResult binding,
                           HttpServletResponse response,
                           Model model) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "register", binding);
            return "auth/register";
        }
        try {
            AuthResponse auth = authService.register(form);
            response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.create(auth.token()).toString());
            return "redirect:/account";
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return "redirect:/login";
    }

    private static String safeRedirect(String redirect) {
        if (redirect != null
                && redirect.startsWith("/")
                && !redirect.startsWith("//")
                && !redirect.startsWith("/\\")
                && redirect.indexOf('\r') < 0
                && redirect.indexOf('\n') < 0) {
            return redirect;
        }
        return "/account";
    }
}