package com.fashionstore.controller;

import com.fashionstore.exception.OrderException;
import com.fashionstore.service.OrderManagementService;
import com.fashionstore.service.OrderService;
import com.fashionstore.service.OrderService.OrderView;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderManagementService managementService;

    @Value("${shop.bank.name:Vietcombank}")
    private String bankName;
    @Value("${shop.bank.account:0123456789}")
    private String bankAccount;
    @Value("${shop.bank.owner:CONG TY FASHIONSTORE}")
    private String bankOwner;

    public OrderController(OrderService orderService, OrderManagementService managementService) {
        this.orderService = orderService;
        this.managementService = managementService;
    }

    @GetMapping
    public String list(Model model, Principal principal) {
        model.addAttribute("orders", orderService.getOrders(principal.getName()));
        return "orders";
    }

    @GetMapping("/{orderCode}")
    public String detail(@PathVariable String orderCode, Model model, Principal principal) {
        OrderView view;
        try {
            view = orderService.getOrderView(principal.getName(), orderCode);
        } catch (OrderException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("order", view.getOrder());
        model.addAttribute("details", view.getDetails());
        model.addAttribute("payment", view.getPayment());
        model.addAttribute("address", view.getAddress());
        model.addAttribute("bankName", bankName);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner", bankOwner);
        return "order-detail";
    }

    @PostMapping("/{orderCode}/cancel")
    public String cancel(@PathVariable String orderCode, @RequestParam(defaultValue = "") String reason,
                         Principal principal, RedirectAttributes redirect) {
        try {
            managementService.customerCancel(principal.getName(), orderCode, reason);
            redirect.addFlashAttribute("orderMessage", "Đã hủy đơn hàng");
        } catch (OrderException e) {
            redirect.addFlashAttribute("orderError", e.getMessage());
        }
        return "redirect:/orders/" + orderCode;
    }
}