package com.fashionstore.controller;

import com.fashionstore.entity.Category;
import com.fashionstore.repository.CategoryRepository;
import com.fashionstore.service.CartService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final CartService cartService;
    private final CategoryRepository categoryRepository;

    public GlobalModelAdvice(CartService cartService, CategoryRepository categoryRepository) {
        this.cartService = cartService;
        this.categoryRepository = categoryRepository;
    }

    @ModelAttribute("username")
    public String username(Principal principal) {
        return principal != null ? principal.getName() : null;
    }

    @ModelAttribute("cartCount")
    public Integer cartCount(Principal principal) {
        return principal == null ? 0 : cartService.countItems(principal.getName());
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryRepository.findByParentCategoryIdIsNullAndStatus("ACTIVE");
    }
}