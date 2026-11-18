package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.admin.dto.AdminCategoryDetail;
import cn.edu.csu.mycoursestore.admin.dto.CategoryCreateRequest;
import cn.edu.csu.mycoursestore.admin.dto.CategoryUpdateRequest;
import cn.edu.csu.mycoursestore.admin.service.AdminCategoryService;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.common.FormErrors;
import cn.edu.csu.mycoursestore.security.RequireAdmin;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/admin/categories")
@RequireAdmin
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    public AdminCategoryController(AdminCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", categoryService.groups());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long parentId, Model model) {
        model.addAttribute("form", new CategoryCreateRequest("", parentId, "", 0));
        prepareCreateForm(model, parentId);
        return "admin/categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryCreateRequest form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "category", binding);
            prepareCreateForm(model, form.parentId());
            return "admin/categories/form";
        }
        try {
            categoryService.create(form);
            redirectAttributes.addFlashAttribute("success", "分类已创建");
            return "redirect:/admin/categories";
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            prepareCreateForm(model, form.parentId());
            return "admin/categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        AdminCategoryDetail category = categoryService.detail(id);
        model.addAttribute("category", category);
        model.addAttribute("form", new CategoryUpdateRequest(category.name(), category.sortOrder()));
        prepareEditForm(model, id);
        return "admin/categories/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("form") CategoryUpdateRequest form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "category", binding);
            model.addAttribute("category", categoryService.detail(id));
            prepareEditForm(model, id);
            return "admin/categories/form";
        }
        try {
            categoryService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "分类已更新");
            return "redirect:/admin/categories";
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                throw ex;
            }
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("category", categoryService.detail(id));
            prepareEditForm(model, id);
            return "admin/categories/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "分类已删除");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    private void prepareCreateForm(Model model, Long parentId) {
        model.addAttribute("parent", parentId == null ? null : categoryService.requireFirstLevel(parentId));
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/admin/categories");
    }

    private void prepareEditForm(Model model, long id) {
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/admin/categories/" + id);
    }
}
