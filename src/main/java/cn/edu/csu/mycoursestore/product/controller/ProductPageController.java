package cn.edu.csu.mycoursestore.product.controller;

import cn.edu.csu.mycoursestore.product.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductPageController {

    private final CatalogService catalogService;

    public ProductPageController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("product", catalogService.productDetail(id));
        return "shop/product-detail";
    }
}