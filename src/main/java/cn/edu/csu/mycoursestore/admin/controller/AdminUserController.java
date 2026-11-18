package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.admin.dto.AdminPasswordResetRequest;
import cn.edu.csu.mycoursestore.admin.dto.AdminUserFilter;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.service.AdminUserService;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.common.FormErrors;
import cn.edu.csu.mycoursestore.security.CurrentUser;
import cn.edu.csu.mycoursestore.security.RequireAdmin;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequireAdmin
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) UserRole role,
                       @RequestParam(required = false) UserStatus status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @CurrentUser StoreUser currentUser,
                       Model model) {
        AdminUserFilter filter = new AdminUserFilter(keyword, role, status);
        model.addAttribute("filter", filter);
        model.addAttribute("result", userService.list(filter, new PageQuery(page, size), currentUser.getId()));
        return "admin/users/list";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) UserRole role,
                         @RequestParam(required = false) UserStatus status,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @CurrentUser StoreUser currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "用户已删除");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        appendFilters(redirectAttributes, keyword, role, status, page, size);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable long id,
                               @RequestParam UserStatus status,
                               @RequestParam UserStatus expectedStatus,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) UserRole role,
                               @RequestParam(required = false) UserStatus filterStatus,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @CurrentUser StoreUser currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            String displayName = userService.updateStatus(id, status, expectedStatus, currentUser.getId());
            redirectAttributes.addFlashAttribute("success",
                    (status == UserStatus.DISABLED ? "已禁用 " : "已启用 ") + displayName);
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        appendFilters(redirectAttributes, keyword, role, filterStatus, page, size);
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/password")
    public String passwordForm(@PathVariable long id,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) UserRole role,
                               @RequestParam(required = false) UserStatus status,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        model.addAttribute("user", userService.detail(id));
        model.addAttribute("form", new AdminPasswordResetRequest(null, null));
        addFilterModel(model, keyword, role, status, page, size);
        return "admin/users/password";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable long id,
                                @Valid @ModelAttribute("form") AdminPasswordResetRequest form,
                                BindingResult binding,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) UserRole role,
                                @RequestParam(required = false) UserStatus status,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!binding.hasErrors() && !form.newPassword().equals(form.confirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "两次输入的新密码不一致");
        }
        if (binding.hasErrors()) {
            FormErrors.add(model, "password", binding);
            model.addAttribute("user", userService.detail(id));
            addFilterModel(model, keyword, role, status, page, size);
            return "admin/users/password";
        }
        userService.resetPassword(id, form);
        redirectAttributes.addFlashAttribute("success",
                "密码已重置；该用户已登录的会话将在 2 小时内自然过期");
        appendFilters(redirectAttributes, keyword, role, status, page, size);
        return "redirect:/admin/users";
    }

    private static void addFilterModel(Model model, String keyword, UserRole role, UserStatus status,
                                       int page, int size) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
    }

    private static void appendFilters(RedirectAttributes redirectAttributes,
                                      String keyword,
                                      UserRole role,
                                      UserStatus status,
                                      int page,
                                      int size) {
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
        if (role != null) {
            redirectAttributes.addAttribute("role", role.name());
        }
        if (status != null) {
            redirectAttributes.addAttribute("status", status.name());
        }
        if (page > 1) {
            redirectAttributes.addAttribute("page", page);
        }
        if (size != PageQuery.DEFAULT_SIZE) {
            redirectAttributes.addAttribute("size", size);
        }
    }
}
