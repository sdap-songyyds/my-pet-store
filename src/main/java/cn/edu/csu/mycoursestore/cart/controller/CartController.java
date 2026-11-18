package cn.edu.csu.mycoursestore.cart.controller;

import cn.edu.csu.mycoursestore.cart.service.CartService;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.security.CurrentUser;
import cn.edu.csu.mycoursestore.security.RequireAuth;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @RequireAuth
    @GetMapping("/cart")
    public String cart(@CurrentUser StoreUser user,
                       @RequestParam(required = false) String error,
                       Model model) {
        model.addAttribute("cart", cartService.getCart(user.getId()));
        if (error != null) {
            model.addAttribute("error", CartMessages.of(error));
        }
        return "cart/index";
    }

    @RequireAuth
    @PostMapping("/cart/items")
    public String add(@CurrentUser StoreUser user,
                      @RequestParam long productId,
                      @RequestParam(defaultValue = "1") int quantity) {
        try {
            cartService.add(user.getId(), productId, quantity);
            return "redirect:/products/" + productId + "?added=1";
        } catch (ApiException ex) {
            return "redirect:/products/" + productId + "?cartError=" + ex.getCode();
        }
    }

    @RequireAuth
    @PostMapping("/cart/items/{productId}/quantity")
    public String updateQuantity(@CurrentUser StoreUser user,
                                 @PathVariable long productId,
                                 @RequestParam int quantity) {
        try {
            cartService.updateQuantity(user.getId(), productId, quantity);
            return "redirect:/cart";
        } catch (ApiException ex) {
            return errorRedirect(ex);
        }
    }

    @RequireAuth
    @PostMapping("/cart/items/{productId}/remove")
    public String remove(@CurrentUser StoreUser user, @PathVariable long productId) {
        try {
            cartService.remove(user.getId(), productId);
            return "redirect:/cart";
        } catch (ApiException ex) {
            return errorRedirect(ex);
        }
    }

    private static String errorRedirect(ApiException ex) {
        return "redirect:/cart?error=" + ex.getCode();
    }
}