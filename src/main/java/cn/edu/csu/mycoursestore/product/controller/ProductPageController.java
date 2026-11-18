package cn.edu.csu.mycoursestore.product.controller;

import cn.edu.csu.mycoursestore.cart.controller.CartMessages;
import cn.edu.csu.mycoursestore.product.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductPageController {

    private final CatalogService catalogService;

    public ProductPageController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable long id,
                         @RequestParam(required = false) String added,
                         @RequestParam(required = false) String cartError,
                         Model model) {
        model.addAttribute("product", catalogService.productDetail(id));
        if (added != null) {
            model.addAttribute("cartSuccess", true);
        }
        if (cartError != null) {
            model.addAttribute("cartError", CartMessages.of(cartError));
        }
        return "shop/product-detail";
    }
}