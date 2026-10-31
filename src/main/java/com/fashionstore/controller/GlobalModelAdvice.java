package com.fashionstore.controller;

import com.fashionstore.entity.Category;
import com.fashionstore.repository.CategoryRepository;
import com.fashionstore.service.CartService;
import com.fashionstore.util.OrderLabels;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
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

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @ModelAttribute("orderStatusLabels")
    public Map<String, String> orderStatusLabels() {
        return OrderLabels.ORDER_STATUS;
    }

    @ModelAttribute("paymentMethodLabels")
    public Map<String, String> paymentMethodLabels() {
        return OrderLabels.PAYMENT_METHOD;
    }

    @ModelAttribute("paymentStatusLabels")
    public Map<String, String> paymentStatusLabels() {
        return OrderLabels.PAYMENT_STATUS;
    }
}