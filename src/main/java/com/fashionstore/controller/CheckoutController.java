package com.fashionstore.controller;

import com.fashionstore.DTO.CheckoutForm;
import com.fashionstore.entity.Address;
import com.fashionstore.entity.CartItem;
import com.fashionstore.entity.Orders;
import com.fashionstore.entity.User;
import com.fashionstore.exception.OrderException;
import com.fashionstore.service.CartService;
import com.fashionstore.service.OrderService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping
    public String page(Model model, Principal principal, RedirectAttributes redirect) {
        List<CartItem> items = cartService.getItems(principal.getName());
        if (items.isEmpty()) {
            redirect.addFlashAttribute("cartError", "Giỏ hàng đang trống");
            return "redirect:/cart";
        }
        List<Address> addresses = orderService.getAddresses(principal.getName());
        CheckoutForm form = new CheckoutForm();
        if (!addresses.isEmpty()) {
            form.setAddressId(addresses.get(0).getAddressId());   // mặc định chọn địa chỉ đầu tiên
        }
        fill(model, items, addresses, principal.getName());
        model.addAttribute("form", form);
        return "checkout";
    }

    @PostMapping
    public String place(@ModelAttribute("form") CheckoutForm form, BindingResult result,
                        Model model, Principal principal, RedirectAttributes redirect) {
        validate(form, result);
        if (!result.hasErrors()) {
            try {
                Orders order = orderService.placeOrder(principal.getName(), form);
                redirect.addFlashAttribute("orderMessage", "Đặt hàng thành công! Mã đơn hàng: " + order.getOrderCode());
                return "redirect:/orders/" + order.getOrderCode();
            } catch (OrderException e) {
                model.addAttribute("orderError", e.getMessage());
            }
        }
        List<CartItem> items = cartService.getItems(principal.getName());
        if (items.isEmpty()) {
            redirect.addFlashAttribute("cartError", "Giỏ hàng đang trống");
            return "redirect:/cart";
        }
        fill(model, items, orderService.getAddresses(principal.getName()), principal.getName());
        return "checkout";
    }

    private void fill(Model model, List<CartItem> items, List<Address> addresses, String email) {
        BigDecimal total = items.stream().map(CartItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fee = orderService.shippingFee(total);
        model.addAttribute("items", items);
        model.addAttribute("addresses", addresses);
        model.addAttribute("total", total);
        model.addAttribute("shippingFee", fee);
        model.addAttribute("finalAmount", total.add(fee));

        User user = orderService.getUser(email);
        if (user != null) {
            model.addAttribute("profileName", blankToNull(user.getFullName()));
            model.addAttribute("profilePhone", blankToNull(user.getPhone()));
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private void validate(CheckoutForm f, BindingResult r) {
        if (!"COD".equals(f.getPaymentMethod()) && !"BANK_TRANSFER".equals(f.getPaymentMethod())) {
            r.rejectValue("paymentMethod", "invalid", "Phương thức thanh toán không hợp lệ");
        }
        if (f.getAddressId() == null) {   // địa chỉ mới thì bắt buộc nhập đủ
            require(r, "receiverName", f.getReceiverName(), "Vui lòng nhập họ tên người nhận");
            require(r, "phone", f.getPhone(), "Vui lòng nhập số điện thoại");
            if (f.getPhone() != null && !f.getPhone().isBlank()
                    && !f.getPhone().trim().matches("^(0|\\+84)\\d{9,10}$")) {
                r.rejectValue("phone", "invalid", "Số điện thoại không hợp lệ");
            }
            require(r, "province", f.getProvince(), "Vui lòng nhập tỉnh/thành phố");
            require(r, "district", f.getDistrict(), "Vui lòng nhập quận/huyện");
            require(r, "ward", f.getWard(), "Vui lòng nhập phường/xã");
            require(r, "addressDetail", f.getAddressDetail(), "Vui lòng nhập số nhà, tên đường");
        }
    }

    private void require(BindingResult r, String field, String value, String message) {
        if (value == null || value.isBlank()) {
            r.rejectValue(field, "required", message);
        }
    }
}