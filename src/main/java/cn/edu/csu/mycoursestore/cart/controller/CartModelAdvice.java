package cn.edu.csu.mycoursestore.cart.controller;

import cn.edu.csu.mycoursestore.cart.service.CartService;
import cn.edu.csu.mycoursestore.security.AuthInterceptor;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CartModelAdvice {

    private final CartService cartService;

    public CartModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpServletRequest request) {
        StoreUser user = (StoreUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        return user == null ? 0 : cartService.cartQuantity(user.getId());
    }
}