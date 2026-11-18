package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.admin.dto.AdminProductDetail;
import cn.edu.csu.mycoursestore.admin.dto.AdminProductFilter;
import cn.edu.csu.mycoursestore.admin.dto.ProductCreateRequest;
import cn.edu.csu.mycoursestore.admin.dto.ProductImageRequest;
import cn.edu.csu.mycoursestore.admin.dto.ProductUpdateRequest;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.service.AdminProductService;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.common.FormErrors;
import cn.edu.csu.mycoursestore.product.entity.ProductStatus;
import cn.edu.csu.mycoursestore.security.RequireAdmin;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequestMapping("/admin/products")
@RequireAdmin
public class AdminProductController {

    private static final String DEFAULT_COVER = "images/placeholder.svg";

    private final AdminProductService productService;

    public AdminProductController(AdminProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) ProductStatus status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        AdminProductFilter filter = new AdminProductFilter(keyword, categoryId, status);
        model.addAttribute("filter", filter);
        model.addAttribute("result", productService.list(filter, new PageQuery(page, size)));
        model.addAttribute("categoryGroups", productService.categoryGroups());
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductCreateRequest("", null, "", "", "", DEFAULT_COVER,
                null, 0, 0));
        prepareForm(model, false, "/admin/products");
        return "admin/products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductCreateRequest form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "product", binding);
            prepareForm(model, false, "/admin/products");
            return "admin/products/form";
        }
        try {
            long id = productService.create(form);
            redirectAttributes.addFlashAttribute("success", "商品已创建，当前为草稿状态");
            return "redirect:/admin/products/" + id;
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            prepareForm(model, false, "/admin/products");
            return "admin/products/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("product", productService.detail(id));
        return "admin/products/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        AdminProductDetail product = productService.detail(id);
        model.addAttribute("product", product);
        model.addAttribute("form", new ProductUpdateRequest(
                product.name(),
                product.summary(),
                product.description(),
                product.coverImage(),
                product.price(),
                product.sortOrder(),
                product.version()));
        prepareForm(model, true, "/admin/products/" + id);
        return "admin/products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("form") ProductUpdateRequest form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "product", binding);
            model.addAttribute("product", productService.detail(id));
            prepareForm(model, true, "/admin/products/" + id);
            return "admin/products/form";
        }
        productService.update(id, form);
        redirectAttributes.addFlashAttribute("success", "商品信息已更新");
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable long id,
                               @RequestParam ProductStatus status,
                               @RequestParam long version,
                               RedirectAttributes redirectAttributes) {
        productService.changeStatus(id, status, version);
        redirectAttributes.addFlashAttribute("success", status == ProductStatus.ON_SALE ? "商品已上架" : "商品已下架");
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/images")
    public String addImage(@PathVariable long id,
                           @Valid @ModelAttribute("imageForm") ProductImageRequest form,
                           BindingResult binding,
                           RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstMessage(binding));
            return "redirect:/admin/products/" + id;
        }
        try {
            productService.addImage(id, form);
            redirectAttributes.addFlashAttribute("success", "图片已添加");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/images/{imageId}")
    public String updateImage(@PathVariable long id,
                              @PathVariable long imageId,
                              @Valid @ModelAttribute("imageForm") ProductImageRequest form,
                              BindingResult binding,
                              RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstMessage(binding));
            return "redirect:/admin/products/" + id;
        }
        try {
            productService.updateImage(id, imageId, form);
            redirectAttributes.addFlashAttribute("success", "图片已更新");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable long id,
                              @PathVariable long imageId,
                              RedirectAttributes redirectAttributes) {
        try {
            productService.deleteImage(id, imageId);
            redirectAttributes.addFlashAttribute("success", "图片已删除");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    private void prepareForm(Model model, boolean editing, String formAction) {
        model.addAttribute("editing", editing);
        model.addAttribute("formAction", formAction);
        model.addAttribute("categoryGroups", productService.categoryGroups());
        model.addAttribute("defaultCover", DEFAULT_COVER);
    }

    private static String firstMessage(BindingResult binding) {
        return binding.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("提交内容有误");
    }
}
