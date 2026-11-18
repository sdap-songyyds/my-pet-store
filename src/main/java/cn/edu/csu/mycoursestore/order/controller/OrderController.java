package cn.edu.csu.mycoursestore.order.controller;

import cn.edu.csu.mycoursestore.cart.dto.CartView;
import cn.edu.csu.mycoursestore.cart.service.CartService;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.common.FormErrors;
import cn.edu.csu.mycoursestore.order.dto.CheckoutRequest;
import cn.edu.csu.mycoursestore.order.service.OrderService;
import cn.edu.csu.mycoursestore.security.CurrentUser;
import cn.edu.csu.mycoursestore.security.RequireAuth;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @RequireAuth
    @GetMapping("/checkout")
    public String checkout(@CurrentUser StoreUser user, Model model) {
        CartView cart = cartService.getCart(user.getId());
        if (cart.kinds() == 0) {
            return "redirect:/cart?error=CART_EMPTY";
        }
        model.addAttribute("cart", cart);
        return "order/checkout";
    }

    @RequireAuth
    @PostMapping("/orders")
    public String create(@CurrentUser StoreUser user,
                         @Valid @ModelAttribute("form") CheckoutRequest form,
                         BindingResult binding,
                         Model model) {
        if (binding.hasErrors()) {
            FormErrors.add(model, "checkout", binding);
            model.addAttribute("cart", cartService.getCart(user.getId()));
            return "order/checkout";
        }
        try {
            long orderId = orderService.createFromCart(user.getId(), form);
            return "redirect:/orders/" + orderId + "?created=1";
        } catch (ApiException ex) {
            if ("CART_EMPTY".equals(ex.getCode())) {
                return "redirect:/cart?error=CART_EMPTY";
            }
            model.addAttribute("error", OrderMessages.of(ex.getCode()));
            model.addAttribute("cart", cartService.getCart(user.getId()));
            return "order/checkout";
        }
    }

    @RequireAuth
    @GetMapping("/orders")
    public String list(@CurrentUser StoreUser user, Model model) {
        model.addAttribute("orders", orderService.listOrders(user.getId()));
        return "order/list";
    }

    @RequireAuth
    @GetMapping("/orders/{id}")
    public String detail(@CurrentUser StoreUser user,
                         @PathVariable long id,
                         @RequestParam(required = false) String created,
                         @RequestParam(required = false) String paid,
                         @RequestParam(required = false) String cancelled,
                         @RequestParam(required = false) String error,
                         Model model) {
        model.addAttribute("order", orderService.getOrder(user.getId(), id));
        if (created != null) {
            model.addAttribute("success", "订单已创建，请尽快完成支付");
        } else if (paid != null) {
            model.addAttribute("success", "支付成功（模拟）");
        } else if (cancelled != null) {
            model.addAttribute("success", "订单已取消，库存已恢复");
        }
        if (error != null) {
            model.addAttribute("error", OrderMessages.of(error));
        }
        return "order/detail";
    }

    @RequireAuth
    @PostMapping("/orders/{id}/pay")
    public String pay(@CurrentUser StoreUser user, @PathVariable long id) {
        try {
            orderService.pay(user.getId(), id);
            return "redirect:/orders/" + id + "?paid=1";
        } catch (ApiException ex) {
            return "redirect:/orders/" + id + "?error=" + ex.getCode();
        }
    }

    @RequireAuth
    @PostMapping("/orders/{id}/cancel")
    public String cancel(@CurrentUser StoreUser user, @PathVariable long id) {
        try {
            orderService.cancel(user.getId(), id);
            return "redirect:/orders/" + id + "?cancelled=1";
        } catch (ApiException ex) {
            return "redirect:/orders/" + id + "?error=" + ex.getCode();
        }
    }
}