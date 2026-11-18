package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.admin.dto.AdminOrderFilter;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.service.AdminOrderService;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.order.entity.OrderStatus;
import cn.edu.csu.mycoursestore.security.RequireAdmin;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/orders")
@RequireAdmin
public class AdminOrderController {

    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        AdminOrderFilter filter = new AdminOrderFilter(keyword, status, from, to);
        model.addAttribute("filter", filter);
        model.addAttribute("result", orderService.list(filter, new PageQuery(page, size)));
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) OrderStatus status,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model) {
        model.addAttribute("order", orderService.detail(id));
        addFilterModel(model, keyword, status, from, to, page, size);
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/ship")
    public String ship(@PathVariable long id,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       RedirectAttributes redirectAttributes) {
        try {
            orderService.ship(id);
            redirectAttributes.addFlashAttribute("success", "订单已发货");
            return redirectToDetail(id, keyword, status, from, to, page, size, redirectAttributes);
        } catch (ApiException ex) {
            return handleActionError(id, ex, keyword, status, from, to, page, size, redirectAttributes);
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable long id,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) OrderStatus status,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id);
            redirectAttributes.addFlashAttribute("success", "订单已取消，库存已恢复");
            return redirectToDetail(id, keyword, status, from, to, page, size, redirectAttributes);
        } catch (ApiException ex) {
            return handleActionError(id, ex, keyword, status, from, to, page, size, redirectAttributes);
        }
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable long id,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) OrderStatus status,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size,
                           RedirectAttributes redirectAttributes) {
        try {
            orderService.complete(id);
            redirectAttributes.addFlashAttribute("success", "订单已完成");
            return redirectToDetail(id, keyword, status, from, to, page, size, redirectAttributes);
        } catch (ApiException ex) {
            return handleActionError(id, ex, keyword, status, from, to, page, size, redirectAttributes);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) OrderStatus status,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         RedirectAttributes redirectAttributes) {
        try {
            orderService.delete(id);
            redirectAttributes.addFlashAttribute("success", "订单已删除");
            appendFilters(redirectAttributes, keyword, status, from, to, page, size);
            return "redirect:/admin/orders";
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                appendFilters(redirectAttributes, keyword, status, from, to, page, size);
                return "redirect:/admin/orders";
            }
            return redirectToDetail(id, keyword, status, from, to, page, size, redirectAttributes);
        }
    }

    private String handleActionError(long id, ApiException ex, String keyword, OrderStatus status,
                                     LocalDate from, LocalDate to, int page, int size,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        if (ex.getStatus() == HttpStatus.NOT_FOUND) {
            appendFilters(redirectAttributes, keyword, status, from, to, page, size);
            return "redirect:/admin/orders";
        }
        return redirectToDetail(id, keyword, status, from, to, page, size, redirectAttributes);
    }

    private static String redirectToDetail(long id, String keyword, OrderStatus status,
                                           LocalDate from, LocalDate to, int page, int size,
                                           RedirectAttributes redirectAttributes) {
        appendFilters(redirectAttributes, keyword, status, from, to, page, size);
        return "redirect:/admin/orders/" + id;
    }

    private static void addFilterModel(Model model, String keyword, OrderStatus status,
                                       LocalDate from, LocalDate to, int page, int size) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
    }

    private static void appendFilters(RedirectAttributes redirectAttributes,
                                      String keyword,
                                      OrderStatus status,
                                      LocalDate from,
                                      LocalDate to,
                                      int page,
                                      int size) {
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
        if (status != null) {
            redirectAttributes.addAttribute("status", status.name());
        }
        if (from != null) {
            redirectAttributes.addAttribute("from", from);
        }
        if (to != null) {
            redirectAttributes.addAttribute("to", to);
        }
        if (page > 1) {
            redirectAttributes.addAttribute("page", page);
        }
        if (size != PageQuery.DEFAULT_SIZE) {
            redirectAttributes.addAttribute("size", size);
        }
    }
}
