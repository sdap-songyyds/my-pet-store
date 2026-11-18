package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.admin.dto.AdminInventoryFilter;
import cn.edu.csu.mycoursestore.admin.dto.InventoryUpdateRequest;
import cn.edu.csu.mycoursestore.admin.dto.StockLevel;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.service.AdminInventoryService;
import cn.edu.csu.mycoursestore.admin.service.AdminProductService;
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
@RequestMapping("/admin/inventory")
@RequireAdmin
public class AdminInventoryController {

    private final AdminInventoryService inventoryService;
    private final AdminProductService productService;

    public AdminInventoryController(AdminInventoryService inventoryService,
                                    AdminProductService productService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) StockLevel level,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        AdminInventoryFilter filter = new AdminInventoryFilter(keyword, categoryId, level);
        model.addAttribute("filter", filter);
        model.addAttribute("result", inventoryService.list(filter, new PageQuery(page, size)));
        model.addAttribute("categoryGroups", productService.categoryGroups());
        model.addAttribute("lowStockThreshold", AdminInventoryService.LOW_STOCK_THRESHOLD);
        return "admin/inventory/list";
    }

    @PostMapping("/{id}/stock")
    public String updateStock(@PathVariable long id,
                              @Valid @ModelAttribute("stockForm") InventoryUpdateRequest form,
                              BindingResult binding,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) StockLevel level,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstMessage(binding));
        } else {
            inventoryService.updateStock(id, form);
            redirectAttributes.addFlashAttribute("success", "库存已更新");
        }
        appendFilters(redirectAttributes, keyword, categoryId, level, page, size);
        return "redirect:/admin/inventory";
    }

    private static void appendFilters(RedirectAttributes redirectAttributes,
                                      String keyword,
                                      Long categoryId,
                                      StockLevel level,
                                      int page,
                                      int size) {
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
        if (categoryId != null) {
            redirectAttributes.addAttribute("categoryId", categoryId);
        }
        if (level != null) {
            redirectAttributes.addAttribute("level", level.name());
        }
        if (page > 1) {
            redirectAttributes.addAttribute("page", page);
        }
        if (size != PageQuery.DEFAULT_SIZE) {
            redirectAttributes.addAttribute("size", size);
        }
    }

    private static String firstMessage(BindingResult binding) {
        return binding.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("提交内容有误");
    }
}
