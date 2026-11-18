package cn.edu.csu.mycoursestore.admin.controller;

import cn.edu.csu.mycoursestore.security.RequireAdmin;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/products")
@RequireAdmin
public class AdminProductController {

    @GetMapping
    public String list() {
        return "admin/products/list";
    }
}
