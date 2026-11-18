package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.security.RequireAdmin;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequireAdmin
public class AdminController {

    @GetMapping("/admin")
    public String index() {
        return "redirect:/admin/products";
    }
}
