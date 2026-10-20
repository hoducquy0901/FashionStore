package com.fashionstore.controller;

import com.fashionstore.repository.CategoryRepository;
import com.fashionstore.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.security.Principal;

@Controller
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/category/{id}")
    public String byCategory(@PathVariable Integer id, Model model, Principal principal) {
        var category = categoryRepository.findById(id).orElse(null);
        model.addAttribute("categories", categoryRepository.findByParentCategoryIdIsNullAndStatus("ACTIVE"));
        model.addAttribute("products", productRepository.findByCategoryIdAndStatus(id, "ACTIVE"));
        model.addAttribute("title", category != null ? category.getCategoryName() : "Danh mục");
        model.addAttribute("activeCategoryId", id);
        model.addAttribute("username", principal != null ? principal.getName() : null);
        return "home";
    }
}