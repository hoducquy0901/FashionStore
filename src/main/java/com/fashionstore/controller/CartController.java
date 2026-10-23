package com.fashionstore.controller;

import com.fashionstore.entity.CartItem;
import com.fashionstore.entity.ProductVariant;
import com.fashionstore.service.CartService;
import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String view(Model model, Principal principal) {
        List<CartItem> items = cartService.getItems(principal.getName());
        BigDecimal total = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam(required = false) Integer variantId,
                      @RequestParam(required = false) Integer productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(defaultValue = "false") boolean buyNow,
                      @RequestHeader(value = "Referer", required = false) String referer,
                      Principal principal, RedirectAttributes redirect) {

        Optional<ProductVariant> variant = cartService.resolveVariant(variantId, productId);
        if (variant.isEmpty()) {
            redirect.addFlashAttribute("cartError", "Vui lòng chọn màu sắc và kích thước");
            return productId != null ? "redirect:/product/" + productId : "redirect:/";
        }
        try {
            cartService.addToCart(principal.getName(), variant.get().getVariantId(), quantity);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("cartError", e.getMessage());
            return "redirect:" + backUrl(referer);
        }
        if (buyNow) {
            return "redirect:/cart";
        }
        redirect.addFlashAttribute("cartMessage", "Đã thêm sản phẩm vào giỏ hàng");
        return "redirect:" + backUrl(referer);
    }

    @PostMapping("/update")
    public String update(@RequestParam Integer cartItemId, @RequestParam int quantity,
                         Principal principal, RedirectAttributes redirect) {
        try {
            cartService.updateQuantity(principal.getName(), cartItemId, quantity);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Integer cartItemId, Principal principal, RedirectAttributes redirect) {
        try {
            cartService.remove(principal.getName(), cartItemId);
            redirect.addFlashAttribute("cartMessage", "Đã xóa sản phẩm khỏi giỏ hàng");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:/cart";
    }

    /** Quay lại trang trước, chỉ lấy phần đường dẫn nội bộ để tránh chuyển hướng ra ngoài */
    private String backUrl(String referer) {
        if (referer == null || referer.isBlank()) return "/";
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) return "/";
            return uri.getRawQuery() != null ? path + "?" + uri.getRawQuery() : path;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}