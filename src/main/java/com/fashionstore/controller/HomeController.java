package com.fashionstore.controller;

import com.fashionstore.repository.CategoryRepository;
import com.fashionstore.repository.ProductRepository;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public HomeController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        model.addAttribute("categories", categoryRepository.findByParentCategoryIdIsNullAndStatus("ACTIVE"));
        model.addAttribute("products", productRepository.findTop8ByStatusOrderByCreatedAtDesc("ACTIVE"));
        model.addAttribute("title", "Sản phẩm mới nhất");
        model.addAttribute("username", principal != null ? principal.getName() : null);
        return "home";
    }
}