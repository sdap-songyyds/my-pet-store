package cn.edu.csu.mycoursestore.product.controller;

import cn.edu.csu.mycoursestore.product.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final int FEATURED_LIMIT = 8;

    private final CatalogService catalogService;

    public HomeController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", catalogService.categoryTree());
        model.addAttribute("products", catalogService.featuredProducts(FEATURED_LIMIT));
        return "shop/home";
    }
}